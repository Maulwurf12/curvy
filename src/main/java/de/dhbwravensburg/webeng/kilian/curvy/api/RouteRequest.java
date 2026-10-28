package de.dhbwravensburg.webeng.kilian.curvy.api;

public record RouteRequest(Coordinate start, Coordinate destination, Double targetScore) {

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
    }
}
