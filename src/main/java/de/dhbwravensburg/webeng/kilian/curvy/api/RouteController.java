package de.dhbwravensburg.webeng.kilian.curvy.api;

import de.dhbwravensburg.webeng.kilian.curvy.routing.BRouterService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final BRouterService bRouterService;

    public RouteController(BRouterService bRouterService) {
        this.bRouterService = bRouterService;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<Coordinate>> calculateRoute(
            @RequestBody RouteRequest request) {

        request.validate();

        String geoJson = bRouterService.calculateRoute(request);

        List<Coordinate> coordinates =
                bRouterService.extractCoordinates(geoJson);

        return ResponseEntity.ok(coordinates);
    }
}
