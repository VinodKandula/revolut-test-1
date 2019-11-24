FROM openjdk:11-jdk-slim as runtime
COPY build/libs/revolut-test*all.jar revolut-test.jar
EXPOSE 8080
CMD java ${JAVA_OPTS} -jar revolut-test.jar