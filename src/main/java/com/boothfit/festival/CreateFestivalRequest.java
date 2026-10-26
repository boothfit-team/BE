package com.boothfit.festival;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

record CreateFestivalRequest(
        String name,
        List<LocalDate> operatingDates,
        LocalTime startTime,
        LocalTime endTime) {
}
