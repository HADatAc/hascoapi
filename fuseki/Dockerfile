FROM hansidm/fuseki:4.7.0

RUN mkdir /fuseki/configuration
COPY --chown=fuseki:fuseki fuseki-jetty.xml hascoapi_assembler.ttl /fuseki/configuration/

CMD [ \
    "--config=/fuseki/configuration/hascoapi_assembler.ttl", \
    "--jetty=/fuseki/configuration/fuseki-jetty.xml", \
    "--ping", \
    "--stats", \
    "--metrics", \
    "--compact" \
    ]
