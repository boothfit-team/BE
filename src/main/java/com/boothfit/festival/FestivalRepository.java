package com.boothfit.festival;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
class FestivalRepository {

    private static final String SELECT_FESTIVALS = """
            SELECT f.id, f.name, f.start_time, f.end_time, d.operating_date
            FROM festivals f
            LEFT JOIN festival_operating_days d ON d.festival_id = f.id
            """;

    private final JdbcTemplate jdbcTemplate;

    FestivalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<FestivalDetail> findAll() {
        return jdbcTemplate.query(
                SELECT_FESTIVALS + " ORDER BY f.id, d.operating_date",
                FestivalRepository::readFestivals);
    }

    Optional<FestivalDetail> findById(long id) {
        List<FestivalDetail> festivals = jdbcTemplate.query(
                SELECT_FESTIVALS + " WHERE f.id = ? ORDER BY d.operating_date",
                FestivalRepository::readFestivals,
                id);
        return festivals.stream().findFirst();
    }

    long insert(String name, LocalTime startTime, LocalTime endTime) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO festivals (name, start_time, end_time) VALUES (?, ?, ?)",
                    new String[] {"id"});
            statement.setString(1, name);
            statement.setObject(2, startTime);
            statement.setObject(3, endTime);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    void insertOperatingDates(long festivalId, List<LocalDate> operatingDates) {
        for (LocalDate operatingDate : operatingDates) {
            jdbcTemplate.update(
                    "INSERT INTO festival_operating_days (festival_id, operating_date) VALUES (?, ?)",
                    festivalId,
                    operatingDate);
        }
    }

    private static List<FestivalDetail> readFestivals(ResultSet resultSet) throws SQLException {
        Map<Long, FestivalRows> byId = new LinkedHashMap<>();
        while (resultSet.next()) {
            long id = resultSet.getLong("id");
            FestivalRows rows = byId.get(id);
            if (rows == null) {
                rows = new FestivalRows(
                        id,
                        resultSet.getString("name"),
                        resultSet.getObject("start_time", LocalTime.class),
                        resultSet.getObject("end_time", LocalTime.class));
                byId.put(id, rows);
            }
            LocalDate operatingDate = resultSet.getObject("operating_date", LocalDate.class);
            if (operatingDate != null) {
                rows.operatingDates.add(operatingDate);
            }
        }
        return byId.values().stream().map(FestivalRows::toDetail).toList();
    }

    private static final class FestivalRows {
        private final long id;
        private final String name;
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final List<LocalDate> operatingDates = new ArrayList<>();

        private FestivalRows(long id, String name, LocalTime startTime, LocalTime endTime) {
            this.id = id;
            this.name = name;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        private FestivalDetail toDetail() {
            return new FestivalDetail(id, name, List.copyOf(operatingDates), startTime, endTime);
        }
    }
}
