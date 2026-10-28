package de.dhbwravensburg.webeng.kilian.curvy.api;

import de.dhbwravensburg.webeng.kilian.curvy.routing.BRouterService;
import de.dhbwravensburg.webeng.kilian.curvy.routing.CurvinessScore;
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
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RouteFixtureTests {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final double TOLERANCE = 1e-8;

    private MockRestServiceServer server;
    private MockMvc mockMvc;
    private BRouterService bRouterService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://brouter.example");
        server = MockRestServiceServer.bindTo(builder).build();
        bRouterService = new BRouterService(builder.build());
        var routingService = new CurvyRoutingService(bRouterService, new CurvinessScorer(180));
        mockMvc = MockMvcBuilders.standaloneSetup(new RouteController(routingService))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void fixtureGeometriesMatchIndependentlyCalculatedScores() throws IOException {
        var scorer = new CurvinessScorer(180);
        for (int index = 0; index < 3; index++) {
            var coordinates = bRouterService.extractCoordinates(fixture("alternative-" + index + ".geojson"));
            assertScore(expectedScore(index), scorer.score(coordinates));
        }
    }

    @Test
    void apiReturnsTheExpectedWinningRoute() throws Exception {
        for (int index = 0; index < 3; index++) {
            server.expect(requestTo(url(index))).andRespond(withSuccess(
                    fixture("alternative-" + index + ".geojson"), MediaType.APPLICATION_JSON));
        }

        RouteResponse actual = calculateRoute();
        RouteResponse expected = JSON.readValue(fixture("expected-response.json"), RouteResponse.class);

        assertEquals(expected.alternativeIdx(), actual.alternativeIdx());
        assertEquals(expected.coordinates(), actual.coordinates());
        assertEquals(expected.targetScore(), actual.targetScore());
        assertEquals(expected.scoreDeviation(), actual.scoreDeviation());
        assertScore(expected.curviness(), actual.curviness());
        server.verify();
    }

    @Test
    void apiSelectsNextBestCandidateWhenTheWinnerIsUnavailable() throws Exception {
        for (int index = 0; index < 3; index++) {
            var expectation = server.expect(requestTo(url(index)));
            if (index == 1) {
                expectation.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
            } else {
                expectation.andRespond(withSuccess(
                        fixture("alternative-" + index + ".geojson"), MediaType.APPLICATION_JSON));
            }
        }

        RouteResponse actual = calculateRoute();

        assertEquals(2, actual.alternativeIdx());
        assertEquals(bRouterService.extractCoordinates(fixture("alternative-2.geojson")), actual.coordinates());
        assertScore(expectedScore(2), actual.curviness());
        server.verify();
    }

    private RouteResponse calculateRoute() throws Exception {
        var result = mockMvc.perform(post("/api/routes").contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("request.json")))
                .andExpect(status().isOk()).andReturn();
        return JSON.readValue(result.getResponse().getContentAsString(), RouteResponse.class);
    }

    private CurvinessScore expectedScore(int index) throws IOException {
        return JSON.treeToValue(JSON.readTree(fixture("expected-scores.json")).path(index), CurvinessScore.class);
    }

    private void assertScore(CurvinessScore expected, CurvinessScore actual) {
        assertEquals(expected.distanceKm(), actual.distanceKm(), TOLERANCE, "Streckenlänge in km");
        assertEquals(expected.totalTurnAngleDegrees(), actual.totalTurnAngleDegrees(), TOLERANCE, "Winkelsumme");
        assertEquals(expected.degreesPerKm(), actual.degreesPerKm(), TOLERANCE, "Grad pro km");
        assertEquals(expected.score(), actual.score(), TOLERANCE, "Score von 0 bis 100");
    }

    private String fixture(String filename) throws IOException {
        try (var input = getClass().getResourceAsStream("/routing/fixtures/" + filename)) {
            assertNotNull(input, "Testdatei fehlt: " + filename);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String url(int index) {
        return "https://brouter.example/brouter?lonlats=0.0,0.0%7C1.0,1.0"
                + "&profile=car-fast&alternativeidx=" + index + "&format=geojson";
    }
}
