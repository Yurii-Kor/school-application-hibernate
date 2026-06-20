FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY target/SchoolApplicationHibernate-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]