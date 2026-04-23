# Creator Settlement API

## 프로젝트 개요
크리에이터별 강의 판매/취소 데이터를 바탕으로 월별 정산 예정 금액을 계산하고, 운영자 기간 집계를 조회하는 Spring Boot 백엔드 과제 프로젝트다.

제출 편의를 위해 정적 콘솔 UI도 함께 제공한다.

- API 진입점: `/api`
- 제출용 UI: `/`
- 기본 실행 포트: `8080`

### 주요 기능
- 크리에이터/강의 카탈로그 조회
- 판매 등록
- 판매 취소 등록
- 강사별 판매 내역 조회
- 월별 정산 조회
- 정산 생성 / 확정 / 지급 처리
- 운영자 기간 집계 조회
- 운영자 CSV 다운로드
- 수수료율 이력 조회 / 등록
- UI 기반 테스트 시나리오 실행

### 샘플 데이터
앱 시작 시 `APP_SEED_ENABLED=true`면 샘플 데이터가 자동 반영된다.

- creators: `6`
- courses: `9`
- sales: `19`
- cancellations: `11`
- fee rate histories: `1` (`2020-01-01`, `20%`)

### 강사별 샘플 구성
| 강사 ID | 강사명 | 강의 | 주요 시나리오 |
|---|---|---|---|
| `creator-1` | `김강사` | `Spring Boot 입문`, `JPA 실전` | 기본 월정산, 전액 취소, 부분 취소, 다중 취소 |
| `creator-2` | `이강사` | `Kotlin 기초` | 판매월/취소월 분리, HALF_UP 반올림, 미래 월 조회 |
| `creator-3` | `박강사` | `MSA 설계` | 빈 월 조회, 결제와 같은 시각 취소 |
| `creator-4` | `최강사` | `Querydsl 튜닝`, `Docker 배포 자동화` | 추가 강사 정산, 지급 완료 후 취소, 월 경계 검증 |
| `creator-5` | `정강사` | `Redis 실전` | 수수료율 변경 소급, 월 경계 취소 |
| `creator-6` | `한강사` | `관측성 입문`, `배치 처리 워크숍` | 누적 부분 환불, 음수 정산, 부분 취소 |

기존 MySQL 볼륨을 재사용하면 수동 입력 데이터가 남아 있을 수 있다. 위 수치는 빈 볼륨 기준 canonical count다.

## 기술 스택
- Java 21
- Spring Boot 3.5.0
- Spring Web
- Spring Data JPA
- Spring Validation
- MySQL 8.4
- H2 (test profile)
- Gradle Wrapper
- Docker Compose
- 정적 HTML/CSS/Vanilla JavaScript UI

## 실행 방법
### 1. Docker Compose로 전체 실행
```bash
docker compose up -d --build
```

- API/UI: `http://localhost:8080`
- MySQL: `127.0.0.1:3306`

### 2. MySQL만 Docker로 띄우고 앱은 로컬 실행
```bash
docker compose up -d mysql
./gradlew bootRun
```

Windows:
```powershell
.\gradlew.bat bootRun
```

### 3. 포트 변경이 필요할 때
```powershell
$env:SERVER_PORT=8082
.\gradlew.bat bootRun
```

### 4. 제출용 UI 사용 흐름
1. `/` 접속
2. 상단에서 강사 선택
3. 선택한 강사 기준으로 판매 등록 / 취소 등록 / 월별 정산 / 판매 조회 진행
4. 하단 `테스트 시나리오` 카드에서 `실행` 또는 `SQL` 버튼 사용

## 요구사항 해석 및 가정
- 정산 기준 키는 `creatorId + yearMonth` 조합으로 해석했다.
- 판매 금액은 `paidAt`이 속한 월에 집계한다.
- 환불 금액은 `cancelledAt`이 속한 월에 집계한다.
- 월별 정산 조회는 KST 기준 월 경계를 따른다.
- MySQL reporting view에서는 UTC 저장 시각을 KST로 변환해서 월 집계를 맞춘다.
- 환불 수수료는 환불 시점 수수료율이 아니라 원판매 시점 수수료율을 사용한다.
- 지급 완료된 정산 스냅샷은 유지하고, 이후 취소는 취소가 발생한 월 정산에 반영한다.
- 환불만 존재하는 월은 현재 구현에서 `0원 보정`이나 `차기 이월` 정책을 두지 않고 음수 정산값을 그대로 반환한다.
- 샘플 시나리오 재실행을 위해 UI 테스트 데이터는 고정 ID를 사용한다.

