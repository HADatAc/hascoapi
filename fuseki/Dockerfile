FROM atomgraph/fuseki:9985c4edd850c9277d241f6edc0d883013713ad3

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