package de.dhbwravensburg.webeng.kilian.curvy.api;

import de.dhbwravensburg.webeng.kilian.curvy.routing.CurvyRoutingService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final CurvyRoutingService routingService;

    public RouteController(CurvyRoutingService routingService) {
        this.routingService = routingService;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<RouteResponse> calculateRoute(
            @RequestBody RouteRequest request) {

        return ResponseEntity.ok(routingService.calculateRoute(request));
    }
}
