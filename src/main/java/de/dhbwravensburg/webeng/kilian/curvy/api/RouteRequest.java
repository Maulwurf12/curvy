package de.dhbwravensburg.webeng.kilian.curvy.api;

public record RouteRequest(Coordinate start, Coordinate destination) {

    public void validate() {
        if (start == null || destination == null) {
            throw new IllegalArgumentException("Start und Ziel müssen angegeben werden.");
        }
        start.validate("start");
        destination.validate("destination");
    }
}
