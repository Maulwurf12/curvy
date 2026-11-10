package de.dhbwravensburg.webeng.kilian.curvy.routing;

import de.dhbwravensburg.webeng.kilian.curvy.api.Coordinate;
import de.dhbwravensburg.webeng.kilian.curvy.api.RouteRequest;
import de.dhbwravensburg.webeng.kilian.curvy.api.RouteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
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

        List<RouteResponse> candidates = new ArrayList<>();
        double shortestDistanceKm = Double.POSITIVE_INFINITY;
        RestClientException lastFailure = null;

        for (int alternativeIdx = 0; alternativeIdx < ALTERNATIVE_COUNT; alternativeIdx++) {
            try {
                String geoJson = bRouterService.calculateRoute(request, alternativeIdx);
                List<Coordinate> coordinates = bRouterService.extractCoordinates(geoJson);
                CurvinessScore curviness = curvinessScorer.score(coordinates);
                if (curviness.distanceKm() == 0) {
                    throw new RestClientException("BRouter hat eine Route ohne Streckenlänge geliefert.");
                }

                Double targetScore = request.targetScore();
                Double scoreDeviation = targetScore == null ? null : Math.abs(curviness.score() - targetScore);
                RouteResponse candidate = new RouteResponse(
                        coordinates, curviness, alternativeIdx, targetScore, scoreDeviation);

                candidates.add(candidate);
                shortestDistanceKm = Math.min(shortestDistanceKm, curviness.distanceKm());
            } catch (RestClientException exception) {
                lastFailure = exception;
                LOG.warn("BRouter-Alternative {} konnte nicht verwendet werden: {}",
                        alternativeIdx, exception.getMessage());
            }
        }

        if (candidates.isEmpty()) {
            throw new RestClientException("Keine gültige BRouter-Route verfügbar.", lastFailure);
        }

        // Erst nach allen Anfragen steht die kürzeste gültige Alternative fest.
        RouteResponse bestRoute = null;
        for (RouteResponse candidate : candidates) {
            double detourFactor = candidate.curviness().distanceKm() / shortestDistanceKm;
            if (detourFactor > request.allowedDetourFactor()) {
                continue;
            }
            if (bestRoute == null || isBetterRoute(candidate, bestRoute)) {
                bestRoute = candidate;
            }
        }
        // Mindestens die kürzeste Route erfüllt jeden erlaubten Faktor (>= 1).
        return bestRoute;
    }

    private boolean isBetterRoute(RouteResponse candidate, RouteResponse currentBest) {
        // Bei Gleichstand bleibt die zuerst geprüfte Alternative erhalten.
        if (candidate.targetScore() != null) {
            return candidate.scoreDeviation() < currentBest.scoreDeviation();
        }

        // Der Rohwert unterscheidet auch besonders kurvige Routen mit jeweils 100 Punkten.
        return candidate.curviness().degreesPerKm() > currentBest.curviness().degreesPerKm();
    }
}
