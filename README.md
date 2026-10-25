# BoothFit BE

Java 21과 Spring Boot로 구성한 부스핏 백엔드입니다. 현재는 CI 검증을 위한 최소 앱이며 비즈니스 API는 아직 포함하지 않습니다.

## 로컬 실행

Java 21을 설치한 뒤 Windows에서는 다음 명령을 실행합니다.

```powershell
.\gradlew.bat test bootJar
.\gradlew.bat bootRun
```

macOS/Linux에서는 `./gradlew test bootJar`, `./gradlew bootRun`을 사용합니다. 앱 실행 후 `http://localhost:8080/actuator/health`에서 상태를 확인할 수 있습니다.

## 협업과 CI

작업 전 [협업 규칙](CONTRIBUTING.md)을 확인해 주세요. BE 이슈를 만들고 `dev`에서 작업 브랜치를 분기해 `dev`로 PR을 엽니다. 배포할 변경은 `dev`에서 `main`으로 PR을 엽니다.

GitHub Actions는 `dev`와 `main` 대상 PR 및 해당 브랜치의 푸시에서 Java 21로 `test`와 `bootJar`를 실행합니다. PR에는 브랜치 번호와 같은 `Closes #<이슈번호>`를 적어야 합니다.
