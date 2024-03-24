package recommender;

import com.proto.common.Movie;
import com.proto.recommender.RecommenderRequest;
import com.proto.recommender.RecommenderResponse;
import com.proto.recommender.RecommenderServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class RecommenderServiceImpl extends RecommenderServiceGrpc.RecommenderServiceImplBase {
    /**
     * Collects movies and then recommends one of them.
     */
    @Override
    public StreamObserver<RecommenderRequest> getRecommendedMovie(StreamObserver<RecommenderResponse> responseObserver) {
        return new StreamObserver<>() {
            final List<Movie> movies = new ArrayList<>();

            @Override
            public void onNext(RecommenderRequest value) {
                movies.add(value.getMovie());
            }

            @Override
            public void onError(Throwable t) {
                responseObserver.onError(
                        Status.INTERNAL.withDescription("Internal server error").asRuntimeException()
                );
            }

            @Override
            public void onCompleted() {
                System.out.println("Finding a recommendation from " + movies.size() + " movies");

                if (movies.size() > 0) {
                    var recommendedMovie = findMovieForRecommendation(movies);
                    responseObserver.onNext(
                            RecommenderResponse.newBuilder().setMovie(recommendedMovie).build()
                    );
                    responseObserver.onCompleted();
                } else {
                    responseObserver.onError(
                            Status.NOT_FOUND.withDescription("No movies to recommend.").asRuntimeException()
                    );
                }

            }
        };
    }

    private Movie findMovieForRecommendation(List<Movie> movies) {
        int random = new SecureRandom().nextInt(movies.size());
        return movies.stream().skip(random).findAny().get();
    }
}
