package com.boothfit.festival;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record FestivalDetail(
        long id,
        String name,
        List<LocalDate> operatingDates,
        LocalTime startTime,
        LocalTime endTime) {

    FestivalSummary toSummary() {
        return new FestivalSummary(id, name, operatingDates);
    }
}
