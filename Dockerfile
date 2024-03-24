FROM gradle:jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
# This builds the full project every time - done for simplicity of this example,
# but should consider separate builds per service to improve dev experience.
RUN gradle jar --no-daemon

# Use jlink to build custom jre (can't find a good minimal image online)
# props to https://careers.wolt.com/en/blog/tech/how-to-reduce-jvm-docker-image-size
RUN $JAVA_HOME/bin/jlink \
         --verbose \
         --add-modules ALL-MODULE-PATH \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /customjre

FROM public.ecr.aws/amazonlinux/amazonlinux:minimal as custom-jre
ENV JAVA_HOME=/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=build /customjre $JAVA_HOME

FROM custom-jre AS movie-finder
EXPOSE 50051
RUN mkdir /app
COPY --from=build /home/gradle/src/movie-finder/build/libs/*.jar /app/movie-finder.jar
ENTRYPOINT ["java", "-jar", "/app/movie-finder.jar"]

FROM custom-jre AS user-preferences
EXPOSE 50051
RUN mkdir /app
COPY --from=build /home/gradle/src/user-preferences/build/libs/*.jar /app/user-preferences.jar
ENTRYPOINT ["java", "-jar", "/app/user-preferences.jar"]

FROM custom-jre AS recommender
EXPOSE 50051
RUN mkdir /app
COPY --from=build /home/gradle/src/recommender/build/libs/*.jar /app/recommender.jar
ENTRYPOINT ["java", "-jar", "/app/recommender.jar"]

FROM custom-jre AS movie-store
EXPOSE 50051
RUN mkdir /app
COPY --from=build /home/gradle/src/movie-store/build/libs/*.jar /app/movie-store.jar
ENTRYPOINT ["java", "-jar", "/app/movie-store.jar"]
