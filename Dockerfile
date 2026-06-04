FROM eclipse-temurin:17-jdk AS build

WORKDIR /build

COPY gradle/ gradle/
COPY gradlew settings.gradle ./
COPY app/ app/

RUN chmod +x gradlew && ./gradlew :app:shadowJar --no-daemon

FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY --from=build /build/app/build/libs/ikaros-server-*-all.jar server.jar

EXPOSE 9000

ENTRYPOINT ["java", "-jar", "server.jar"]
