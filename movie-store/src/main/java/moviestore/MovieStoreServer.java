package moviestore;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.Optional;

public class MovieStoreServer {
    public static final int DEFAULT_MOVIE_STORE_SERVICE_PORT = 50052;

    public static void main(String[] args) throws IOException, InterruptedException {
        var port = Optional.ofNullable(System.getenv("PORT"))
                .map(Integer::parseInt)
                .orElseGet(() -> DEFAULT_MOVIE_STORE_SERVICE_PORT);

        Server server = ServerBuilder.forPort(port)
                .addService(new MovieStoreServiceImpl())
                .build();

        server.start();
        System.out.println("Movie-store server started on port " + port);

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    server.shutdown();
                    System.out.println("Successfully stopped the server");
                })
        );
        server.awaitTermination();
    }
}