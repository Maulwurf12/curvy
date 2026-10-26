package de.dhbwravensburg.webeng.kilian.curvy.routing;

import de.dhbwravensburg.webeng.kilian.curvy.api.Coordinate;
import de.dhbwravensburg.webeng.kilian.curvy.api.RouteRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

@Service
public class BRouterService {

    private static final String ROUTING_PROFILE = "car-fast";
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private final RestClient restClient;

    public BRouterService(RestClient restClient) {
        this.restClient = restClient;
    }

    public String calculateRoute(RouteRequest request, int alternativeIdx) {
        if (alternativeIdx < 0) {
            throw new IllegalArgumentException("Der Alternativindex darf nicht negativ sein.");
        }
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
                        .queryParam("alternativeidx", alternativeIdx)
                        .queryParam("format", "geojson")
                        .build())
                .retrieve()
                .body(String.class);
    }

    public List<Coordinate> extractCoordinates(String geoJson) {
        if (geoJson == null || geoJson.isBlank()) {
            throw new RestClientException("BRouter hat keine Route geliefert.");
        }
        JsonNode root;
        try {
            root = JSON_MAPPER.readTree(geoJson);
        } catch (JacksonException exception) {
            throw new RestClientException("BRouter hat kein gültiges GeoJSON geliefert.", exception);
        }

        JsonNode geometry = root.path("features").path(0).path("geometry");
        if (!geometry.path("type").isString()
                || !"LineString".equals(geometry.path("type").asString())) {
            throw new RestClientException("BRouter hat keine LineString-Route geliefert.");
        }

        JsonNode coordinates = geometry.path("coordinates");

        if (!coordinates.isArray() || coordinates.size() < 2) {
            throw new RestClientException(
                    "Keine gültigen Streckenkoordinaten gefunden."
            );
        }

        List<Coordinate> points = new ArrayList<>();

        for (JsonNode point : coordinates) {
            if (!point.isArray() || point.size() < 2
                    || !point.get(0).isNumber() || !point.get(1).isNumber()) {
                throw new RestClientException(
                        "Ungültiger Streckenpunkt."
                );
            }

            double longitude = point.get(0).asDouble();
            double latitude = point.get(1).asDouble();

            Coordinate coordinate = new Coordinate(latitude, longitude);
            try {
                coordinate.validate("Streckenpunkt");
            } catch (IllegalArgumentException exception) {
                throw new RestClientException("BRouter hat ungültige Koordinaten geliefert.", exception);
            }
            points.add(coordinate);
        }

        return points;
    }
}
