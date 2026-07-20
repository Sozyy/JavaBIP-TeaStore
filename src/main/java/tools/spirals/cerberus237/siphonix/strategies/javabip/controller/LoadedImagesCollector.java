package tools.spirals.cerberus237.siphonix.strategies.javabip.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tools.spirals.cerberus237.metricscollectorbase.IMetricsCollector;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Collects the real image IDs loaded by the AdaptableTeaStore image service since the last poll.
 */
public class LoadedImagesCollector implements IMetricsCollector<int[]> {
    private static final String ENDPOINT_PATH = "/rest/metrics/loadedImages";

    private static final String[] CANDIDATE_FIELDS = {
        "loadedImageIds",
        "imageIds",
        "images",
        "ids"
    };

    private final String       label;
    private final HttpClient   http;
    private final ObjectMapper mapper;
    private final URI          metricsEndpoint;

    private String  detectedField = null;
    private boolean firstCall     = true;

    /**
     * @param label   Label printed in logs
     * @param baseUrl base URL of the image service (ex "http://localhost:8083/tools.descartes.teastore.image")
     */
    public LoadedImagesCollector(String label, String baseUrl) {
        this.label = label;
        this.metricsEndpoint = URI.create(baseUrl + ENDPOINT_PATH);
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.mapper = new ObjectMapper();
    }

    /**
     * @return the IDs of the images loaded since the last poll, or an empty array if none / on error
     */
    @Override
    public int[] get() {
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
                return new int[0];
            }

            String body = resp.body();
            if (firstCall) {
                System.out.printf("[%sCollector] First response body : %s%n", label, body);
                firstCall = false;
            }

            JsonNode root = mapper.readTree(body);
            return extractImageIds(root);

        } catch (Exception e) {
            System.err.printf("[%sCollector] error : %s%n", label, e.getMessage());
            return new int[0];
        }
    }

    private int[] extractImageIds(JsonNode root) {
        JsonNode arrayNode = root.isArray() ? root : null;

        if (arrayNode == null) {
            if (detectedField != null) {
                JsonNode n = root.get(detectedField);
                if (n != null && n.isArray()) {
                    arrayNode = n;
                } else {
                    detectedField = null;
                }
            }
            if (arrayNode == null) {
                for (String field : CANDIDATE_FIELDS) {
                    JsonNode n = root.get(field);
                    if (n != null && n.isArray()) {
                        detectedField = field;
                        System.out.printf("[%sCollector] Using JSON field '%s'%n", label, field);
                        arrayNode = n;
                        break;
                    }
                }
            }
        }

        if (arrayNode == null) {
            System.err.printf("[%sCollector] No known array field found in : %s%n  -> potential fix : Add the correct field name to CANDIDATE_FIELDS%n", label, root);
            return new int[0];
        }

        int[] ids = new int[arrayNode.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = arrayNode.get(i).asInt();
        }
        return ids;
    }

    @Override
    public String toString() {
        return metricsEndpoint.toString();
    }
}
