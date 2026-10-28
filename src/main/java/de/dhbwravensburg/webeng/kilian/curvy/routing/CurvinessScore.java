package de.dhbwravensburg.webeng.kilian.curvy.routing;

/** Kurvenbewertung mit dem Rohwert in Grad pro Kilometer und dem Score von 0 bis 100. */
public record CurvinessScore(
        double distanceKm,
        double totalTurnAngleDegrees,
        double degreesPerKm,
        double score
) {
}
