package de.dhbwravensburg.webeng.kilian.curvy.routing;

import de.dhbwravensburg.webeng.kilian.curvy.api.Coordinate;
import de.dhbwravensburg.webeng.kilian.curvy.api.RouteRequest;
import de.dhbwravensburg.webeng.kilian.curvy.api.RouteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
public class CurvyRoutingService {

    private static final Logger LOG = LoggerFactory.getLogger(CurvyRoutingService.class);
    private static final int ALTERNATIVE_COUNT = 3;

    private final BRouterService bRouterService;
    private final CurvinessScorer curvinessScorer;

    public CurvyRoutingService(BRouterService bRouterService, CurvinessScorer curvinessScorer) {
        this.bRouterService = bRouterService;
        this.curvinessScorer = curvinessScorer;
    }

    public RouteResponse calculateRoute(RouteRequest request) {
        request.validate();

        RouteResponse bestRoute = null;
        RestClientException lastFailure = null;

        for (int alternativeIdx = 0; alternativeIdx < ALTERNATIVE_COUNT; alternativeIdx++) {
            try {
                String geoJson = bRouterService.calculateRoute(request, alternativeIdx);
                List<Coordinate> coordinates = bRouterService.extractCoordinates(geoJson);
                CurvinessScore curviness = curvinessScorer.score(coordinates);
                if (curviness.distanceKm() == 0) {
                    throw new RestClientException("BRouter hat eine Route ohne Streckenlänge geliefert.");
                }

                // Bei gleichem Score bleibt der Kandidat mit dem kleineren Index erhalten.
                if (bestRoute == null || curviness.score() > bestRoute.curviness().score()) {
                    bestRoute = new RouteResponse(coordinates, curviness, alternativeIdx);
                }
            } catch (RestClientException exception) {
                lastFailure = exception;
                LOG.warn("BRouter-Alternative {} konnte nicht verwendet werden: {}",
                        alternativeIdx, exception.getMessage());
            }
        }

        if (bestRoute == null) {
            throw new RestClientException("Keine gültige BRouter-Route verfügbar.", lastFailure);
        }
        return bestRoute;
    }
}
