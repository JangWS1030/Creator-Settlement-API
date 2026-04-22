# Creator Settlement API

크리에이터 강의 판매/취소 데이터를 기준으로 월별 정산 예정 금액과 운영자 집계를 조회하는 Spring Boot API 프로젝트다.

## 기술 스택
- Java 21
- Spring Boot 3.5
- Spring Web / Spring Data JPA / Validation
- MySQL 8.4
- H2 test profile
- Gradle Wrapper
- Docker Compose

## 주요 기능
- 판매 등록: `POST /api/sales`
- 취소 등록: `POST /api/sales/{saleId}/cancellations`
- 강사별 판매 내역 조회: `GET /api/sales`
- 월별 정산 조회: `GET /api/settlements/monthly`
- 운영자 기간 집계 조회: `GET /api/admin/settlements`
- 정산 생성/확정/지급: `POST /api/settlements`, `PATCH /api/settlements/{id}/confirm`, `PATCH /api/settlements/{id}/pay`
- 운영자 CSV 다운로드: `GET /api/admin/settlements.csv`
- 수수료율 이력 관리: `GET /api/admin/fee-rates`, `POST /api/admin/fee-rates`
- 제출용 정적 콘솔 UI: `/`

## 실행

### 1. Docker Compose로 전체 실행
```bash
docker compose up -d --build
```

- API: `http://localhost:8080`
- MySQL: `127.0.0.1:3306`

### 2. 로컬 앱 + Docker MySQL
```bash
docker compose up -d mysql
./gradlew bootRun
```

Windows:
```powershell
.\gradlew.bat bootRun
```

## 테스트
```bash
./gradlew test
```

Windows:
```powershell
.\gradlew.bat test
```

## 샘플 데이터
앱 시작 시 `APP_SEED_ENABLED=true`이면 샘플 데이터가 자동 반영된다.

- creators: `6`
- courses: `9`
- sales: `19`
- cancellations: `11`
- fee rate histories: `1` (`2020-01-01`, `20%`)

### 강사별 데이터
| 강사 ID | 강사명 | 강의 | 주요 데이터 |
|---|---|---|---|
| `creator-1` | `김강사` | `Spring Boot 입문`, `JPA 실전` | 2025-03 기본 정산, 전액 취소, 부분 취소, 2025-04 다중 취소, 2025-05 월경계 취소 |
| `creator-2` | `이강사` | `Kotlin 기초` | 2025-01/02 판매-취소 월 분리, 2025-11 HALF_UP 반올림, 2026-12 미래 월 조회 |
| `creator-3` | `박강사` | `MSA 설계` | 2025-03 빈 월, 2025-04 결제와 같은 시각 취소 |
| `creator-4` | `최강사` | `Querydsl 튜닝`, `Docker 배포 자동화` | 2025-06 추가 강사 정산, 부분 취소 |
| `creator-5` | `정강사` | `Redis 실전` | 2025-07 판매, 2025-08 월경계 취소 |
| `creator-6` | `한강사` | `관측성 입문`, `배치 처리 워크숍` | 2025-08 부분 취소, 2025-10 부분 취소, 2025-12 추가 판매 |

기존 MySQL 볼륨을 재사용하면 수동으로 넣은 데이터가 남을 수 있다. 빈 볼륨 기준 canonical count는 위 표 기준이다.

## 데이터 넣는 쿼리는 어디서 생성되나

### 1. 샘플 데이터 insert/update
- 시작점: `SampleDataInitializer`
- 호출 메서드: `SampleDataService.seedDefaultDataIfEmpty()`
- 실제 SQL 생성: JPA Repository `save(...)` + 영속성 컨텍스트 dirty checking
- 즉, `insert` / `update` SQL은 Hibernate가 생성한다.

관련 파일:
- `src/main/java/io/github/jangws1030/creatorsettlementapi/sample/SampleDataInitializer.java`
- `src/main/java/io/github/jangws1030/creatorsettlementapi/sample/SampleDataService.java`
- `src/main/resources/application.properties`

SQL 로그는 `application.properties`의 아래 설정으로 볼 수 있다.

```properties
logging.level.org.hibernate.SQL=info
```

## Scenario Playbook

UI scenario runner:
- open `/`
- use `TC Scenario Playbook` card
- click `Run` to create or reuse scenario data and fetch result
- click `SQL` to see DB verification snippet for that TC

DBClient query file:
- [DB_SCENARIO_CHECK_QUERIES.sql](DB_SCENARIO_CHECK_QUERIES.sql)

