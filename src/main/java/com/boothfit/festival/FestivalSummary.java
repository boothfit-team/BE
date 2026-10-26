package com.boothfit.festival;

import java.time.LocalDate;
import java.util.List;

public record FestivalSummary(long id, String name, List<LocalDate> operatingDates) {
}
