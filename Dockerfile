FROM openjdk:17-slim AS runner
ENV CI=true
WORKDIR /app

FROM maven:3.9.8-amazoncorretto-17 AS builder
ENV CI=true
WORKDIR /app

FROM builder AS build
COPY . /app/ 
RUN mvn package -Dclassifier=exec -DskipTests

# Debugging step: List the files in the key directory
# RUN ls -l /app/target
# RUN ls -l /app/src
# RUN ls -l /app/src/main
# RUN ls -l /app/src/main/resources
# RUN ls -l /app/src/main/resources/key

FROM runner
COPY --from=build /app/target/example-auth-jwt-custom-0.0.0-E.jar /opt/app.jar
# COPY --from=build src/main/resources/key/ES512.json /opt/key/ES512.json
COPY wait-for-it.sh /usr/local/bin/wait-for-it.sh
RUN chmod +x /usr/local/bin/wait-for-it.sh
EXPOSE 8080
CMD ["wait-for-it.sh", "database:5432", "--", "java", "-jar", "/opt/app.jar"]
