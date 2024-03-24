# grpc-tutorial

This repo expands on the following tutorial:
https://www.cncf.io/blog/2021/08/04/grpc-in-action-example-using-java-microservices/

## Quick start - docker

> WIP 
> 1. need to fix ports (exposing 50051 for all services, but they're not listening there) 
> 2. make ports configurable (should not be hardcoded)
> 3. add gRPC discovery to movie-finder server to remove reliance on client
 
 ```shell
docker compose up
```

## Quick start - manual

```shell
# generate classes from proto definitions
./gradlew generateProto
```
```shell
# run servers (in different processes)
./gradlew movie-store:run
./gradlew user-preferences:run
./gradlew recommender:run
./gradlew movie-finder:run
```
```shell
# call using the client
./gradlew movie-finder-consumer:run
```

> NOTE: for some reason the output of running the client appears in the `movie-finder` server process. 
> I have no idea why this happens...

## Structure

```text
Root project 'movie-grpc'
+--- Project ':movie-api'                 | common proto definitions used across the full system
+--- Project ':movie-finder'              | the main API for the project - demonstrates taking in a request and returning a response
+--- Project ':movie-finder-consumer'     | a consumer of the main API for the project - demonstrates calling with a client
+--- Project ':movie-store'               | the "data-layer" of the example (faked with inline data for now) - demonstrates taking in a request and returning a stream
+--- Project ':recommender'               | a server "in-the-middle" - demonstrates taking in a stream and returning a response
\--- Project ':user-preferences'          | another server "in-the-middle" -  demonstrates taking in AND returning a stream
```