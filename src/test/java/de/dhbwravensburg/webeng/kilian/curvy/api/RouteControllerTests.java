package de.dhbwravensburg.webeng.kilian.curvy.api;

import de.dhbwravensburg.webeng.kilian.curvy.routing.BRouterService;
import de.dhbwravensburg.webeng.kilian.curvy.routing.CurvinessScorer;
import de.dhbwravensburg.webeng.kilian.curvy.routing.CurvyRoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RouteControllerTests {

    private static final String REQUEST = """
            {
              "start": {"latitude": 0.0, "longitude": 0.0},
              "destination": {"latitude": 0.0, "longitude": 2.0}
            }
            """;

    private MockRestServiceServer server;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://brouter.example");
        server = MockRestServiceServer.bindTo(builder).build();
        var routingService = new CurvyRoutingService(
                new BRouterService(builder.build()), new CurvinessScorer());
        mockMvc = MockMvcBuilders.standaloneSetup(new RouteController(routingService))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void returnsTheWinningGeometryScoreAndAlternativeIndexAsJson() throws Exception {
        for (int index = 0; index < 3; index++) {
            String coordinates = index == 1 ? "[[0,0,400],[1,0,410],[1,1,420],[2,0,430]]"
                    : "[[0,0],[1,0],[2,0]]";
            server.expect(requestTo(url(index))).andRespond(withSuccess(
                    "{\"features\":[{\"geometry\":{\"type\":\"LineString\",\"coordinates\":"
                            + coordinates + "}}]}", MediaType.APPLICATION_JSON));
        }

        mockMvc.perform(post("/api/routes").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alternativeIdx").value(1))
                .andExpect(jsonPath("$.coordinates.length()").value(4))
                .andExpect(jsonPath("$.coordinates[2].latitude").value(1.0))
                .andExpect(jsonPath("$.coordinates[2].longitude").value(1.0))
                .andExpect(jsonPath("$.curviness.distanceKm").isNumber())
                .andExpect(jsonPath("$.curviness.totalTurnAngleDegrees").isNumber())
                .andExpect(jsonPath("$.curviness.score").isNumber());
        server.verify();
    }

    @Test
    void returnsBadGatewayWhenAllAlternativesFail() throws Exception {
        for (int index = 0; index < 3; index++) {
            server.expect(requestTo(url(index))).andRespond(withStatus(HttpStatus.BAD_REQUEST));
        }

        mockMvc.perform(post("/api/routes").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
        server.verify();
    }

    @Test
    void returnsBadGatewayWhenAllRoutesContainInvalidGeoJson() throws Exception {
        for (int index = 0; index < 3; index++) {
            server.expect(requestTo(url(index)))
                    .andRespond(withSuccess("{\"features\":[]}", MediaType.APPLICATION_JSON));
        }

        mockMvc.perform(post("/api/routes").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isBadGateway());
        server.verify();
    }

    @Test
    void rejectsInvalidRequestsWithoutCallingBRouter() throws Exception {
        mockMvc.perform(post("/api/routes").contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST.replace("\"latitude\": 0.0", "\"latitude\": 91.0")))
                .andExpect(status().isBadRequest());
        server.verify();
    }

    private String url(int index) {
        return "https://brouter.example/brouter?lonlats=0.0,0.0%7C2.0,0.0"
                + "&profile=car-fast&alternativeidx=" + index + "&format=geojson";
    }
}
