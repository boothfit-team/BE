# BE 배포 아키텍처와 운영 절차

## 현재 구성

```text
main 푸시
  → GitHub Actions: Java 21 테스트와 bootJar
  → DEPLOY_ENABLED=true일 때만 배포 단계 진행
  → GitHub OIDC로 임시 AWS 권한 획득
  → Docker 이미지 빌드, Amazon ECR에 커밋 SHA 태그로 저장
  → AWS Systems Manager Run Command로 EC2 배포
  → EC2가 ECR에서 이미지를 받아 Docker 컨테이너 교체
  → 같은 EC2의 PostgreSQL 컨테이너에 연결하고 Flyway 마이그레이션 실행
  → /actuator/health 확인, 실패하면 직전 이미지 복구
```

| 영역 | 선택한 방식 |
| --- | --- |
| 서버 | 단일 EC2, Amazon Linux 2023, 기본 `t3.small` |
| 앱 | Java 21 Spring Boot JAR를 Docker 컨테이너로 실행 |
| 이미지 저장소 | Amazon ECR, 커밋 SHA 태그, 최근 20개 보관 |
| 인프라 정의 | Terraform, S3 원격 state 및 잠금 파일 |
| 배포 | `main` 검증 성공 후 `DEPLOY_ENABLED=true`일 때 ECR → SSM → EC2 |
| AWS 인증 | GitHub OIDC 단기 자격 증명, EC2 인스턴스 역할 |
| 서버 접속 | Systems Manager Session Manager, 인바운드 SSH 포트 없음 |
| 상태 확인 | `http://<Elastic IP>/actuator/health` |
| 데이터베이스 | 같은 EC2의 PostgreSQL 17 컨테이너, Docker named volume에 영속 저장 |
| 스키마 변경 | 앱 시작 시 Flyway SQL 마이그레이션 적용 |

팀이 Java 21, Spring Boot 4, Gradle, PostgreSQL, Flyway, EC2, Docker를 승인했습니다. 현재는 단일 EC2에 앱과 DB 컨테이너를 함께 두어 초기 구성을 단순화했습니다. DB 포트는 외부에 열지 않고 두 컨테이너만 `boothfit-net` 네트워크로 통신합니다. 예약·부스 테이블의 실제 SQL은 데이터 모델이 정해진 뒤 `src/main/resources/db/migration/V1__...sql`부터 추가합니다. 지금은 Flyway 경로만 준비되어 있습니다.

같은 EC2의 Docker volume은 컨테이너 교체 후에도 남지만, 인스턴스나 디스크 장애에 대비한 자동 백업은 아직 없습니다. 실제 예약 데이터를 넣기 전 백업·복구를 구성하거나 관리형 RDS PostgreSQL로 전환해야 합니다.

현재 보안 그룹은 HTTP 80만 엽니다. **HTTPS와 FE 연동에는 API 도메인 및 TLS 구성이 추가로 필요합니다.** Vercel의 HTTPS 페이지에서 HTTP API를 호출하는 운영 구성으로 사용하지 마세요.

## 1. AWS 사전 준비

- 사용할 AWS 계정과 `ap-northeast-2` 리전의 기본 VPC 또는 기존 VPC의 **인터넷에 연결된 퍼블릭 서브넷**을 준비합니다. Terraform은 VPC와 서브넷 자체는 생성하지 않습니다.
- Terraform state용 S3 버킷을 먼저 만듭니다. 버킷명은 AWS 전체에서 고유해야 하며, 퍼블릭 접근을 차단하고 버전 관리와 서버 측 암호화를 켭니다. state 파일과 `backend.hcl`은 Git에 올리지 않습니다.
- AWS CLI 인증이 유효해야 합니다. `aws sts get-caller-identity`로 대상 계정을 확인합니다.
- 같은 AWS 계정에 `token.actions.githubusercontent.com` OIDC 공급자가 이미 있으면 그 ARN을 `github_oidc_provider_arn`에 설정합니다. 그렇지 않으면 Terraform이 새로 생성합니다.

예를 들어 서울 리전에서 state 버킷을 처음 만든다면 아래의 `BUCKET_NAME`을 **AWS 전체에서 고유한 이름**으로 바꿔 실행합니다. 이미 버킷이 있다면 생성 명령은 생략합니다.

```bash
BUCKET_NAME="YOUR_UNIQUE_TERRAFORM_STATE_BUCKET"
aws s3api create-bucket --bucket "$BUCKET_NAME" --region ap-northeast-2 \
  --create-bucket-configuration LocationConstraint=ap-northeast-2
aws s3api put-public-access-block --bucket "$BUCKET_NAME" \
  --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
aws s3api put-bucket-versioning --bucket "$BUCKET_NAME" \
  --versioning-configuration Status=Enabled
aws s3api put-bucket-encryption --bucket "$BUCKET_NAME" \
  --server-side-encryption-configuration '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'
```

`infra/terraform/backend.hcl.example`을 `backend.hcl`로 복사하고 버킷명을 입력합니다. 필요한 경우 `terraform.tfvars.example`도 `terraform.tfvars`로 복사해 VPC·서브넷·리전을 지정합니다. 실제 비밀 값은 `.tfvars`에도 넣지 않습니다.

```bash
cd infra/terraform
terraform init -backend-config=backend.hcl
terraform plan
terraform apply
terraform output
```

