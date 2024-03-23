package moviefinder;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class MovieFinderServer {
    public static final int MOVIE_FINDER_SERVER_PORT = 50050;

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(MOVIE_FINDER_SERVER_PORT)
                .addService(new MovieFinderServiceImpl())
                .build();

        server.start();
        System.out.println("Movie finder server started on port " + MOVIE_FINDER_SERVER_PORT);

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    server.shutdown();
                    System.out.println("Successfully stopped the server");
                })
        );
        server.awaitTermination();
    }
}