package com.boothfit.festival;

import java.net.URI;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"local", "test"})
@RequestMapping("/api/festivals")
@Tag(name = "행사", description = "행사 정보")
class FestivalWriteController {

    private final FestivalService service;

    FestivalWriteController(FestivalService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "행사 생성 (로컬 전용)", description = "local·test 프로필에서만 사용할 수 있습니다. 이름은 공백 제외 200자 이하, 운영 날짜는 중복 없는 1개 이상이며 시작 시간은 종료 시간보다 빨라야 합니다.")
    @ApiResponse(responseCode = "201", description = "생성된 행사 상세와 Location 헤더",
            content = @Content(schema = @Schema(implementation = FestivalDetail.class)))
    @ApiResponse(responseCode = "400", description = "요청 값이 유효하지 않음")
    ResponseEntity<FestivalDetail> create(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateFestivalRequest.class),
                            examples = @ExampleObject(
                                    name = "행사 생성 예시",
                                    value = """
                                            {"name":"시연용 캠퍼스 학술제","operatingDates":["2026-10-16","2026-10-17"],"startTime":"10:00:00","endTime":"18:00:00"}
                                            """)))
            CreateFestivalRequest request) {
        FestivalDetail festival = service.create(request);
        return ResponseEntity.created(URI.create("/api/festivals/" + festival.id())).body(festival);
    }
}
