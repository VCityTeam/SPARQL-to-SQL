SELECT nvg.name, COUNT(rl1.name) as number
FROM versioned_quad vq1
    JOIN resource_or_literal rl1 ON vq1.id_subject = rl1.id_resource_or_literal
    JOIN resource_or_literal rl2 ON vq1.id_property = rl2.id_resource_or_literal AND rl2.name = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'
    JOIN resource_or_literal rl3 ON vq1.id_object = rl3.id_resource_or_literal AND rl3.name = 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Ontologies/CityGML/3.0/building#Building'
    JOIN versioned_named_graph nvg ON vq1.id_named_graph = nvg.id_named_graph
GROUP BY nvg.name
UNION ALL
(
    WITH val as (
        SELECT rl1.name, vq1.validity::text
            FROM versioned_quad vq1
                JOIN resource_or_literal rl1 ON vq1.id_subject = rl1.id_resource_or_literal
                JOIN resource_or_literal rl2 ON vq1.id_property = rl2.id_resource_or_literal AND rl2.name = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'
                JOIN resource_or_literal rl3 ON vq1.id_object = rl3.id_resource_or_literal AND rl3.name = 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Ontologies/CityGML/3.0/building#Building'
        )
    SELECT v.message, COUNT(name) as number
    FROM val, generate_series(1, length(validity), 1) g(position)
    JOIN version v ON position = v.index_version
    WHERE substr(validity, position, 1) = '1'
    GROUP BY v.message
);
