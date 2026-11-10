package de.dhbwravensburg.webeng.kilian.curvy.api;

public record RouteRequest(
        Coordinate start,
        Coordinate destination,
        Double targetScore,
        Double maxDetourFactor
) {

    public RouteRequest(Coordinate start, Coordinate destination, Double targetScore) {
        this(start, destination, targetScore, null);
    }

    public double allowedDetourFactor() {
        return maxDetourFactor == null ? 1.5 : maxDetourFactor;
    }

    public void validate() {
        if (start == null || destination == null) {
            throw new IllegalArgumentException("Start und Ziel müssen angegeben werden.");
        }
        start.validate("start");
        destination.validate("destination");
        if (targetScore != null
                && (!Double.isFinite(targetScore) || targetScore < 0 || targetScore > 100)) {
            throw new IllegalArgumentException("targetScore muss eine Zahl zwischen 0 und 100 sein.");
        }
        if (maxDetourFactor != null
                && (!Double.isFinite(maxDetourFactor) || maxDetourFactor < 1)) {
            throw new IllegalArgumentException("maxDetourFactor muss eine endliche Zahl größer oder gleich 1 sein.");
        }
    }
}
