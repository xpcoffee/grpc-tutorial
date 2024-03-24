# gRPC Tutorial

My personal reference for learning the basics of service-to-service communications using [gRPC](https://grpc.io/docs/what-is-grpc/introduction/).

This repo expands on the following tutorial:
https://www.cncf.io/blog/2021/08/04/grpc-in-action-example-using-java-microservices/

## Quick start

### Using Docker

> Pre-requisite for having docker installed locally. See also [Docker Desktop](https://www.docker.com/products/docker-desktop/).

Start the services:
 
 ```shell
docker compose up
```

Using a client like [Postman](https://www.postman.com/), use [server reflection](https://www.postman.com/postman/workspace/postman-grpc-enablement/collection/641c6ef916854af1220ad91b) on the following endpoint:

```text
localhost:50051
```

Make a call

```shell
MovieFinderService/getMovie
# message
{
    "genre": "ACTION"
}
```

### Run projects & servers directly

Run servers (in different processes)
```shell
# movie-finder service needs to know where other services are running
PORT=50051 \
MOVIE_STORE_SERVER_ENDPOINT=localhost:50052 \
USER_PREFERENCES_SERVER_ENDPOINT=localhost:50053 \
RECOMMENDER_SERVER_ENDPOINT=localhost:50054 \
./gradlew movie-finder:run

PORT=50052 \
./gradlew movie-store:run

PORT=50053 \
./gradlew user-preferences:run

PORT=50054 \
./gradlew recommender:run
```

## Development

### Building / generating types
Generate proto definitions:
```shell
./gradlew generateProto
```

Build:
```shell
./gradlew build
```


### Subproject structure

```text
Root project 'movie-grpc'
+--- Project ':movie-api'                 | common proto definitions used across the full system
+--- Project ':movie-finder'              | the main API for the project - demonstrates taking in a request and returning a response
+--- Project ':movie-store'               | the "data-layer" of the example (faked with inline data for now) - demonstrates taking in a request and returning a stream
+--- Project ':recommender'               | a server "in-the-middle" - demonstrates taking in a stream and returning a response
\--- Project ':user-preferences'          | another server "in-the-middle" -  demonstrates taking in AND returning a stream
```

### Call sequence

The full order of operations is as follows (not super obvious; pay attention to when streams are opened and closed):

```mermaid
sequenceDiagram
    some-client->>+movie-finder: getMovie({ genre })

    movie-finder->>+recommender: recommendMovie({ movies })
    note over recommender: open recommendMovie stream

    movie-finder->>+user-preferences: getShortListedMovies({ userId })
    note over user-preferences: open getShortListedMovies stream

    movie-finder->>+movie-store: getMovies({ genre })
    note over movie-store: open getMovies stream
    note over movie-store: get all movies that match the genre

    loop for each movie
      movie-store->>movie-finder: movie for genre
      movie-finder->>user-preferences: move that can be shortlisted
      note over user-preferences: determine if move makes the shortlist
      opt if shortlisted 
        user-preferences->>movie-finder: shortlisted movie
        movie-finder->>recommender: movie that can be recommended
        note over recommender: register movie
      end
    end
    movie-store->>-movie-finder: close getMovies stream

    user-preferences->>-movie-finder: close getShortlistedMovies stream
    note over recommender: determine recommended movie from registered movies
    recommender->>-movie-finder: close stream with recommended movie
    
    
    movie-finder->>-some-client: recommended Movie```