## 설계 결정과 이유
### 1. 계층 분리
`api / application / domain / repository` 구조로 나눴다.

- API: 요청/응답 DTO와 엔드포인트
- Application: 명령/조회 로직
- Domain: 엔티티와 상태 모델
- Repository: JPA 접근 계층

이유:
- 과제 범위에서 구조가 과하지 않으면서도 책임이 분명하다.
- 정산 계산 로직과 웹 계층을 분리해 테스트하기 쉽다.

### 2. 정산 스냅샷과 실시간 조회 분리
`GET /api/settlements/monthly`는 월 기준 실시간 계산 결과를 반환하고, `POST /api/settlements`는 해당 시점 스냅샷을 `settlements` 테이블에 저장한다.

이유:
- 조회와 지급 상태 관리를 동시에 만족하려면 스냅샷이 필요하다.
- 지급 이후 취소 같은 시나리오를 설명하기 쉽다.

### 3. 수수료율 이력 테이블 도입
`fee_rate_histories`를 별도 테이블로 두고 `effective_from` 기준으로 수수료율을 관리한다.

이유:
- 단일 상수보다 요구사항 확장에 유리하다.
- TC-04 같은 소급 계산 검증이 가능하다.

### 4. DB reporting view / function 제공
MySQL에서 아래 DB 객체를 자동 생성한다.

- functions
  - `fn_net_sale_amount`
  - `fn_platform_fee_amount`
  - `fn_scheduled_settlement_amount`
- views
  - `vw_sale_audit`
  - `vw_creator_monthly_settlement_base`

이유:
- DBClient에서 검증하기 쉽다.
- 제출 시 “정산 계산 근거”를 SQL 레벨에서도 보여줄 수 있다.

### 5. 시드 데이터와 DDL 생성 방식 분리
- 샘플 데이터 insert/update
  - 시작점: `SampleDataInitializer`
  - 메서드: `SampleDataService.seedDefaultDataIfEmpty()`
  - 실제 SQL 생성 주체: Hibernate (`save(...)`, dirty checking)
- view/function DDL
  - 시작점: `DatabaseObjectInitializer`
  - 실제 실행 주체: `JdbcTemplate.execute(...)`

이유:
- 일반 엔티티 데이터는 JPA 흐름을 재사용하고
- DB 종속 객체(view/function)는 명시적 SQL로 관리하는 편이 명확하다.

## 미구현 / 제약사항
- 인증 / 권한 처리는 없다.
- 페이지네이션, 정렬, 고급 검색은 없다.
- 판매/취소 수정 및 삭제 API는 제공하지 않는다.
- 통화는 원화(long) 기준으로만 처리한다.
- 정산 음수 금액에 대한 별도 이월 정책은 구현하지 않았다.
- Flyway/Liquibase 같은 마이그레이션 도구는 쓰지 않고, JPA + initializer 방식으로 구성했다.
- 제출용 UI는 운영용 제품 화면이 아니라 검증/시연용 콘솔이다.

## AI 활용 범위
- README 구조 재정리와 문서 초안 작성 보조
- 제출용 UI 흐름 개선과 문구 정리 보조
- 테스트 시나리오 문장 정리와 반복 코드 리팩터링 보조
- 최종 반영 내용은 로컬 테스트와 브라우저 확인으로 검증했다.

## API 목록 및 예시
### API 목록
| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/creators` | 크리에이터 목록 조회 |
| `GET` | `/api/courses` | 전체 강의 조회 |
| `GET` | `/api/courses?creatorId=creator-1` | 특정 강사 강의 조회 |
| `POST` | `/api/sales` | 판매 등록 |
| `POST` | `/api/sales/{saleId}/cancellations` | 판매 취소 등록 |
| `GET` | `/api/sales?creatorId=creator-1&fromDate=2025-03-01&toDate=2025-03-31` | 강사별 판매 내역 조회 |
| `GET` | `/api/settlements/monthly?creatorId=creator-1&yearMonth=2025-03` | 월별 정산 조회 |
| `POST` | `/api/settlements` | 월 정산 스냅샷 생성 |
| `GET` | `/api/settlements/{settlementId}` | 정산 상세 조회 |
| `PATCH` | `/api/settlements/{settlementId}/confirm` | 정산 확정 |
| `PATCH` | `/api/settlements/{settlementId}/pay` | 정산 지급 완료 |
| `GET` | `/api/admin/settlements?startDate=2025-03-01&endDate=2025-03-31` | 운영자 기간 집계 |
| `GET` | `/api/admin/settlements.csv?startDate=2025-03-01&endDate=2025-03-31` | 운영자 집계 CSV 다운로드 |
| `GET` | `/api/admin/fee-rates` | 수수료율 이력 조회 |
| `POST` | `/api/admin/fee-rates` | 수수료율 이력 등록 |

### 예시 1. 판매 등록
```http
POST /api/sales
Content-Type: application/json

