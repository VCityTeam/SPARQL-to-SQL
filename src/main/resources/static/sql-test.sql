SELECT DISTINCT ve.*
FROM versioned_quad vq1
         JOIN versioned_quad vq2 ON vq1.id_subject = vq2.id_subject AND bit_count(vq1.validity & vq2.validity) > 0
         JOIN resource_or_literal rl1 ON vq1.id_subject = rl1.id_resource_or_literal
         JOIN resource_or_literal rl2 ON vq1.id_property = rl2.id_resource_or_literal AND rl2.name = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'
         JOIN resource_or_literal rl3 ON vq1.id_object = rl3.id_resource_or_literal AND rl3.name = 'http://www.opengis.net/citygml/transportation/2.0/transportation#Stop'
         JOIN resource_or_literal rl4 ON vq2.id_property = rl4.id_resource_or_literal AND rl4.name = 'http://www.opengis.net/citygml/transportation/2.0/transportation#Stop.accessibility'
         JOIN resource_or_literal rl5 ON vq2.id_object = rl5.id_resource_or_literal AND rl5.name = 'true'
         JOIN version ve ON (ve.begin_version_date, COALESCE(ve.end_version_date, NOW())) OVERLAPS (date '2021-05-04 00:00', date '2023-08-23 00:00');

-- Get the workspace scenarios information
SELECT rl4.name, rl5.name, rl6.name
FROM workspace w
         JOIN resource_or_literal rl3 ON w.id_object = rl3.id_resource_or_literal AND rl3.name = 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Ontologies/Workspace/3.0/workspace#Scenario'
         JOIN workspace w2 on w.id_subject = w2.id_subject
         JOIN resource_or_literal rl4 ON w2.id_subject = rl4.id_resource_or_literal
         JOIN resource_or_literal rl5 ON w2.id_property = rl5.id_resource_or_literal
         JOIN resource_or_literal rl6 ON w2.id_object = rl6.id_resource_or_literal
ORDER BY rl4.name, rl5.name;
