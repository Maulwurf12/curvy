package de.dhbwravensburg.webeng.kilian.curvy.api;

public record Coordinate(Double latitude, Double longitude) {

    public void validate(String name) {
        if (latitude == null || longitude == null
                || !Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException(
                    name + " muss gültige Koordinaten enthalten (Breitengrad -90 bis 90, Längengrad -180 bis 180)."
            );
        }
    }
}
