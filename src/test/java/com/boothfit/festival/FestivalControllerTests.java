package com.boothfit.festival;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FestivalControllerTests {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void insertFestivals() {
        jdbcTemplate.update("DELETE FROM festival_operating_days");
        jdbcTemplate.update("DELETE FROM festivals");
        jdbcTemplate.execute("ALTER TABLE festivals ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.update("INSERT INTO festivals (name, start_time, end_time) VALUES ('Campus Festival', '10:00:00', '18:00:00')");
        jdbcTemplate.update("INSERT INTO festivals (name, start_time, end_time) VALUES ('Autumn Festival', '11:00:00', '17:00:00')");
        jdbcTemplate.update("INSERT INTO festival_operating_days (festival_id, operating_date) VALUES (1, '2026-10-16')");
        jdbcTemplate.update("INSERT INTO festival_operating_days (festival_id, operating_date) VALUES (1, '2026-10-17')");
        jdbcTemplate.update("INSERT INTO festival_operating_days (festival_id, operating_date) VALUES (2, '2026-11-02')");
    }

    @Test
    void listIncludesEachFestivalAndAllOperatingDays() throws Exception {
        HttpResponse<String> response = get("/api/festivals");

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
        assertTrue(response.body().contains("\"id\":1"));
        assertTrue(response.body().contains("\"name\":\"Campus Festival\""));
        assertTrue(response.body().contains("\"operatingDates\":[\"2026-10-16\",\"2026-10-17\"]"));
        assertTrue(response.body().contains("\"id\":2"));
        assertTrue(response.body().contains("\"2026-11-02\""));
    }

    @Test
    void detailIncludesOperatingHoursAndAllDays() throws Exception {
        HttpResponse<String> response = get("/api/festivals/1");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"id\":1"));
        assertTrue(response.body().contains("\"operatingDates\":[\"2026-10-16\",\"2026-10-17\"]"));
        assertTrue(response.body().contains("\"startTime\":\"10:00:00\""));
        assertTrue(response.body().contains("\"endTime\":\"18:00:00\""));
    }

    @Test
    void missingFestivalReturnsNotFound() throws Exception {
        HttpResponse<String> response = get("/api/festivals/999");

        assertEquals(404, response.statusCode());
    }

    @Test
    void localCreateCanBeReadBackWithSortedOperatingDays() throws Exception {
        HttpResponse<String> created = post("""
                {"name":"New Festival","operatingDates":["2026-11-04","2026-11-03"],
                 "startTime":"09:00:00","endTime":"16:00:00"}
                """);

        assertEquals(201, created.statusCode());
        String location = created.headers().firstValue("Location").orElseThrow();
        assertTrue(location.startsWith("/api/festivals/"));

        HttpResponse<String> detail = get(location);
        assertEquals(200, detail.statusCode());
        assertTrue(detail.body().contains("\"name\":\"New Festival\""));
        assertTrue(detail.body().contains("\"operatingDates\":[\"2026-11-03\",\"2026-11-04\"]"));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM festival_operating_days WHERE festival_id = ?",
                Integer.class,
                Long.parseLong(location.substring(location.lastIndexOf('/') + 1))));
    }

    @Test
    void invalidCreateDoesNotSaveAnyRows() throws Exception {
        HttpResponse<String> response = post("""
                {"name":"Invalid","operatingDates":["2026-11-03","2026-11-03"],
                 "startTime":"18:00:00","endTime":"10:00:00"}
                """);

        assertEquals(400, response.statusCode());
        assertEquals(2, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM festivals", Integer.class));
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/festivals"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
