FROM jboss/wildfly:10.1.0.Final

# Datasource de desenvolvimento (H2 em memória). Para produção, troque pelo
# docker/postgres-ds.xml (e instale o driver JDBC do Postgres antes — ver README).
COPY docker/h2-ds.xml /opt/jboss/wildfly/standalone/deployments/h2-ds.xml

# Gere o WAR antes de construir a imagem: ./gradlew war
COPY build/libs/desafio-wildfly.war /opt/jboss/wildfly/standalone/deployments/desafio-wildfly.war

EXPOSE 8080

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]
