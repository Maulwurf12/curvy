package de.dhbwravensburg.webeng.kilian.curvy.routing;

import de.dhbwravensburg.webeng.kilian.curvy.api.Coordinate;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurvinessScorerTests {

    private final CurvinessScorer scorer = new CurvinessScorer(180);

    @Test
    void straightRouteHasZeroScoreAndDistanceInKilometers() {
        CurvinessScore result = scorer.score(List.of(
                new Coordinate(0.0, 0.0), new Coordinate(0.0, 1.0), new Coordinate(0.0, 2.0)));

        assertEquals(222.39016, result.distanceKm(), 0.00001);
        assertEquals(0, result.score(), 1e-10);
    }

    @Test
    void leftAndRightTurnsHaveTheSameAbsoluteScore() {
        for (double latitude : new double[]{-1, 1}) {
            CurvinessScore result = scorer.score(List.of(
                    new Coordinate(0.0, 0.0), new Coordinate(0.0, 1.0), new Coordinate(latitude, 1.0)));

            assertEquals(90, result.totalTurnAngleDegrees(), 1e-10);
            assertEquals(222.39016, result.distanceKm(), 0.00001);
            assertEquals(0.404694, result.degreesPerKm(), 0.000001);
            assertEquals(0.224830, result.score(), 0.000001);
        }
    }

    @Test
    void normalizesKnownCurvesAndCapsTheScoreAt100() {
        // Jede Strecke hat genau eine 90°-Kurve und die angegebene Länge.
        double[][] examples = {
                {2.0, 45.0, 25.0},
                {1.0, 90.0, 50.0},
                {2.0 / 3, 135.0, 75.0},
                {0.5, 180.0, 100.0},
                {0.25, 360.0, 100.0}
        };

        for (double[] example : examples) {
            CurvinessScore result = scorer.score(rightAngleRouteWithLengthKm(example[0]));

            assertEquals(example[0], result.distanceKm(), 1e-8);
            assertEquals(90, result.totalTurnAngleDegrees(), 1e-8);
            assertEquals(example[1], result.degreesPerKm(), 1e-8);
            assertEquals(example[2], result.score(), 1e-8);
        }
    }

    @Test
    void referenceValueChangesTheScaleWhilePreservingTheRawMeasurement() {
        var route = rightAngleRouteWithLengthKm(1);
        CurvinessScore defaultScale = scorer.score(route);
        CurvinessScore higherReference = new CurvinessScorer(360).score(route);

        assertEquals(50, defaultScale.score(), 1e-8);
        assertEquals(25, higherReference.score(), 1e-8);
        assertEquals(defaultScale.degreesPerKm(), higherReference.degreesPerKm());
        assertEquals(defaultScale.distanceKm(), higherReference.distanceKm());
    }

    @Test
    void rejectsNonPositiveAndNonFiniteReferenceValues() {
        for (double reference : new double[]{0, -1, Double.NaN,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new CurvinessScorer(reference));
        }
    }

    @Test
    void scoresAReversalAs180Degrees() {
        CurvinessScore result = scorer.score(List.of(
                new Coordinate(0.0, 0.0), new Coordinate(0.0, 1.0), new Coordinate(0.0, 0.0)));
        assertEquals(180, result.totalTurnAngleDegrees(), 1e-10);
    }

    @Test
    void handlesDirectionWrapAcrossNorthAndTheDateLine() {
        CurvinessScore north = scorer.score(List.of(
                new Coordinate(0.0, 0.0), new Coordinate(1.0, -0.01), new Coordinate(2.0, 0.0)));
        assertTrue(north.totalTurnAngleDegrees() > 1 && north.totalTurnAngleDegrees() < 2);

        CurvinessScore dateLine = scorer.score(List.of(
                new Coordinate(0.0, 179.0), new Coordinate(0.0, 180.0), new Coordinate(0.0, -179.0)));
        assertEquals(222.39016, dateLine.distanceKm(), 0.00001);
        assertEquals(0, dateLine.score(), 1e-10);
    }

    @Test
    void doesNotCountEarthCurvatureAsATurnOnAGreatCircle() {
        CurvinessScore result = scorer.score(List.of(
                new Coordinate(60.0, -45.0), new Coordinate(67.7923457014035, 0.0),
                new Coordinate(60.0, 45.0)));
        assertEquals(0, result.totalTurnAngleDegrees(), 1e-10);
    }

    @Test
    void ignoresDuplicatePointsIncludingAtATurn() {
        Coordinate start = new Coordinate(0.0, 0.0);
        Coordinate corner = new Coordinate(0.0, 1.0);
        Coordinate end = new Coordinate(1.0, 1.0);
        assertEquals(scorer.score(List.of(start, corner, end)),
                scorer.score(List.of(start, start, corner, corner, end, end)));
    }

    @Test
    void handlesEmptyShortAndZeroLengthRoutes() {
        Coordinate point = new Coordinate(47.678, 9.173);
        CurvinessScore zero = new CurvinessScore(0, 0, 0, 0);
        assertEquals(zero, scorer.score(List.of()));
        assertEquals(zero, scorer.score(List.of(point)));
        assertEquals(zero, scorer.score(List.of(point, point, point)));
        CurvinessScore twoPoints = scorer.score(List.of(point, new Coordinate(47.7, 9.2)));
        assertTrue(twoPoints.distanceKm() > 0);
        assertEquals(0, twoPoints.score());
    }

    @Test
    void rejectsMissingAndInvalidCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> scorer.score(null));
        assertThrows(IllegalArgumentException.class, () -> scorer.score(Arrays.asList((Coordinate) null)));
        for (Coordinate point : List.of(new Coordinate(Double.NaN, 0.0),
                new Coordinate(0.0, Double.POSITIVE_INFINITY), new Coordinate(91.0, 0.0),
                new Coordinate(0.0, 181.0), new Coordinate(null, 0.0))) {
            assertThrows(IllegalArgumentException.class, () -> scorer.score(List.of(point)));
        }
    }

    private List<Coordinate> rightAngleRouteWithLengthKm(double lengthKm) {
        double halfLengthInDegrees = Math.toDegrees(lengthKm / 2 / 6371.0088);
        return List.of(new Coordinate(0.0, 0.0), new Coordinate(0.0, halfLengthInDegrees),
                new Coordinate(halfLengthInDegrees, halfLengthInDegrees));
    }
}
