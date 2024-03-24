package moviestore;

import com.proto.common.Genre;
import com.proto.common.Movie;
import com.proto.moviestore.MovieStoreRequest;
import com.proto.moviestore.MovieStoreResponse;
import com.proto.moviestore.MovieStoreServiceGrpc;
import io.grpc.stub.StreamObserver;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MovieStoreServiceImpl extends MovieStoreServiceGrpc.MovieStoreServiceImplBase {

    /**
     * Returns a list of movies from the data-store (mocked out with local data) that match filters set on the request.
     *
     * Can currently only filter by genre.
     */
    @Override
    public void getMovies(MovieStoreRequest request, StreamObserver<MovieStoreResponse> responseObserver) {
        System.out.println("Returning movies for genre " + request.getGenre());

        List<Movie> movies = Arrays.asList(
                Movie.newBuilder()
                        .setTitle("No country for old men")
                        .setDescription("Western crime thriller")
                        .setRating(8.1f)
                        .setGenre(Genre.THRILLER)
                        .build(),
                Movie.newBuilder()
                        .setTitle("The Bourne Ultimatum")
                        .setDescription("Spy action-thriller")
                        .setRating(8.0f)
                        .setGenre(Genre.ACTION)
                        .build(),
                Movie.newBuilder()
                        .setTitle("The Taxi Driver")
                        .setDescription("Psychological thriller")
                        .setRating(8.2f)
                        .setGenre(Genre.THRILLER)
                        .build(),
                Movie.newBuilder()
                        .setTitle("The Hangover")
                        .setDescription("Raucous comedy")
                        .setRating(7.7f)
                        .setGenre(Genre.COMEDY)
                        .build(),
                Movie.newBuilder()
                        .setTitle("Cast away")
                        .setDescription("Survival drama")
                        .setRating(7.8f)
                        .setGenre(Genre.DRAMA)
                        .build(),
                Movie.newBuilder()
                        .setTitle("Gladiator")
                        .setDescription("Historical action")
                        .setRating(7.8f)
                        .setGenre(Genre.ACTION)
                        .build()
        );

        movies.stream()
                .filter(movie -> movie.getGenre().equals(request.getGenre()))
                .collect(Collectors.toList())
                .forEach(movie -> {
                    var movieResponse = MovieStoreResponse.newBuilder().setMovie(movie).build();
                    responseObserver.onNext(movieResponse);
                });

        responseObserver.onCompleted();
    }
}
