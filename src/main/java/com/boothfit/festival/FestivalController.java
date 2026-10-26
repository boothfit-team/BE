package com.boothfit.festival;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/festivals")
@Tag(name = "행사", description = "방문자가 조회하는 행사 정보")
class FestivalController {

    private final FestivalService service;

    FestivalController(FestivalService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "행사 목록 조회", description = "모든 행사와 각 행사의 운영 날짜를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "행사 목록. 등록된 행사가 없으면 빈 배열",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = FestivalSummary.class))))
    List<FestivalSummary> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "행사 상세 조회", description = "행사의 운영 날짜와 공통 시작·종료 시간을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "행사 상세",
            content = @Content(schema = @Schema(implementation = FestivalDetail.class)))
    @ApiResponse(responseCode = "400", description = "행사 ID가 숫자가 아님")
    @ApiResponse(responseCode = "404", description = "해당 행사가 없음")
    FestivalDetail findById(@Parameter(description = "행사 ID", example = "1") @PathVariable long id) {
        return service.findById(id);
    }
}
