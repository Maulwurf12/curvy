package de.dhbwravensburg.webeng.kilian.curvy.routing;

import de.dhbwravensburg.webeng.kilian.curvy.api.Coordinate;
import de.dhbwravensburg.webeng.kilian.curvy.api.RouteRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

@Service
public class BRouterService {

    private final RestClient restClient;
    // Standardmäßige festlegung, weil nötig für brouter, kann aber auch angepasst werden, wenn andere Profile benötigt werden
    private static final String ROUTING_PROFILE = "car-fast";

    public BRouterService(RestClient restClient) {
        this.restClient = restClient;
    }

    public String calculateRoute(RouteRequest request) {
        Coordinate start = request.start();
        Coordinate destination = request.destination();

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/brouter")
                        .queryParam(
                                "lonlats",
                                start.longitude() + "," + start.latitude()
                                        + "|" + destination.longitude() + "," + destination.latitude()
                        )
                        .queryParam("profile", ROUTING_PROFILE)
                        .queryParam("alternativeidx", 0)
                        .queryParam("format", "geojson")
                        .build())
                .retrieve()
                .body(String.class);
    }
    private static final JsonMapper JSON_MAPPER =
            JsonMapper.builder().build();

    public List<Coordinate> extractCoordinates(String geoJson) {

        JsonNode root = JSON_MAPPER.readTree(geoJson);

        JsonNode coordinates = root
                .path("features")
                .path(0)
                .path("geometry")
                .path("coordinates");

        if (!coordinates.isArray()) {
            throw new IllegalArgumentException(
                    "Keine Streckenkoordinaten gefunden."
            );
        }

        List<Coordinate> points = new ArrayList<>();

        for (JsonNode point : coordinates) {
            if (!point.isArray() || point.size() < 2) {
                throw new IllegalArgumentException(
                        "Ungültiger Streckenpunkt."
                );
            }

            double longitude = point.get(0).asDouble();
            double latitude = point.get(1).asDouble();

            points.add(new Coordinate(latitude, longitude));
        }

        return points;
    }
}
