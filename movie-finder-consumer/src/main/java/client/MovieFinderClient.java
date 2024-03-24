package client;

import com.proto.common.Genre;
import com.proto.moviefinder.MovieFinderServiceGrpc;
import com.proto.moviefinder.MovieRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

/**
 * Makes a grpc call to get a movie recommendation.
 */
public class MovieFinderClient {
    public static final int MOVIE_FINDER_SERVER_PORT = 50051;

    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", MOVIE_FINDER_SERVER_PORT).usePlaintext().build();
        var movieFinderClient = MovieFinderServiceGrpc.newBlockingStub(channel);

        try {
            var request = MovieRequest.newBuilder().setGenre(Genre.ACTION).setUserid("user-foo1234").build();
            var movieResponse = movieFinderClient.getMovie(request);

            System.out.println("Recommended movie: " + movieResponse.getMovie());
        } catch (StatusRuntimeException e) {
            System.out.println("Unable to recommend a movie.");
            e.printStackTrace();
        }
    }
}