### Added TC summary
| Scenario ID | Name | Purpose | Expected result |
|---|---|---|---|
| `TC-01` | 누적 부분 환불 | 환불 합계와 잔액 계산 검증 | 총 환불액이 원금 이내이고 순매출/잔액이 정확히 반영된다. |
| `TC-02` | 당월 순매출 음수 | 환불만 존재하는 월 처리 검증 | 현재 구현은 `0원` 클램프가 아니라 음수 정산값을 그대로 반환한다. |
| `TC-03` | 지급 완료 후 취소 | 기지급 정산과 차기 차감 관계 검증 | 지급 완료 스냅샷은 유지되고, 이후 취소는 차기 월 정산에 반영된다. |
| `TC-04` | 수수료율 변경 소급 | 환불 수수료 기준 검증 | 환불 수수료는 환불 월이 아니라 원판매 시점 수수료율을 기준으로 계산한다. |
| `TC-05` | 밀리초/초 단위 경계 | KST 월 경계 처리 검증 | `23:59:59.999` 와 `00:00:00.000` 이 서로 다른 월로 정확히 분리된다. |

### UI-created fixed ids
The UI scenario runner uses fixed ids below so rerun is idempotent enough for demo and DB verification.

- `sale-tc01-ui`, `cancel-tc01-ui-1`, `cancel-tc01-ui-2`
- `sale-tc02-ui`, `cancel-tc02-ui-1`
- `sale-tc03-ui`, `cancel-tc03-ui-1`
- `sale-tc04-ui`, `cancel-tc04-ui-1`
- `sale-tc05-ui-1`, `sale-tc05-ui-2`

### 2. view / function DDL
- 시작점: `DatabaseObjectInitializer`
- 실행 조건: DB product name이 MySQL일 때만
- 실제 SQL 실행: `JdbcTemplate.execute(...)`

관련 파일:
- `src/main/java/io/github/jangws1030/creatorsettlementapi/db/DatabaseObjectInitializer.java`

## DB views / functions

### Functions
- `fn_net_sale_amount(total_sale_amount, refund_amount)`
  - 순매출 계산
- `fn_platform_fee_amount(net_sale_amount, fee_rate_percentage)`
  - 플랫폼 수수료 계산
- `fn_scheduled_settlement_amount(total_sale_amount, refund_amount, fee_rate_percentage)`
  - 정산 예정 금액 계산

### Views
- `vw_sale_audit`
  - 판매 1건 기준 감사용 뷰
  - 강사/강의/환불 누계/잔액/상태를 한 번에 조회
  - 저장된 UTC 시각을 KST 기준으로 변환해서 노출
- `vw_creator_monthly_settlement_base`
  - 강사별 월별 기초 집계 뷰
  - 판매 월과 취소 월을 분리해서 월별 총액/환불/건수를 계산
  - 월경계 계산도 KST 기준으로 맞춘다

### 로컬 MySQL 설정
MySQL 함수 생성에는 `log_bin_trust_function_creators=1`이 필요하다. `docker-compose.yml`에 아래 옵션을 넣어 두었다.

```yaml
--log-bin-trust-function-creators=1
```

## 추가한 시나리오
- `creator-4 / 2025-06`: 신규 강사 2개 강의 판매 + 부분 취소
- `creator-5 / 2025-07~08`: 7월 판매가 8월 1일 00:00:00 취소로 넘어가는 월경계 검증
- `creator-6 / 2025-08`: 관측성 강의 부분 취소 검증
- `creator-6 / 2025-10`: 배치 강의 부분 취소 검증
- `TC-01`: 누적 부분 환불 합계와 잔액 검증
- `TC-02`: 환불만 존재하는 월의 음수 정산 검증
- `TC-03`: 지급 완료 후 취소가 차기 월에만 반영되는지 검증
- `TC-04`: 환불 수수료를 판매 시점 수수료율 기준으로 계산하는지 검증
- `TC-05`: KST 기준 `00:00:00.000` 월경계 분리 검증
- 강사/강의 샘플 데이터가 영어 placeholder에서 실제 제출용 한국어 데이터로 정리됨
- 기존 시나리오 유지:
  - 기본 월정산
  - 전액 취소
  - 부분 취소
  - 빈 월 조회
  - 결제와 같은 시각 취소
  - 미래 월 조회
  - HALF_UP 수수료 반올림

## 정산 계산 메모
- 판매 수수료: 판매 발생일의 수수료율 적용
- 환불 수수료 차감: 환불 월 수수료율이 아니라 원판매 발생일의 수수료율 적용
- 환불만 있는 월은 현재 구현에서 `0원 클램프`가 아니라 음수 정산값을 그대로 반환한다.

## 문서 안내
- 요구사항: [REQUIREMENTS.md](REQUIREMENTS.md)
- DB 설계: [DATABASE_DESIGN.md](DATABASE_DESIGN.md)
- 제출 요약: [SUBMISSION.md](SUBMISSION.md)
- 문서 인덱스: [HELP.md](HELP.md)
