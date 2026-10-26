package de.dhbwravensburg.webeng.kilian.curvy.api;

import de.dhbwravensburg.webeng.kilian.curvy.routing.CurvinessScore;

import java.util.List;

public record RouteResponse(List<Coordinate> coordinates, CurvinessScore curviness, int alternativeIdx) {
}
