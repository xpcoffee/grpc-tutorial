package recommender;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.Optional;

public class RecommenderServer {
    public static final int DEFAULT_RECOMMENDER_SERVICE_PORT = 50054;

    public static void main(String[] args) throws IOException, InterruptedException {
        var port = Optional.ofNullable(System.getenv("PORT"))
                .map(Integer::parseInt)
                .orElseGet(() -> DEFAULT_RECOMMENDER_SERVICE_PORT);

        Server server = ServerBuilder.forPort(port)
                .addService(new RecommenderServiceImpl())
                .build();

        server.start();
        System.out.println("Recommender server started on port " + port);

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    server.shutdown();
                    System.out.println("Successfully stopped the server");
                })
        );
        server.awaitTermination();
    }
}