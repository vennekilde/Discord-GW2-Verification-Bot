#
# Build stage
#
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build

ENV HOME=/usr/app
RUN mkdir -p $HOME
WORKDIR $HOME
ADD . $HOME
RUN mvn -f $HOME/pom.xml clean package

#
# Package stage
#
FROM eclipse-temurin:21-jre-jammy

ARG JAR_FILE=/usr/app/target/*.jar
COPY --from=build $JAR_FILE /app/gw2verify-discord.jar

WORKDIR /app
ENTRYPOINT ["java"]
CMD ["-jar", "gw2verify-discord.jar"]