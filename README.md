# BE

부스핏 Spring Boot 백엔드 저장소입니다. 현재 애플리케이션 소스가 추가되기 전이므로 CI는 Gradle 실행을 건너뛰고, PR의 브랜치·이슈 연결 규칙을 검사합니다.

작업을 시작하기 전 [협업 규칙](CONTRIBUTING.md)을 확인해 주세요. 기능 작업은 BE 이슈를 만들고 `dev`에서 분기한 뒤 `dev`로 PR을 엽니다. 배포할 변경은 `dev`에서 `main`으로 PR을 엽니다.

Spring Boot 프로젝트를 추가할 때는 `gradlew`, `gradle/wrapper`와 `build.gradle` 또는 `build.gradle.kts`를 함께 반영해 주세요. 그러면 CI에서 Java 21로 `test`와 `bootJar`를 실행합니다.
