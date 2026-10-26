package com.boothfit.festival;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class FestivalService {

    private final FestivalRepository repository;

    FestivalService(FestivalRepository repository) {
        this.repository = repository;
    }

    List<FestivalSummary> findAll() {
        return repository.findAll().stream().map(FestivalDetail::toSummary).toList();
    }

    FestivalDetail findById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Festival not found"));
    }

    @Transactional
    public FestivalDetail create(CreateFestivalRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()
                || request.name().length() > 200 || request.startTime() == null || request.endTime() == null
                || !request.startTime().isBefore(request.endTime()) || request.operatingDates() == null
                || request.operatingDates().isEmpty() || request.operatingDates().stream().anyMatch(date -> date == null)
                || request.operatingDates().stream().distinct().count() != request.operatingDates().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid festival data");
        }

        List<LocalDate> operatingDates = request.operatingDates().stream().sorted().toList();
        long id = repository.insert(request.name().trim(), request.startTime(), request.endTime());
        repository.insertOperatingDates(id, operatingDates);
        return new FestivalDetail(id, request.name().trim(), operatingDates, request.startTime(), request.endTime());
    }
}
