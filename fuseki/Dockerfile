FROM hansidm/fuseki:4.7.0

RUN mkdir /fuseki/configuration
COPY fuseki-jetty.xml sirapi_assembler.ttl /fuseki/configuration/

CMD [ \
    "--config=/fuseki/configuration/sirapi_assembler.ttl", \
    "--jetty=/fuseki/configuration/fuseki-jetty.xml", \
    "--ping", \
    "--stats", \
    "--metrics", \
    "--compact" \
    ]