`apply`는 EC2, Elastic IP, ECR 및 IAM 리소스를 실제로 만들고 비용이 발생할 수 있습니다. 계획 내용을 확인한 뒤 실행합니다. 신규 EC2의 user data가 Docker를 설치하고 시작하며, `/etc/boothfit/app.env`를 빈 파일로 만듭니다. 첫 배포 전 Session Manager로 접속해 이 파일에 다음 값을 넣어야 합니다. `POSTGRES_PASSWORD`는 충분히 긴 무작위 값으로 정하고 외부에 공유하지 않습니다.

```text
SPRING_PROFILES_ACTIVE=prod
DB_HOST=boothfit-db
POSTGRES_DB=boothfit
POSTGRES_USER=boothfit
POSTGRES_PASSWORD=<strong-random-password>
```

`sudoedit /etc/boothfit/app.env`로 편집하고 `sudo chmod 600 /etc/boothfit/app.env`를 유지합니다. 이미 생성된 DB volume에서 비밀번호만 바꾸면 DB 계정 비밀번호는 자동 변경되지 않으므로 별도의 비밀번호 변경 절차가 필요합니다. 첫 배포 전에는 앱이 실행되지 않으므로 health URL도 응답하지 않습니다.

GitHub OIDC 신뢰 정책은 `boothfit-team/BE`의 현재 조직·저장소 ID가 포함된 `main` 브랜치 subject를 사용합니다. 저장소의 OIDC subject 설정이 변경되었다면 `github_oidc_subject`를 실제 값으로 바꿔야 합니다.

## 2. GitHub Actions 변수 설정

BE 저장소 `Settings → Secrets and variables → Actions → Variables`에 Terraform 출력값을 다음 이름으로 등록합니다.

| GitHub 변수 | Terraform 출력 |
| --- | --- |
| `AWS_REGION` | 사용한 AWS 리전, 기본 `ap-northeast-2` |
| `AWS_DEPLOY_ROLE_ARN` | `github_deploy_role_arn` |
| `ECR_REPOSITORY_URL` | `ecr_repository_url` |
| `EC2_INSTANCE_ID` | `instance_id` |
| `DEPLOY_ENABLED` | 모든 배포 준비가 끝난 뒤에만 `true` |

`DEPLOY_ENABLED`가 없거나 `true`가 아니면 배포 작업은 건너뛰고 빌드 검증만 실행됩니다. 이 변수는 EC2 생성, `/etc/boothfit/app.env` 설정, 나머지 GitHub 변수 입력을 마친 뒤 켭니다. 나머지 값들은 식별자이며 AWS 장기 액세스 키가 아닙니다. 배포 워크플로는 `main`에서만 실행되며, 배포 전에 다시 테스트와 JAR 패키징을 완료합니다. `workflow_dispatch`로 `main`에서 재실행할 수 있습니다. 같은 커밋의 ECR 이미지가 이미 있으면 재사용합니다.

## 3. 배포 확인 및 장애 대응

`dev`에서 검증한 변경을 `main`으로 PR 병합하면 `.github/workflows/deploy.yml`이 실행됩니다. 성공 후 `terraform output health_url`의 응답에 `"status":"UP"`가 있는지 확인합니다.

컨테이너와 로그는 AWS Systems Manager Session Manager로 EC2에 접속해 확인합니다. SSH 키와 22번 포트는 사용하지 않습니다.

```bash
sudo docker ps
sudo docker logs --tail 100 boothfit-be
sudo docker logs --tail 100 boothfit-db
curl -fsS http://127.0.0.1/actuator/health
```

배포 스크립트 `deploy/deploy.sh`는 새 컨테이너의 health 확인에 실패하면 이전 이미지로 되돌리고 실패 상태를 GitHub Actions에 전달합니다. 첫 배포에서 실패하거나 복구도 실패하면 SSM 명령 출력과 `docker logs`를 확인합니다. 수동 재배포는 `main`의 배포 워크플로를 다시 실행합니다.

앱과 PostgreSQL의 환경 변수는 EC2의 `/etc/boothfit/app.env`에서 읽습니다. 이 파일은 root만 읽을 수 있고 저장소에는 포함되지 않습니다. 비밀번호를 수동 설정하는 초기 방식이며, 자동 배포 환경에서의 비밀 값 회전은 추후 Parameter Store 또는 Secrets Manager 연동이 필요합니다.

## 4. 아직 필요한 결정

- FE와 연결할 API 도메인과 HTTPS 종료 방식
- PostgreSQL 백업·복구 정책과 향후 RDS 전환 여부
- 팀의 AWS 계정 및 S3 state 버킷 실제 값

이 값이 준비되기 전까지는 코드와 배포 자동화만 검증할 수 있으며 실제 EC2 배포 성공은 확인할 수 없습니다.

## 5. 로컬 PostgreSQL과 Flyway

로컬에서는 Docker로 DB만 실행하고 Spring Boot는 Gradle에서 실행합니다.

```bash
docker compose up -d postgres
./gradlew bootRun --args='--spring.profiles.active=local'
```

Windows에서는 `./gradlew` 대신 `gradlew.bat`을 사용합니다. 로컬 기본 비밀번호는 `boothfit-local`이며 `POSTGRES_PASSWORD` 환경 변수로 바꿀 수 있습니다. DB는 로컬 PC의 `127.0.0.1:5432`에서만 접근됩니다. `docker compose down`은 컨테이너만 내리고 `postgres_data` volume은 유지합니다.

프로덕션은 `application-prod.yml`이 환경 변수에서 DB 주소와 계정을 읽습니다. Spring Boot 시작 시 Flyway가 `db/migration`의 버전별 SQL을 적용합니다. 도메인 테이블용 첫 마이그레이션은 예약·부스 데이터 모델 확정 후 추가합니다. JPA와 Redis는 이번 승인 스택에 포함되지 않았으므로 추가하지 않았습니다.
