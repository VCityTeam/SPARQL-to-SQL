import argparse
import os
import hashlib
import rdflib
from rdflib import Dataset, URIRef, Literal

RDFLIB_INPUT_SUPPORTED_FORMATS = ['turtle', 'ttl', 'turtle2', 'xml',
                                  'pretty-xml', 'json-ld', 'ntriples', 'nt', 'nt11', 'n3', 'trig', 'trix']
ANNOTATION_TYPES = ['theoretical', 'relational']


def main():
    parser = argparse.ArgumentParser(description='Annotate RDF graph formats')
    parser.add_argument('input_file', help='Specify the input datafile, folder, or URL')
    parser.add_argument('input_format', choices=RDFLIB_INPUT_SUPPORTED_FORMATS,
                        help='Specify the input RDF format (only RDFLib supported formats)')
    parser.add_argument('output_file', default='', help='Specify the output datafile')
    parser.add_argument('annotation_type', choices=ANNOTATION_TYPES,
                        help='Specify the annotation type for the given triples')
    parser.add_argument('annotation', help='Specify the graph name annotation')
    args = parser.parse_args()

    if args.annotation_type == 'version':
        print(f'version annotation to input file: {args.input_file}')
    else:
        if args.annotation is None:
            print('Graph name annotation is missing')
            exit(1)
        print(f'({args.annotation_type} annotation) - file: {args.input_file} with {args.annotation}')

    converter = RdfConverter(args)
    converter.convert(args.input_file, args.input_format, args.output_file)


class RdfConverter:
    def __init__(self, args):
        self.args = args
        self.filename = '.'.join(os.path.split(
            args.input_file)[-1].split('.')[:-1])
        self.graph = rdflib.Graph()
        self.version = f'https://github.com/VCityTeam/SPARQL-to-SQL/Version#{self.filename}'
        self.annotation = f'https://github.com/VCityTeam/SPARQL-to-SQL/Named-Graph#{args.annotation}'

        if args.annotation_type == 'theoretical':
            self.graph_name = ('https://github.com/VCityTeam/SPARQL-to-SQL/Versioned-Named-Graph#'
                               + hashlib.sha256(self.filename.encode("utf-8")).hexdigest()
                               )
        else:
            self.graph_name = f'https://github.com/VCityTeam/SPARQL-to-SQL/Named-Graph#{args.annotation}'
        self.annotation_type = args.annotation_type

    def convert(self, input_file, input_format, output_file):
        """
        It takes an input file, an input format, an output file, and an annotation, and it adds annotation to the
        input file from the input format and saves it to the output file

        :param input_file: The file to be converted
        :param input_format: The format of the input RDF file. Must be an RDFlib compliant format
        :param output_file: The file to write the output to
        """
        self.graph.parse(input_file, format=input_format)
        ds = Dataset()
        named_graph = URIRef(self.graph_name)

        for s, p, o in self.graph.query('''
                SELECT ?s ?p ?o
                WHERE { ?s ?p ?o . }'''):
            # Définir un triple
            subject = URIRef(s)
            predicate = URIRef(p)
            object_literal = Literal(o)

            # Ajouter le quadruplet au jeu de données
            ds.add((subject, predicate, object_literal, named_graph))
        if self.annotation_type == 'theoretical':
            ds.add(
                (
                    named_graph,
                    URIRef('https://github.com/VCityTeam/SPARQL-to-SQL#is-version-of'),
                    Literal(self.annotation)
                )
            )
            ds.add(
                (
                    named_graph,
                    URIRef('https://github.com/VCityTeam/SPARQL-to-SQL#is-in-version'),
                    Literal(self.version)
                )
            )
        ds.serialize(destination=output_file, format='nquads', encoding='utf-8')


if __name__ == "__main__":
    main()
