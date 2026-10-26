package de.dhbwravensburg.webeng.kilian.curvy.routing;

import de.dhbwravensburg.webeng.kilian.curvy.api.Coordinate;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurvinessScorerTests {

    private final CurvinessScorer scorer = new CurvinessScorer();

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
            assertEquals(0.404694, result.score(), 0.000001);
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
        CurvinessScore zero = new CurvinessScore(0, 0, 0);
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
}
