# BoothFit BE

Java 21과 Spring Boot로 구성한 부스핏 백엔드입니다. 현재는 CI 검증을 위한 최소 앱이며 비즈니스 API는 아직 포함하지 않습니다.

## 현재 상태

Java 21·Spring Boot 4·Gradle과 로컬 PostgreSQL·Flyway 실행을 확인했습니다. EC2 배포 파일은 준비되어 있지만 AWS 리소스 생성과 실제 배포는 아직 진행하지 않았습니다. `main`의 빌드 검증은 실행되며, 배포 작업은 GitHub Actions 변수 `DEPLOY_ENABLED=true`를 설정한 뒤에만 실행됩니다.

## 패키지 구조

`src/main/java/com/boothfit` 아래를 기능별로 나눕니다. `auth`는 인증·권한, `festival`은 행사, `booth`는 부스, `reservation`은 예약·충돌, `queue`는 현장 대기, `review`는 후기, `global`은 공통 설정·예외 처리입니다. 현재는 각 패키지의 `package-info.java`만 있으며 API나 DB 모델은 정의하지 않았습니다.

기능 구현이 시작되면 해당 패키지에 필요한 Controller·Service·Repository·DTO를 추가합니다. DB 스키마 변경은 `src/main/resources/db/migration`의 Flyway SQL로 관리합니다.

## 로컬 실행

Java 21과 Docker를 준비한 뒤 Windows에서는 다음 명령을 실행합니다.

```powershell
.\gradlew.bat test bootJar
docker compose up -d postgres
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

macOS/Linux에서는 `./gradlew test bootJar`, `./gradlew bootRun --args='--spring.profiles.active=local'`을 사용합니다. 앱 실행 후 `http://localhost:8080/actuator/health`에서 상태를 확인할 수 있습니다. 테스트는 내장 H2를 사용하며, 로컬 실행과 운영은 PostgreSQL을 사용합니다. 로컬 기본 DB 계정과 Flyway 설정은 [배포 문서](docs/deployment.md)를 참고하세요.

IntelliJ에서 `BoothfitApplication`을 실행한다면 실행 구성의 환경 변수에 `SPRING_PROFILES_ACTIVE=local`을 넣고, 먼저 `docker compose up -d postgres`를 실행합니다. 기본 프로필에는 DB 접속 주소가 없어 앱을 시작할 수 없습니다.

## 공개 API

행사 목록 `GET /api/festivals`와 행사 상세 `GET /api/festivals/{id}`를 제공합니다. 로컬 검증용 행사 생성 `POST /api/festivals`는 `local`·`test` 프로필에서만 사용 가능합니다. 요청·응답 형식은 [API 명세](docs/api.md)를 참고하세요. 새 데이터베이스에는 행사 데이터가 자동으로 들어가지 않으므로, 등록 전 목록 응답은 빈 배열입니다.

## 협업과 CI

작업 전 [협업 규칙](CONTRIBUTING.md)을 확인해 주세요. BE 이슈를 만들고 `dev`에서 작업 브랜치를 분기해 `dev`로 PR을 엽니다. 배포할 변경은 `dev`에서 `main`으로 PR을 엽니다.

GitHub Actions는 `dev`와 `main` 대상 PR 및 해당 브랜치의 푸시에서 Java 21로 `test`와 `bootJar`를 실행합니다. PR에는 브랜치 번호와 같은 `Closes #<이슈번호>`를 적어야 합니다.

EC2 배포 구성과 운영 절차는 [배포 문서](docs/deployment.md)를 참고해 주세요.
