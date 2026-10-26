package de.dhbwravensburg.webeng.kilian.curvy.routing;

import de.dhbwravensburg.webeng.kilian.curvy.api.Coordinate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CurvinessScorer {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    /** Summiert absolute Richtungswechsel pro Kilometer; überspringt doppelte Punkte. */
    public CurvinessScore score(List<Coordinate> coordinates) {
        if (coordinates == null) {
            throw new IllegalArgumentException("Streckenkoordinaten müssen angegeben werden.");
        }

        double distanceKm = 0;
        double totalTurnAngleDegrees = 0;
        Coordinate previous = null;
        Double arrivalBearing = null;

        for (Coordinate current : coordinates) {
            if (current == null) {
                throw new IllegalArgumentException("Ungültiger Streckenpunkt.");
            }
            current.validate("Streckenpunkt");

            if (previous != null) {
                double segmentKm = distanceKm(previous, current);
                if (segmentKm == 0) {
                    continue;
                }

                distanceKm += segmentKm;
                if (arrivalBearing != null) {
                    double departureBearing = bearingDegrees(previous, current);
                    // Den kleinsten Winkel zwischen -180° und 180° verwenden.
                    totalTurnAngleDegrees += Math.abs(
                            Math.IEEEremainder(departureBearing - arrivalBearing, 360));
                }
                // Ankunftsrichtung auf der Erdkugel aus der Gegenrichtung bestimmen.
                arrivalBearing = bearingDegrees(current, previous) + 180;
            }
            previous = current;
        }

        double score = distanceKm == 0 ? 0 : totalTurnAngleDegrees / distanceKm;
        return new CurvinessScore(distanceKm, totalTurnAngleDegrees, score);
    }

    private double distanceKm(Coordinate from, Coordinate to) {
        double fromLatitude = Math.toRadians(from.latitude());
        double toLatitude = Math.toRadians(to.latitude());
        double halfLatitudeDifference = (toLatitude - fromLatitude) / 2;
        double halfLongitudeDifference = Math.toRadians(to.longitude() - from.longitude()) / 2;

        // Haversine-Formel für die Entfernung auf der Erdkugel.
        double a = Math.pow(Math.sin(halfLatitudeDifference), 2)
                + Math.cos(fromLatitude) * Math.cos(toLatitude)
                * Math.pow(Math.sin(halfLongitudeDifference), 2);
        a = Math.clamp(a, 0, 1);
        return 2 * EARTH_RADIUS_KM * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private double bearingDegrees(Coordinate from, Coordinate to) {
        double fromLatitude = Math.toRadians(from.latitude());
        double toLatitude = Math.toRadians(to.latitude());
        double longitudeDifference = Math.toRadians(to.longitude() - from.longitude());

        double y = Math.sin(longitudeDifference) * Math.cos(toLatitude);
        double x = Math.cos(fromLatitude) * Math.sin(toLatitude)
                - Math.sin(fromLatitude) * Math.cos(toLatitude) * Math.cos(longitudeDifference);
        return Math.toDegrees(Math.atan2(y, x));
    }
}
