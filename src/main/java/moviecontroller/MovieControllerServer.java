package moviecontroller;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class MovieControllerServer {
    public static final int MOVIE_CONTROLLER_SERVER_PORT = 50050;

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(MOVIE_CONTROLLER_SERVER_PORT)
                .addService(new MovieControllerServiceImpl())
                .build();

        server.start();
        System.out.println("Movie controller server started on port " + MOVIE_CONTROLLER_SERVER_PORT);

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    server.shutdown();
                    System.out.println("Successfully stopped the server");
                })
        );
        server.awaitTermination();
    }
}