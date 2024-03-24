package server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;

import java.io.IOException;
import java.util.Optional;

public class MovieFinderServer {
    public static final int DEFAULT_MOVIE_FINDER_SERVER_PORT = 50051;

    public static void main(String[] args) throws IOException, InterruptedException {
        var port = Optional.ofNullable(System.getenv("PORT"))
                .map(Integer::parseInt)
                .orElseGet(() -> DEFAULT_MOVIE_FINDER_SERVER_PORT);

        Server server = ServerBuilder.forPort(port)
                .addService(new MovieFinderServiceImpl())
                .addService(ProtoReflectionService.newInstance())
                .build();

        server.start();
        System.out.println("Movie-finder server started on port " + port);

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    server.shutdown();
                    System.out.println("Successfully stopped the server");
                })
        );
        server.awaitTermination();
    }
}