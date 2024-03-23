package moviestore;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class MovieStoreServer {
    public static final int MOVIE_STORE_SERVICE_PORT = 50052;

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(MOVIE_STORE_SERVICE_PORT)
                .addService(new MovieStoreServiceImpl())
                .build();

        server.start();
        System.out.println("Movie store started.");

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    server.shutdown();
                    System.out.println("Successfully stopped the server");
                })
        );
        server.awaitTermination();
    }
}