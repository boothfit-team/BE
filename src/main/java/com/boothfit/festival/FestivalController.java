package com.boothfit.festival;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/festivals")
class FestivalController {

    private final FestivalService service;

    FestivalController(FestivalService service) {
        this.service = service;
    }

    @GetMapping
    List<FestivalSummary> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    FestivalDetail findById(@PathVariable long id) {
        return service.findById(id);
    }
}
