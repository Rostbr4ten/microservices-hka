FROM gradle:jdk11 AS builder
COPY --chown=gradle:gradle . /home/gradle
RUN gradle build --no-daemon

FROM openjdk:11

COPY --from=builder /home/gradle/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

# alternatively build with ./gradlew bootBuildImage --imageName=<IMAGE_NAME>
# when running, the env-Variable MYSQL_HOST can be used to specify a other container as mysql host; if omitted localhost will be used