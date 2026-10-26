package de.dhbwravensburg.webeng.kilian.curvy.routing;

/** Ergebnis der Kurvenbewertung; der Score hat die Einheit Grad pro Kilometer. */
public record CurvinessScore(double distanceKm, double totalTurnAngleDegrees, double score) {
}
