package de.dhbwravensburg.webeng.kilian.curvy.routing;

import de.dhbwravensburg.webeng.kilian.curvy.api.Coordinate;
import de.dhbwravensburg.webeng.kilian.curvy.api.RouteRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

class BRouterServiceTests {

    @Test
    void calculatesGeoJsonRouteWithFixedProfile() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://brouter.example");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(
                        "https://brouter.example/brouter?lonlats=9.173,47.678%7C10.7,47.566"
                                        + "&profile=car-fast&alternativeidx=0&format=geojson"
                ))
                .andExpect(method(GET))
                .andExpect(queryParam("lonlats", "9.173,47.678%7C10.7,47.566"))
                .andExpect(queryParam("profile", "car-fast"))
                .andExpect(queryParam("alternativeidx", "0"))
                .andExpect(queryParam("format", "geojson"))
                .andRespond(withSuccess("{\"type\":\"FeatureCollection\",\"features\":[]}", MediaType.APPLICATION_JSON));

        BRouterService service = new BRouterService(builder.build());
        String geoJson = service.calculateRoute(new RouteRequest(
                new Coordinate(47.678, 9.173),
                new Coordinate(47.566, 10.7)
        ));

        assertEquals("{\"type\":\"FeatureCollection\",\"features\":[]}", geoJson);
        server.verify();
    }
}
