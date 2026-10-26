# 공개 행사 조회 API

인증 없이 행사 정보를 읽는 API입니다. 응답의 날짜는 `YYYY-MM-DD`, 시간은 `HH:mm:ss` 형식입니다. 운영 날짜는 오름차순으로 반환합니다.

실행 중인 서버의 Swagger UI는 `http://localhost:8080/swagger-ui.html`, OpenAPI JSON은 `http://localhost:8080/v3/api-docs`에서 확인할 수 있습니다. 로컬·테스트 프로필에서만 아래의 행사 생성 POST가 문서에 표시됩니다.

## 행사 목록

`GET /api/festivals`

성공 응답: `200 OK`

```json
[
  {
    "id": 1,
    "name": "2026 캠퍼스 학술제",
    "operatingDates": ["2026-10-16", "2026-10-17"]
  }
]
```

등록된 행사가 없으면 `200 OK`와 빈 배열 `[]`을 반환합니다.

## 행사 상세

`GET /api/festivals/{id}`

경로 변수 `id`는 행사 ID(양의 정수)입니다. 성공 응답: `200 OK`

```json
{
  "id": 1,
  "name": "2026 캠퍼스 학술제",
  "operatingDates": ["2026-10-16", "2026-10-17"],
  "startTime": "10:00:00",
  "endTime": "18:00:00"
}
```

해당 ID의 행사가 없으면 `404 Not Found`를 반환합니다. 숫자가 아닌 ID는 `400 Bad Request`입니다.

## 데이터 준비

Flyway의 `V1__create_festivals.sql`이 `festivals`와 `festival_operating_days` 테이블을 생성합니다. 마이그레이션은 시연 데이터를 자동 삽입하지 않습니다. 로컬에서는 아래의 POST 또는 SQL로 행사와 운영일을 넣어 조회할 수 있습니다.

아래는 로컬 시연 데이터 입력 예시입니다. 실제 행사 정보가 아닙니다.

```sql
WITH festival AS (
  INSERT INTO festivals (name, start_time, end_time)
  VALUES ('시연용 캠퍼스 학술제', '10:00', '18:00')
  RETURNING id
)
INSERT INTO festival_operating_days (festival_id, operating_date)
SELECT festival.id, days.operating_date
FROM festival
CROSS JOIN (VALUES (DATE '2026-10-16'), (DATE '2026-10-17')) AS days(operating_date);
```

입력 후 `GET http://localhost:8080/api/festivals`로 확인할 수 있습니다.

## 로컬 행사 생성

`POST /api/festivals`는 **`local`과 `test` 프로필에서만** 사용할 수 있습니다. 인증·관리자 권한이 아직 구현되지 않아 운영(`prod`) 프로필에는 이 경로를 열지 않습니다.

요청 예시:

```json
{
  "name": "시연용 캠퍼스 학술제",
  "operatingDates": ["2026-10-16", "2026-10-17"],
  "startTime": "10:00:00",
  "endTime": "18:00:00"
}
```

이름은 공백이 아닌 200자 이하 문자열, 운영일은 중복 없는 1개 이상의 날짜여야 합니다. 시작 시간은 종료 시간보다 빨라야 합니다. 성공하면 `201 Created`, `Location: /api/festivals/{id}`와 행사 상세 JSON을 반환합니다. 유효하지 않은 값은 `400 Bad Request`이며 저장하지 않습니다. 운영일은 날짜순으로 저장·반환합니다.
