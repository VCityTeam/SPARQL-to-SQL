SELECT MAX(rl5.name::float) as maximum
FROM versioned_quad vq1
         JOIN resource_or_literal rl2 ON vq1.id_property = rl2.id_resource_or_literal AND rl2.name = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'
         JOIN resource_or_literal rl3 ON vq1.id_object = rl3.id_resource_or_literal AND rl3.name = 'http://www.w3.org/2002/07/owl#NamedIndividual'
         JOIN versioned_quad vq2 ON vq1.id_subject = vq2.id_subject
         JOIN resource_or_literal rl4 ON vq2.id_property = rl4.id_resource_or_literal AND rl4.name = 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Ontologies/CityGML/3.0/construction#Height.value'
         JOIN resource_or_literal rl5 ON vq2.id_object = rl5.id_resource_or_literal