package com.boothfit.festival;

import java.net.URI;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"local", "test"})
@RequestMapping("/api/festivals")
class FestivalWriteController {

    private final FestivalService service;

    FestivalWriteController(FestivalService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<FestivalDetail> create(@RequestBody CreateFestivalRequest request) {
        FestivalDetail festival = service.create(request);
        return ResponseEntity.created(URI.create("/api/festivals/" + festival.id())).body(festival);
    }
}
