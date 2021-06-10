CREATE TABLE public.eg_abas_shared_data
(
    id character varying(64) NOT NULL,
    createdtime timestamp without time zone NOT NULL,
    jsonstring character varying(3000) NOT NULL,
    createdby character varying(10) NOT NULL,
    feature character varying(64) NOT NULL,
    CONSTRAINT eg_abas_shared_data_pkey PRIMARY KEY (id)
) ;