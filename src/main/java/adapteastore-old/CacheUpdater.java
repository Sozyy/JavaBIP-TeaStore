package adapteastore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Sends HTTP request towards AdaptableTeaStore Image service using POST {url}/rest/image/setCacheSize
 *
 * AdaptableTeaStore side (ImageProviderEndpoint.setCacheSize(long)) :
 *   - Returns 200 OK with a boolean body
 *     - true if the new size has been applied
 *     - false otherwise, null or negativ cache
 *
 * IMPORTANT NOTE !
 *   Actual PID works with a number of item but the endpoint expects bytes. PID logic must be reworked
 */
public class CacheUpdater {

    private final HttpClient http;
    private final URI setCacheSizeEndpoint;
    private final Duration requestTimeout;

    /**
     * @param imageBaseUrl ex: "http://localhost:8083/tools.descartes.teastore.image"
     */
    public CacheUpdater(String imageBaseUrl) {
        this(imageBaseUrl, Duration.ofSeconds(5));
    }

    public CacheUpdater(String imageBaseUrl, Duration requestTimeout) {
        this.setCacheSizeEndpoint = URI.create(imageBaseUrl + "/rest/image/setCacheSize");
        this.requestTimeout = requestTimeout;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Send the new cache size to the Image service
     *
     * @param newCacheSize the new size, it must be positive otherwise it'll be ignored
     *
     * @return UpdateResult represents the result of HTTP request + TeaStore acceptation
     */
    public UpdateResult update(long newCacheSize) {
        System.out.printf("[CacheUpdater] New cache size: %d%n", newCacheSize);
        if (newCacheSize < 0) {
            System.err.printf("[CacheUpdater] negative size requested (%d), skipping%n", newCacheSize);
            return UpdateResult.rejectedLocally(newCacheSize);
        }

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(setCacheSizeEndpoint)
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(BodyPublishers.ofString(Long.toString(newCacheSize)))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            int status = resp.statusCode();
            String body = resp.body() == null ? "" : resp.body().trim();

            if (status != 200) {
                System.err.printf("[CacheUpdater] HTTP %d on %s (body='%s')%n",
                        status, setCacheSizeEndpoint, body);
                return UpdateResult.httpError(newCacheSize, status, body);
            }

            boolean accepted = body.equalsIgnoreCase("true") || body.equalsIgnoreCase("\"true\"");
            return UpdateResult.success(newCacheSize, accepted, body);

        } catch (Exception e) {
            System.err.printf("[CacheUpdater] error contacting %s : %s%n",
                    setCacheSizeEndpoint, e.getMessage());
            return UpdateResult.transportError(newCacheSize, e.getMessage());
        }
    }

    // === Result structure ===

    public static final class UpdateResult {
        public enum Kind { SUCCESS, HTTP_ERROR, TRANSPORT_ERROR, REJECTED_LOCALLY }

        private final Kind kind;
        private final long requestedSize;
        private final boolean acceptedByTeaStore;
        private final int httpStatus;
        private final String message;

        private UpdateResult(Kind kind, long requestedSize, boolean acceptedByTeaStore, int httpStatus, String message) {
            this.kind = kind;
            this.requestedSize = requestedSize;
            this.acceptedByTeaStore = acceptedByTeaStore;
            this.httpStatus = httpStatus;
            this.message = message;
        }

        static UpdateResult success(long size, boolean accepted, String body) {
            return new UpdateResult(Kind.SUCCESS, size, accepted, 200, body);
        }

        static UpdateResult httpError(long size, int status, String body) {
            return new UpdateResult(Kind.HTTP_ERROR, size, false, status, body);
        }

        static UpdateResult transportError(long size, String msg) {
            return new UpdateResult(Kind.TRANSPORT_ERROR, size, false, -1, msg);
        }

        static UpdateResult rejectedLocally(long size) {
            return new UpdateResult(Kind.REJECTED_LOCALLY, size, false, -1, "negative size");
        }

        // getters

        public Kind    getKind()               { return kind; }
        public long    getRequestedSize()      { return requestedSize; }
        public boolean isAcceptedByTeaStore()  { return acceptedByTeaStore; }
        public int     getHttpStatus()         { return httpStatus; }
        public String  getMessage()            { return message; }
        public boolean isOk()                  { return kind == Kind.SUCCESS && acceptedByTeaStore; }

        @Override
        public String toString() {
            switch (kind) {
                case SUCCESS:
                    return String.format("OK size=%d accepted=%s", requestedSize, acceptedByTeaStore);
                case HTTP_ERROR:
                    return String.format("HTTP_ERROR status=%d body='%s'", httpStatus, message);
                case TRANSPORT_ERROR:
                    return String.format("TRANSPORT_ERROR msg='%s'", message);
                case REJECTED_LOCALLY:
                    return String.format("REJECTED_LOCALLY size=%d (%s)", requestedSize, message);
                default:
                    return "UNKNOWN";
            }
        }
    }
}
