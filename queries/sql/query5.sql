WITH val as (SELECT rl1.name, (vq1.validity & vq2.validity)::text as validity
FROM versioned_quad vq1
         JOIN resource_or_literal rl1 ON vq1.id_subject = rl1.id_resource_or_literal
         JOIN resource_or_literal rl2 ON vq1.id_property = rl2.id_resource_or_literal AND rl2.name = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'
         JOIN resource_or_literal rl3 ON vq1.id_object = rl3.id_resource_or_literal AND rl3.name = 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Ontologies/CityGML/3.0/building#Building'
         JOIN versioned_quad vq2 ON vq1.id_subject = vq2.id_subject
         JOIN resource_or_literal rl4 ON vq2.id_property = rl4.id_resource_or_literal AND rl4.name = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'
         JOIN resource_or_literal rl5 ON vq2.id_object = rl5.id_resource_or_literal
WHERE rl5.name != 'http://www.w3.org/2002/07/owl#NamedIndividual')
SELECT COUNT(name) as number
FROM val,
     generate_series(1, length(validity), 1) g(position)
JOIN version v ON v.index_version = position AND v.message = 'GratteCiel_2012_split.ttl'
WHERE substr(validity, position, 1) = '1';
