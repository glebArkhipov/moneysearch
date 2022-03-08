FROM adoptopenjdk:11-jre-hotspot
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} application.jar
ENTRYPOINT ["java", "-jar", "application.jar"]
#In order to user JAVA_OPTS passed from docker-compose following instruction should be used
#ENTRYPOINT exec java $JAVA_OPTS -jar /target/test.jar
