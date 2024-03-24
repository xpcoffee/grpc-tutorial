FROM gradle:jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
# This builds the full project every time - done for simplicity of this example,
# but should consider separate builds per service to improve dev experience.
RUN gradle jar --no-daemon

# FIXME: Should use JRE instead of JDK - JDK bloat not needed
FROM openjdk:17.0.1-jdk-slim AS movie-finder
EXPOSE 50051
RUN mkdir /app
COPY --from=build /home/gradle/src/movie-finder/build/libs/*.jar /app/movie-finder.jar
ENTRYPOINT ["java", "-jar", "/app/movie-finder.jar"]

# FIXME: Should use JRE instead of JDK
FROM openjdk:17.0.1-jdk-slim AS user-preferences
EXPOSE 50051
RUN mkdir /app
COPY --from=build /home/gradle/src/user-preferences/build/libs/*.jar /app/user-preferences.jar
ENTRYPOINT ["java", "-jar", "/app/user-preferences.jar"]

# FIXME: Should use JRE instead of JDK
FROM openjdk:17.0.1-jdk-slim AS recommender
EXPOSE 50051
RUN mkdir /app
COPY --from=build /home/gradle/src/recommender/build/libs/*.jar /app/recommender.jar
ENTRYPOINT ["java", "-jar", "/app/recommender.jar"]

# FIXME: Should use JRE instead of JDK
FROM openjdk:17.0.1-jdk-slim AS movie-store
EXPOSE 50051
RUN mkdir /app
COPY --from=build /home/gradle/src/movie-store/build/libs/*.jar /app/movie-store.jar
ENTRYPOINT ["java", "-jar", "/app/movie-store.jar"]
