package de.dhbwravensburg.webeng.kilian.curvy.routing;

import de.dhbwravensburg.webeng.kilian.curvy.api.Coordinate;
import de.dhbwravensburg.webeng.kilian.curvy.api.RouteRequest;
import de.dhbwravensburg.webeng.kilian.curvy.api.RouteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CurvyRoutingServiceTests {

    // Alle Kandidaten haben denselben Start und dasselbe Ziel.
    private static final String STRAIGHT = "[[0,0],[1,0],[2,0]]";
    private static final String BENT = "[[0,0],[1,1],[2,0]]";
    private static final String CURVY = "[[0,0],[0,1],[1,1],[1,0],[2,0]]";
    private static final RouteRequest REQUEST = new RouteRequest(
            new Coordinate(0.0, 0.0), new Coordinate(0.0, 2.0));

    private MockRestServiceServer server;
    private CurvyRoutingService service;
    private BRouterService bRouterService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://brouter.example");
        server = MockRestServiceServer.bindTo(builder).build();
        bRouterService = new BRouterService(builder.build());
        service = new CurvyRoutingService(bRouterService, new CurvinessScorer());
    }

    @Test
    void selectsHighestScoreRegardlessOfAlternativeIndex() {
        for (int winner = 0; winner < 3; winner++) {
            server.reset();
            for (int index = 0; index < 3; index++) {
                expectRoute(index, index == winner ? CURVY : BENT);
            }

            RouteResponse result = service.calculateRoute(REQUEST);

            assertEquals(winner, result.alternativeIdx());
            assertEquals(bRouterService.extractCoordinates(geoJson(CURVY)), result.coordinates());
            assertEquals(new CurvinessScorer().score(result.coordinates()), result.curviness());
            server.verify();
        }
    }

    @Test
    void choosesByCurvinessInsteadOfLength() {
        expectRoute(0, STRAIGHT);
        expectRoute(1, CURVY);
        expectRoute(2, BENT);

        assertEquals(1, service.calculateRoute(REQUEST).alternativeIdx());
        server.verify();
    }

    @Test
    void choosesAnglePerKilometerInsteadOfTotalAngle() {
        // Der lange Umweg hat mehr Kurvenwinkel, aber weniger Winkel pro Kilometer.
        expectRoute(0, "[[0,0],[0,10],[1,10],[1,0],[2,0]]");
        expectRoute(1, BENT);
        expectRoute(2, STRAIGHT);

        assertEquals(1, service.calculateRoute(REQUEST).alternativeIdx());
        server.verify();
    }

    @Test
    void keepsLowestIndexOnEqualScoresIncludingDuplicateRoutes() {
        expectRoute(0, BENT);
        expectRoute(1, BENT);
        expectRoute(2, BENT);

        assertEquals(0, service.calculateRoute(REQUEST).alternativeIdx());
        server.verify();
    }

    @Test
    void keepsZeroScoreAsAValidCandidateWhenAlternativesAreUnavailable() {
        expectRoute(0, STRAIGHT);
        expect(1, withStatus(HttpStatus.BAD_REQUEST));
        expect(2, withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        RouteResponse result = service.calculateRoute(REQUEST);

        assertEquals(0, result.alternativeIdx());
        assertEquals(0, result.curviness().score());
        server.verify();
    }

    @Test
    void continuesAfterAnHttpFailureAndMalformedGeoJson() {
        expect(0, withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        expect(1, withSuccess("not json", MediaType.APPLICATION_JSON));
        expectRoute(2, BENT);

        assertEquals(2, service.calculateRoute(REQUEST).alternativeIdx());
        server.verify();
    }

    @Test
    void ignoresInvalidCoordinatesAndZeroLengthRoutes() {
        expectRoute(0, "[[0,0],[1,91],[2,0]]");
        expectRoute(1, "[[0,0],[0,0],[0,0]]");
        expectRoute(2, STRAIGHT);

        assertEquals(2, service.calculateRoute(REQUEST).alternativeIdx());
        server.verify();
    }

    @Test
    void continuesAfterANetworkFailureAndAnEmptyResponse() {
        expect(0, withException(new IOException("Connection timed out")));
        expect(1, withSuccess());
        expectRoute(2, CURVY);

        assertEquals(2, service.calculateRoute(REQUEST).alternativeIdx());
        server.verify();
    }

    @Test
    void failsWhenNoAlternativeIsUsable() {
        expect(0, withStatus(HttpStatus.BAD_REQUEST));
        expect(1, withSuccess("{\"features\":[]}", MediaType.APPLICATION_JSON));
        expectRoute(2, "[[0,0]]");

        assertThrows(RestClientException.class, () -> service.calculateRoute(REQUEST));
        server.verify();
    }

    @Test
    void rejectsInvalidRequestsBeforeMakingAnyUpstreamCall() {
        assertThrows(IllegalArgumentException.class, () -> service.calculateRoute(
                new RouteRequest(new Coordinate(91.0, 0.0), REQUEST.destination())));
        server.verify();
    }

    private void expectRoute(int index, String coordinates) {
        expect(index, withSuccess(geoJson(coordinates), MediaType.APPLICATION_JSON));
    }

    private void expect(int index, ResponseCreator response) {
        server.expect(requestTo("https://brouter.example/brouter?lonlats=0.0,0.0%7C2.0,0.0"
                + "&profile=car-fast&alternativeidx=" + index + "&format=geojson"))
                .andRespond(response);
    }

    private String geoJson(String coordinates) {
        return "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\","
                + "\"geometry\":{\"type\":\"LineString\",\"coordinates\":" + coordinates + "}}]}";
    }
}
