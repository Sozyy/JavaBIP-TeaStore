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
 * Generic collector for any TeaStore service that exposes
 * GET /rest/metrics/requests (persistence, image, webui, …).
 *
 * Returns the DELTA of received requests between two successive calls.
 */
public class ServiceCollector implements IMetricsCollector<Integer> {
    private static final String[] CANDIDATE_FIELDS = {
        "receivedRequests",
        "requestCount",
        "totalRequests",
        "count",
        "requests",
        "nbRequests",
        "totalReceivedRequests"
    };

    private final String       label;
    private final HttpClient   http;
    private final ObjectMapper mapper;
    private final URI          metricsEndpoint;

    private long    previousTotal = -1;
    private String  detectedField = null;
    private boolean firstCall     = true;

    /**
     * @param label       Label printed in logs
     * @param baseUrl     base URL of the entry point (ex "http://localhost:8080/tools.descartes.teastore.webui")
     * @param metricsPath path appended to baseUrl (ex "/rest/metrics/requests")
     */
    public ServiceCollector(String label, String baseUrl, String metricsPath) {
        this.label = label;
        this.metricsEndpoint = URI.create(baseUrl + metricsPath);
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.mapper = new ObjectMapper();
    }

    /**
     * Retrieves the requests from the given service since the last poll
     * @return the delta of requests since last poll, or 0 if no new requests or an error occurred
     */
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
                System.err.printf("[%sCollector] HTTP %d on %s%n", label, resp.statusCode(), metricsEndpoint);
                return 0;
            }

            String body = resp.body();
            if (firstCall) {
                System.out.printf("[%sCollector] First response body : %s%n", label, body);
                firstCall = false;
            }

            JsonNode root = mapper.readTree(body);
            long currentTotal = extractRequestCount(root);
            if (currentTotal < 0) {
                return 0;
            } 

            int delta;
            if (previousTotal < 0) {
                delta = 0;
            } else {
                delta = (int) Math.max(0, currentTotal - previousTotal);
            }
            previousTotal = currentTotal;
            return delta;

        } catch (Exception e) {
            System.err.printf("[%sCollector] error : %s%n", label, e.getMessage());
            return 0;
        }
    }

    /**
     * Extracts the total request count from the JSON response
     * @param root the root node of the JSON response
     * @return the total request count, or -1 if not found
     */
    private long extractRequestCount(JsonNode root) {
        if (detectedField != null) {
            JsonNode n = root.get(detectedField);
            if (n != null && n.isNumber()) {
                return n.asLong();
            }
            detectedField = null;
        }

        for (String field : CANDIDATE_FIELDS) {
            JsonNode n = root.get(field);
            if (n != null && n.isNumber()) {
                detectedField = field;
                System.out.printf("[%sCollector] Using JSON field '%s'%n", label, field);
                return n.asLong();
            }
        }

        System.err.printf("[%sCollector] No known numeric field found in : %s%n  -> potential fix : Add the correct field name to CANDIDATE_FIELDS%n", label, root);
        return -1;
    }

    @Override
    public String toString() {
        return metricsEndpoint.toString();
    }
}
