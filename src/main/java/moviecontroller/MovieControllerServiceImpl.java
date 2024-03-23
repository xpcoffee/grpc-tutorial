package moviecontroller;

import com.proto.moviecontroller.MovieControllerServiceGrpc;
import com.proto.moviecontroller.MovieRequest;
import com.proto.moviecontroller.MovieResponse;
import com.proto.moviestore.MovieStoreRequest;
import com.proto.moviestore.MovieStoreServiceGrpc;
import com.proto.recommender.RecommenderRequest;
import com.proto.recommender.RecommenderResponse;
import com.proto.recommender.RecommenderServiceGrpc;
import com.proto.userpreferences.UserPreferencesRequest;
import com.proto.userpreferences.UserPreferencesResponse;
import com.proto.userpreferences.UserPreferencesServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates logic needed to find, filter, recommend movies.
 */
public class MovieControllerServiceImpl extends MovieControllerServiceGrpc.MovieControllerServiceImplBase {
    public static final int MOVIE_STORE_SERVICE_PORT = 50052;
    public static final int USER_PREFERENCES_SERVICE_PORT = 50053;
    public static final int RECOMMENDER_SERVICE_PORT = 50054;


    /**
     * Returns movies based on:
     *  - the genre specified in the request
     *  - the user's preferences
     *  - recommendations engine
     *
     *
     *  The order of filtering operations is:
     *   1. get all movies by genre
     *   2. filter by user preferences
     *   3. recommends one of them
     */
    @Override
    public void getMovie(MovieRequest request, StreamObserver<MovieResponse> responseObserver) {
        String userId = request.getUserid();

        MovieStoreServiceGrpc.MovieStoreServiceBlockingStub movieStoreClient = MovieStoreServiceGrpc.newBlockingStub(getChannel(MOVIE_STORE_SERVICE_PORT));
        UserPreferencesServiceGrpc.UserPreferencesServiceStub userPreferencesClient = UserPreferencesServiceGrpc.newStub(getChannel(USER_PREFERENCES_SERVICE_PORT));
        RecommenderServiceGrpc.RecommenderServiceStub recommenderClient = RecommenderServiceGrpc.newStub(getChannel(RECOMMENDER_SERVICE_PORT));

        // no idea what the latch is for...
        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<RecommenderRequest> recommenderRequestObserver = recommenderClient.getRecommendedMovie(
                new StreamObserver<>() {
                    @Override
                    public void onNext(RecommenderResponse value) {
                        responseObserver.onNext(MovieResponse.newBuilder().setMovie(value.getMovie()).build());
                        System.out.println("Recommended movie: " + value.getMovie());
                    }

                    @Override
                    public void onError(Throwable t) {
                        responseObserver.onError(t);
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        responseObserver.onCompleted();
                        latch.countDown();
                    }
                });

        StreamObserver<UserPreferencesRequest> userPreferencesRequestObserver = userPreferencesClient.getShortlistedMovies(
                new StreamObserver<>() {
                    @Override
                    public void onNext(UserPreferencesResponse value) {
                        recommenderRequestObserver.onNext(
                                RecommenderRequest.newBuilder().setUserid(userId).setMovie(value.getMovie()).build()
                        );
                    }

                    @Override
                    public void onError(Throwable t) {
                        recommenderRequestObserver.onError(t);
                    }

                    @Override
                    public void onCompleted() {
                        recommenderRequestObserver.onCompleted();
                    }
                });

        movieStoreClient.getMovies(MovieStoreRequest.newBuilder().setGenre(request.getGenre()).build())
                .forEachRemaining(response -> {
                    userPreferencesRequestObserver.onNext(
                            UserPreferencesRequest.newBuilder().setMovie(response.getMovie()).setUserid(userId).build()
                    );
                });

        userPreferencesRequestObserver.onCompleted();

        try {
            latch.await(3L, TimeUnit.SECONDS);
        }catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private ManagedChannel getChannel(int port) {
        return ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
    }
}
