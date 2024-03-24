package userpreferences;

import com.proto.userpreferences.UserPreferencesRequest;
import com.proto.userpreferences.UserPreferencesResponse;
import com.proto.userpreferences.UserPreferencesServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.security.SecureRandom;

/**
 * This service implements the bidirectional streaming scenario that  receives a stream of movies and responds with a shortlisted stream  of movies.
 * The shortlisting is performed by matching the incoming  movies with the user preferences.
 */
public class UserPreferencesServiceImpl extends UserPreferencesServiceGrpc.UserPreferencesServiceImplBase {
    @Override
    public StreamObserver<UserPreferencesRequest> getShortlistedMovies(StreamObserver<UserPreferencesResponse> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(UserPreferencesRequest value) {
                if (isEligible()) {
                    responseObserver.onNext(
                            UserPreferencesResponse.newBuilder().setMovie(value.getMovie()).build()
                    );
                }
            }

            @Override
            public void onError(Throwable t) {
                responseObserver.onError(
                        Status.INTERNAL.withDescription("Internal server error").asRuntimeException()
                );
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    /**
     * A simple random  calculation to mark an input movie as eligible or not.
     *
     * In the real world, the logic used for matching user preferences would  be complex.
     * It would involve tasks that track user activities such as  movies watched, bookmarked, rated, liked, disliked and so on.
     */
    private boolean isEligible() {
        return (new SecureRandom().nextInt() % 4 != 0);
    }
}
