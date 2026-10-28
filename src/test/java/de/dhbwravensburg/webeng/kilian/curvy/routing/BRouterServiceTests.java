package de.dhbwravensburg.webeng.kilian.curvy.routing;

import de.dhbwravensburg.webeng.kilian.curvy.api.Coordinate;
import de.dhbwravensburg.webeng.kilian.curvy.api.RouteRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

class BRouterServiceTests {

    @Test
    void calculatesGeoJsonRouteWithFixedProfileAndRequestedAlternative() {
        for (int alternativeIdx = 0; alternativeIdx < 3; alternativeIdx++) {
            RestClient.Builder builder = RestClient.builder().baseUrl("https://brouter.example");
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            server.expect(requestTo(
                            "https://brouter.example/brouter?lonlats=9.173,47.678%7C10.7,47.566"
                                    + "&profile=car-fast&alternativeidx=" + alternativeIdx + "&format=geojson"
                    ))
                    .andExpect(method(GET))
                    .andExpect(queryParam("lonlats", "9.173,47.678%7C10.7,47.566"))
                    .andExpect(queryParam("profile", "car-fast"))
                    .andExpect(queryParam("alternativeidx", Integer.toString(alternativeIdx)))
                    .andExpect(queryParam("format", "geojson"))
                    .andRespond(withSuccess("{\"type\":\"FeatureCollection\",\"features\":[]}", MediaType.APPLICATION_JSON));

            BRouterService service = new BRouterService(builder.build());
            String geoJson = service.calculateRoute(new RouteRequest(
                    new Coordinate(47.678, 9.173),
                    new Coordinate(47.566, 10.7), null
            ), alternativeIdx);

            assertEquals("{\"type\":\"FeatureCollection\",\"features\":[]}", geoJson);
            server.verify();
        }
    }

    @Test
    void rejectsNegativeAlternativeIndices() {
        BRouterService service = new BRouterService(RestClient.create());
        assertThrows(IllegalArgumentException.class, () -> service.calculateRoute(new RouteRequest(
                new Coordinate(0.0, 0.0), new Coordinate(0.0, 2.0), null), -1));
    }

    @Test
    void extractsLongitudeLatitudeAndIgnoresElevation() {
        BRouterService service = new BRouterService(RestClient.create());
        assertEquals(List.of(new Coordinate(47.0, 9.0), new Coordinate(48.0, 10.0)),
                service.extractCoordinates(route("[[9,47,400],[10,48,420]]")));
    }

    @Test
    void rejectsEmptyMalformedAndInvalidUpstreamRoutes() {
        BRouterService service = new BRouterService(RestClient.create());
        for (String geoJson : Arrays.asList(null, "", " ", "not json", "null", "[]", "{}",
                "{\"features\":[]}",
                "{\"features\":[{\"geometry\":{\"type\":{},\"coordinates\":[]}}]}",
                route("[]"), route("[[9,47]]"), route("[[9,47],[]]"),
                route("[[9,47],[10,null]]"), route("[[9,47],[\"10\",48]]"),
                route("[[9,47],[10,91]]"), route("[[9,47],[181,48]]"),
                route("[[9,47],[10,1e400]]"),
                route("[[9,47],[10,48]]").replace("LineString", "MultiLineString"))) {
            assertThrows(RestClientException.class, () -> service.extractCoordinates(geoJson),
                    "Ungültige Antwort muss als BRouter-Fehler behandelt werden: " + geoJson);
        }
    }

    private String route(String coordinates) {
        return "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\","
                + "\"geometry\":{\"type\":\"LineString\",\"coordinates\":" + coordinates + "}}]}";
    }
}
