package adapteastore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tools.spirals.cerberus237.metricscollectorbase.IMetricsCollector;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Adaptiflow collector asking AdaptableTeaStore Persistance service via REST
 * and returns the DELTA of received requests between two successive calls.
 *
 * The delta is an equivalent of the "number of images to load on a cycle" for the current JavaBIP logic (to be changed)
 * Endpoint target : GET http://localhost:8082/tools.descartes.teastore.persistence/rest/metrics/requests
 *
 * Excepted response : serialized JSON from tools.spirals.cerberus237.metricscollectorbase.models.ServiceMetrics.
 * The field name are not guarantied so multiple names are tried (for now).
 */
public class PersistenceCollector implements IMetricsCollector<Integer> {
    private static final String[] CANDIDATE_FIELDS = {  // List of JSON fields name candidates for the counter of request
            "receivedRequests",
            "requestCount",
            "totalRequests",
            "count",
            "requests",
            "nbRequests",
            "totalReceivedRequests"
    };

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final URI metricsEndpoint;

    private long previousTotal = -1;
    private String detectedField = null;    // JSON field name detected at first succeed call
    private boolean firstCall = true;       // Logs JSON once to help debuging

    /**
     * @param baseUrl ex: "http://localhost:8082/tools.descartes.teastore.persistence"
     */
    public PersistenceCollector(String baseUrl) {
        this.metricsEndpoint = URI.create(baseUrl + "/rest/metrics/requests");
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.mapper = new ObjectMapper();
    }

    @Override
    public Integer get() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(metricsEndpoint)
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                System.err.printf("[PersistenceCollector] HTTP %d on %s%n",
                        resp.statusCode(), metricsEndpoint);
                return 0;
            }

            String body = resp.body();
            if (firstCall) {
                System.out.printf("[PersistenceCollector] First response body: %s%n", body);
                firstCall = false;
            }

            JsonNode root = mapper.readTree(body);
            long currentTotal = extractRequestCount(root);
            if (currentTotal < 0) {
                return 0;   // nothing found
            }

            int delta;
            if (previousTotal < 0) {
                delta = 0; // initialize
            } else {
                delta = (int) Math.max(0, currentTotal - previousTotal);
            }
            previousTotal = currentTotal;
            return delta;

        } catch (Exception e) {
            System.err.printf("[PersistenceCollector] error: %s%n", e.getMessage());
            return 0;
        }
    }

    /**
     * Extract the request counter by testing each candidate field name
     *
     * @return  -1 if no numeric field is found, otherwise the value of the detected field
     */
    private long extractRequestCount(JsonNode root) {
        // continues on the last detected field
        if (detectedField != null) {
            JsonNode n = root.get(detectedField);
            if (n != null && n.isNumber()) {
                return n.asLong();
            }
            // the field is gone, looking for another one
            detectedField = null;
        }

        for (String field : CANDIDATE_FIELDS) {
            JsonNode n = root.get(field);
            if (n != null && n.isNumber()) {
                detectedField = field;
                System.out.printf("[PersistenceCollector] Using JSON field '%s'%n", field);
                return n.asLong();
            }
        }

        System.err.printf("[PersistenceCollector] No known numeric field found in: %s%n"
                        + "  -> Add the correct field name to CANDIDATE_FIELDS.%n", root);
        return -1;
    }
}
