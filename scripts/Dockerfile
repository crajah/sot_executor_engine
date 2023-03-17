FROM openjdk:8

COPY target/scala-2.11/*assembly*.jar /code/sot-executor.jar

COPY healthcheck.sh /code/healthcheck.sh

ENV JAR_FILE=/code/sot-executor.jar \
    ARGS=-Dhttp.port=8080

WORKDIR /code

EXPOSE 8080

ENTRYPOINT java ${ARGS} -jar ${JAR_FILE}