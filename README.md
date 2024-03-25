# gRPC Tutorial

My personal reference for learning the basics of service-to-service communications using [gRPC](https://grpc.io/docs/what-is-grpc/introduction/).

The repo contains a distributed system that allows a user to request a movie recommendation. The repo has multiple standalone projects, one for each service. 

Gradle is used to manage the multi-project build, and Docker-compose enables the full distributed system to built and run locally.

This repo expands on the following tutorial:
https://www.cncf.io/blog/2021/08/04/grpc-in-action-example-using-java-microservices/

## Quickstart

### Using Docker

> Pre-requisite of having docker installed locally. See also [Docker Desktop](https://www.docker.com/products/docker-desktop/).

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
```json
{
    "movie": {
        "title": "The Bourne Ultimatum",
        "rating": 8,
        "genre": "ACTION",
        "description": "Spy action-thriller"
    }
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

### The gRPC dev cycle

1. Change/add definitions in `.proto` files e.g. [movie-store.proto](./movie-store/src/main/proto/moviestore/moviestore.proto)
```protobuf
syntax = "proto3";
package moviestore;
import "common/common.proto";

option java_package = "com.proto.moviestore";
option java_multiple_files = true;

message MovieStoreRequest {
    common.Genre genre = 1;
}
message MovieStoreResponse {
    common.Movie movie = 1;
}

service MovieStoreService {
    rpc getMovies(MovieStoreRequest) returns (stream MovieStoreResponse) {};
}
```
2. Clean and generate boilerplate classes out of the proto definitions
```shell
./gradlew clean
./gradlew generateProto
```
```text
com.proto.moviestore.MovieStoreRequest;
com.proto.moviestore.MovieStoreResponse;
com.proto.moviestore.MovieStoreServiceGrpc;
```

3. To implement the logic for services, extend the generated `Base` class e.g. [MovieStoreServiceImpl]([https://github.com/xpcoffee/grpc-tutorial/blob/master/movie-store/src/main/java/moviestore/MovieStoreServer.java](https://github.com/xpcoffee/grpc-tutorial/blob/master/movie-store/src/main/java/moviestore/MovieStoreServiceImpl.java))
```java
import com.proto.moviestore.MovieStoreRequest;
import com.proto.moviestore.MovieStoreResponse;
import com.proto.moviestore.MovieStoreServiceGrpc;

public class OversimplifiedMovieStoreService extends MovieStoreServiceGrpc.MovieStoreServiceImplBase {
    @Override
    public void getMovies(MovieStoreRequest request, StreamObserver<MovieStoreResponse> responseObserver) {
      // Implement the method
    ]
}
```
4. Use the service in a server e.g. [MovieStoreServer](https://github.com/xpcoffee/grpc-tutorial/blob/master/movie-store/src/main/java/moviestore/MovieStoreServer.java)
```java
import com.moviestore.OversimplifiedMovieStoreService;

public class OversimplifiedMovieStoreServer {
    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(50051)
                .addService(new OversimplifiedMovieStoreService())
                .build()
                .start();
   }
}
```
5. To implement logic that calls against a server use the generated client (called a "stub" in gRPC) e.g. implementation of [MovieFinderServiceImpl](./movie-finder/src/main/java/server/MovieFinderServiceImpl.java).
```java
import com.proto.moviestore.MovieStoreServiceGrpc;

// "blocking" means it waits for the response before moving onto the next line of code,
//with "non-blocking" the call happens in the background and you need to react to it async (e.g. with an observer)
MovieStoreServiceGrpc.MovieStoreServiceBlockingStub movieStoreClient = MovieStoreServiceGrpc.newBlockingStub(getChannel(movieStoreEndpoint));

movieStoreClient.getMovies(MovieStoreRequest.newBuilder().setGenre(request.getGenre()).build())
  .forEachRemaining(response -> {
    response.getMovie(); // do something with the response
});
```
  
6. Build the project
```shell
./gradlew build
```
7. Run and call against the server (see [Quickstart](#Quickstart))


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