{
  "id": "sale-demo-1",
  "courseId": "course-1",
  "studentId": "student-demo-1",
  "amount": 70000,
  "paidAt": "2025-03-10T10:00:00+09:00"
}
```

```json
{
  "id": "sale-demo-1",
  "creatorId": "creator-1",
  "creatorName": "김강사",
  "courseId": "course-1",
  "courseTitle": "Spring Boot 입문",
  "studentId": "student-demo-1",
  "amount": 70000,
  "paidAt": "2025-03-10T10:00:00+09:00"
}
```

### 예시 2. 취소 등록
```http
POST /api/sales/sale-demo-1/cancellations
Content-Type: application/json

{
  "id": "cancel-demo-1",
  "refundAmount": 10000,
  "cancelledAt": "2025-03-12T09:00:00+09:00"
}
```

```json
{
  "id": "cancel-demo-1",
  "saleId": "sale-demo-1",
  "refundAmount": 10000,
  "cancelledAt": "2025-03-12T09:00:00+09:00",
  "accumulatedRefundAmount": 10000,
  "remainingSaleAmount": 60000
}
```

### 예시 3. 월별 정산 조회
```http
GET /api/settlements/monthly?creatorId=creator-1&yearMonth=2025-03
```

응답 필드 구조 예시:

```json
{
  "creatorId": "creator-1",
  "creatorName": "김강사",
  "yearMonth": "2025-03",
  "totalSaleAmount": 330000,
  "refundAmount": 30000,
  "netSaleAmount": 300000,
  "platformFeeAmount": 60000,
  "scheduledSettlementAmount": 240000,
  "saleCount": 4,
  "cancellationCount": 1,
  "feeRatePercentage": 20,
  "settlementId": "settlement-creator-1-2025-03",
  "settlementStatus": "PENDING"
}
```

### 예시 4. 운영자 기간 집계 조회
```http
GET /api/admin/settlements?startDate=2025-03-01&endDate=2025-03-31
```

응답 필드 구조 예시:

```json
{
  "startDate": "2025-03-01",
  "endDate": "2025-03-31",
  "totalSaleAmount": 579000,
  "totalRefundAmount": 39000,
  "totalNetSaleAmount": 540000,
  "totalPlatformFeeAmount": 108000,
  "totalScheduledSettlementAmount": 432000,
  "creatorSettlements": [
    {
      "creatorId": "creator-1",
      "creatorName": "김강사",
      "totalSaleAmount": 330000,
      "refundAmount": 30000,
      "netSaleAmount": 300000,
      "platformFeeAmount": 60000,
      "scheduledSettlementAmount": 240000,
      "saleCount": 4,
      "cancellationCount": 1,
      "feeRatePercentage": 20
    }
  ]
}
```

### 예시 5. 수수료율 등록
```http
POST /api/admin/fee-rates
Content-Type: application/json

