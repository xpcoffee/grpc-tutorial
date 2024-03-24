package server;

import com.proto.moviefinder.MovieFinderServiceGrpc;
import com.proto.moviefinder.MovieRequest;
import com.proto.moviefinder.MovieResponse;
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

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates logic needed to find, filter, recommend movies.
 */
public class MovieFinderServiceImpl extends MovieFinderServiceGrpc.MovieFinderServiceImplBase {
    private static final int DEFAULT_MOVIE_STORE_SERVER_PORT = 50052;
    private static final int DEFAULT_USER_PREFERENCES_SERVER_PORT = 50053;
    private static final int DEFAULT_RECOMMENDER_SERVER_PORT = 50054;
    private static final String DEFAULT_SERVER_HOST = "localhost";

    private final String movieStoreEndpoint;
    private final String userPreferencesEndpoint;
    private final String recommenderEndpoint;


    public MovieFinderServiceImpl() {
        this.movieStoreEndpoint = Optional.ofNullable(System.getenv("MOVIE_STORE_SERVER_ENDPOINT"))
                .orElseGet(() -> DEFAULT_SERVER_HOST + ":" + DEFAULT_MOVIE_STORE_SERVER_PORT);
        this.userPreferencesEndpoint = Optional.ofNullable(System.getenv("USER_PREFERENCES_SERVER_ENDPOINT"))
                .orElseGet(() -> DEFAULT_SERVER_HOST + ":" + DEFAULT_USER_PREFERENCES_SERVER_PORT);
        this.recommenderEndpoint = Optional.ofNullable(System.getenv("RECOMMENDER_SERVER_ENDPOINT"))
                .orElseGet(() -> DEFAULT_SERVER_HOST + ":" + DEFAULT_RECOMMENDER_SERVER_PORT);

        System.out.println("Using movie-store endpoint " + this.movieStoreEndpoint);
        System.out.println("Using user-preferences endpoint " + this.userPreferencesEndpoint);
        System.out.println("Using recommender endpoint " + this.recommenderEndpoint);
    }

    /**
     * Returns movies based on:
     * - the genre specified in the request
     * - the user's preferences
     * - recommendations engine
     * <p>
     * <p>
     * The order of filtering operations is:
     * 1. get all movies by genre
     * 2. filter by user preferences
     * 3. recommends one of them
     */
    @Override
    public void getMovie(MovieRequest request, StreamObserver<MovieResponse> responseObserver) {
        String userId = request.getUserid();


        MovieStoreServiceGrpc.MovieStoreServiceBlockingStub movieStoreClient = MovieStoreServiceGrpc.newBlockingStub(getChannel(movieStoreEndpoint));
        UserPreferencesServiceGrpc.UserPreferencesServiceStub userPreferencesClient = UserPreferencesServiceGrpc.newStub(getChannel(userPreferencesEndpoint));
        RecommenderServiceGrpc.RecommenderServiceStub recommenderClient = RecommenderServiceGrpc.newStub(getChannel(recommenderEndpoint));

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
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private ManagedChannel getChannel(String endpoint) {
        var tokens = endpoint.split(":");
        var host = tokens[0];
        var port = Integer.parseInt(tokens[1]);
        return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    }
}
