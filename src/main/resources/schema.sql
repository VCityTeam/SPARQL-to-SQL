CREATE TABLE IF NOT EXISTS resource_or_literal
(
    id_resource_or_literal integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name                   text,
    type                   varchar(255)
);

CREATE TABLE IF NOT EXISTS versioned_named_graph
(
    id_versioned_named_graph integer PRIMARY KEY REFERENCES resource_or_literal (id_resource_or_literal),
    id_named_graph           integer REFERENCES resource_or_literal (id_resource_or_literal),
    index_version            integer
);


CREATE UNIQUE INDEX IF NOT EXISTS resource_or_literal_idx ON resource_or_literal (sha512(name::bytea), type) NULLS NOT DISTINCT;

CREATE TABLE IF NOT EXISTS versioned_quad
(
    id_object      integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_property    integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_subject     integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_named_graph integer REFERENCES resource_or_literal (id_resource_or_literal),
    validity       bit varying,
    PRIMARY KEY (id_object, id_property, id_subject, id_named_graph)
);

CREATE INDEX IF NOT EXISTS versioned_quad_ng_s_p_o ON versioned_quad (id_named_graph, id_subject, id_property, id_object);
CREATE INDEX IF NOT EXISTS versioned_quad_ng_s_o_p ON versioned_quad (id_named_graph, id_subject, id_object, id_property);
CREATE INDEX IF NOT EXISTS versioned_quad_ng_p_o_s ON versioned_quad (id_named_graph, id_property, id_object, id_subject);
CREATE INDEX IF NOT EXISTS versioned_quad_ng_p_s_o ON versioned_quad (id_named_graph, id_property, id_subject, id_object);
CREATE INDEX IF NOT EXISTS versioned_quad_ng_o_p_s ON versioned_quad (id_named_graph, id_object, id_property, id_subject);
CREATE INDEX IF NOT EXISTS versioned_quad_ng_o_s_p ON versioned_quad (id_named_graph, id_object, id_subject, id_property);

CREATE TABLE IF NOT EXISTS version
(
    index_version          integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    message                varchar(255),
    transaction_time_start timestamptz default current_timestamp,
    transaction_time_end   timestamptz default NULL
);

CREATE TABLE IF NOT EXISTS workspace
(
    id_object   integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_property integer REFERENCES resource_or_literal (id_resource_or_literal),
    id_subject  integer REFERENCES resource_or_literal (id_resource_or_literal),
    PRIMARY KEY (id_object, id_property, id_subject)
);
