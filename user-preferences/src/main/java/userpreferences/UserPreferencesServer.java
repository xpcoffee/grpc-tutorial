package userpreferences;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class UserPreferencesServer {
    public static final int USER_PREFERENCES_SERVICE_PORT = 50053;

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(USER_PREFERENCES_SERVICE_PORT)
                .addService(new UserPreferencesServiceImpl())
                .build();

        server.start();
        System.out.println("User preferences server started.");

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    server.shutdown();
                    System.out.println("Successfully stopped the server");
                })
        );
        server.awaitTermination();
    }
}