{
  "effectiveFrom": "2025-04-01",
  "feeRatePercentage": 30
}
```

## 데이터 모델 설명
### 핵심 테이블
- `creators`
  - 크리에이터 마스터
  - 컬럼: `id`, `name`
- `courses`
  - 강의 마스터
  - 컬럼: `id`, `creator_id`, `title`
- `sale_records`
  - 판매 이력
  - 컬럼: `id`, `course_id`, `student_id`, `amount`, `paid_at`
- `cancellation_records`
  - 판매 취소 이력
  - 컬럼: `id`, `sale_record_id`, `refund_amount`, `cancelled_at`
- `fee_rate_histories`
  - 수수료율 이력
  - 컬럼: `id`, `effective_from`, `fee_rate_percentage`, `created_at`
- `settlements`
  - 정산 스냅샷
  - 컬럼: `id`, `creator_id`, `settlement_month`, `period_start_date`, `period_end_date`, `total_sale_amount`, `refund_amount`, `net_sale_amount`, `platform_fee_amount`, `scheduled_settlement_amount`, `sale_count`, `cancellation_count`, `fee_rate_percentage`, `status`, `created_at`, `confirmed_at`, `paid_at`

### 관계
- `Creator 1 : N Course`
- `Course 1 : N SaleRecord`
- `SaleRecord 1 : N CancellationRecord`
- `Creator 1 : N Settlement`

### 정산 계산 보조 DB 객체
#### Functions
- `fn_net_sale_amount(total_sale_amount, refund_amount)`
  - 순매출 계산
- `fn_platform_fee_amount(net_sale_amount, fee_rate_percentage)`
  - 플랫폼 수수료 계산
- `fn_scheduled_settlement_amount(total_sale_amount, refund_amount, fee_rate_percentage)`
  - 정산 예정 금액 계산

#### Views
- `vw_sale_audit`
  - 판매 1건 기준 감사용 뷰
  - 강사/강의/환불 누계/잔액/상태 확인용
- `vw_creator_monthly_settlement_base`
  - 강사별 월 기초 집계 뷰
  - 판매 월 / 환불 월 분리 확인용

### DB 함수 생성 관련 설정
MySQL 함수 생성을 위해 `docker-compose.yml`에 아래 옵션을 넣어 두었다.

```yaml
--log-bin-trust-function-creators=1
```

### 시드 / DDL 생성 위치
- 샘플 데이터
  - `src/main/java/io/github/jangws1030/creatorsettlementapi/sample/SampleDataInitializer.java`
  - `src/main/java/io/github/jangws1030/creatorsettlementapi/sample/SampleDataService.java`
- view / function DDL
  - `src/main/java/io/github/jangws1030/creatorsettlementapi/db/DatabaseObjectInitializer.java`

SQL 로그는 아래 설정으로 확인할 수 있다.

```properties
logging.level.org.hibernate.SQL=info
```

## 테스트 실행 방법
### 1. 전체 테스트 실행
```bash
./gradlew test
```

Windows:
```powershell
.\gradlew.bat test
```

### 2. 포함된 테스트 클래스
- `CreatorSettlementApiApplicationTests`
- `CatalogApiTest`
- `SaleApiTest`
- `AdminSettlementApiTest`
- `MonthlySettlementApiTest`
- `SettlementLifecycleApiTest`
- `SettlementScenarioApiTest`

### 3. 추가 테스트 시나리오
| 시나리오 ID | 시나리오 명 | 목적 | 기대 결과 |
|---|---|---|---|
| `TC-01` | 누적 부분 환불 | 환불 합계 논리 검증 | 총 환불액이 원금 이내이며 순매출이 정확히 반영된다. |
| `TC-02` | 당월 순매출 음수 | 환불 과다 발생 케이스 처리 | 현재 구현은 `0원 처리` 대신 음수 정산값을 그대로 반환한다. |
| `TC-03` | 지급 완료 후 취소 | 정산 상태와 취소의 상관관계 검증 | 기지급 스냅샷은 유지되고 차기 월 정산에서 차감이 반영된다. |
| `TC-04` | 수수료율 변경 소급 | 수수료 이력 관리 검증 | 환불분 수수료 계산은 판매 시점 수수료율을 기준으로 한다. |
| `TC-05` | 밀리초/초 단위 경계 | KST 처리 정확성 검증 | `23:59:59.999` 와 `00:00:00.000` 이 다른 월로 분리된다. |

### 4. UI / SQL 검증 방법
- UI 검증: `/` 접속 후 하단 `테스트 시나리오` 카드에서 `실행`
- SQL 검증: [DB_SCENARIO_CHECK_QUERIES.sql](DB_SCENARIO_CHECK_QUERIES.sql) 실행

### 5. 참고 문서
- 요구사항: [REQUIREMENTS.md](REQUIREMENTS.md)
- DB 설계: [DATABASE_DESIGN.md](DATABASE_DESIGN.md)
- 제출 요약: [SUBMISSION.md](SUBMISSION.md)
- 문서 인덱스: [HELP.md](HELP.md)
