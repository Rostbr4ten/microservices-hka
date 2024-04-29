FROM gradle:jdk11 AS builder
COPY --chown=gradle:gradle . /home/gradle
RUN gradle build --no-daemon

FROM openjdk:11

COPY --from=builder /home/gradle/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]