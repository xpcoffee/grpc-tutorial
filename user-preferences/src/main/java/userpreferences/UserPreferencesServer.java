package userpreferences;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.Optional;

public class UserPreferencesServer {
    public static final int DEFAULT_USER_PREFERENCES_SERVICE_PORT = 50053;

    public static void main(String[] args) throws IOException, InterruptedException {
        var port = Optional.ofNullable(System.getenv("PORT"))
                .map(Integer::parseInt)
                .orElseGet(() -> DEFAULT_USER_PREFERENCES_SERVICE_PORT);

        Server server = ServerBuilder.forPort(port)
                .addService(new UserPreferencesServiceImpl())
                .build();

        server.start();
        System.out.println("User-preferences server started on port " + port);

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    server.shutdown();
                    System.out.println("Successfully stopped the server");
                })
        );
        server.awaitTermination();
    }
}