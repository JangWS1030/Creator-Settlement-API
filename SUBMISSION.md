# Creator Settlement API Submission Guide

## 제출 요약
- Spring Boot 3.5 / Java 21 / JPA / MySQL
- H2 기반 API 테스트 포함
- 제출용 정적 콘솔 UI 제공: `/`
- 샘플 데이터 자동 시드 포함
- MySQL view / function 자동 생성 포함

## 실행
```bash
docker compose up -d --build
```

로컬 앱만 실행할 때:
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

## 시드 데이터 요약
- creators: `6`
- courses: `9`
- sales: `19`
- cancellations: `11`

강사별 시드:
- `creator-1 김강사`: `Spring Boot 입문`, `JPA 실전`
- `creator-2 이강사`: `Kotlin 기초`
- `creator-3 박강사`: `MSA 설계`
- `creator-4 최강사`: `Querydsl 튜닝`, `Docker 배포 자동화`
- `creator-5 정강사`: `Redis 실전`
- `creator-6 한강사`: `관측성 입문`, `배치 처리 워크숍`

빈 MySQL 볼륨 기준 count는 위와 같다. 기존 볼륨을 재사용하면 수동 데이터가 남아 더 많아질 수 있다.

## DB views / functions

### Functions
- `fn_net_sale_amount`
- `fn_platform_fee_amount`
- `fn_scheduled_settlement_amount`

### Views
- `vw_sale_audit`
- `vw_creator_monthly_settlement_base`

용도:
- 함수는 정산 계산식을 DB 레벨에서 재사용
- `vw_sale_audit`는 판매 감사/운영 조회
- `vw_creator_monthly_settlement_base`는 강사별 월 집계 베이스

## 데이터 넣는 쿼리 생성 위치
- 샘플 판매/취소/강사/강의 데이터:
  - `SampleDataInitializer` -> `SampleDataService`
  - JPA `save(...)` / dirty checking으로 Hibernate SQL 생성
- view / function:
  - `DatabaseObjectInitializer`
  - `JdbcTemplate.execute(...)`로 DDL 직접 실행

## 검증 시나리오
- `creator-1 / 2025-03`: 기본 정산, 전액 취소, 부분 취소
- `creator-1 / 2025-04`: 다중 취소
- `creator-1 / 2025-05`: 월경계 직후 취소
- `creator-2 / 2025-01~02`: 판매/취소 월 분리
- `creator-2 / 2025-11`: HALF_UP 반올림
- `creator-2 / 2026-12`: 미래 월 조회
- `creator-3 / 2025-03`: 빈 월
- `creator-3 / 2025-04`: 결제와 같은 시각 취소
- `creator-4 / 2025-06`: 신규 강사 추가 시나리오
- `creator-5 / 2025-07~08`: 8월 1일 00:00:00 경계 취소
- `creator-6 / 2025-08`: 부분 취소
- `creator-6 / 2025-10`: 부분 취소

## 추가 시나리오 표
| 시나리오 ID | 시나리오 명 | 목적 | 기대 결과 |
|---|---|---|---|
| `TC-01` | 누적 부분 환불 | 환불 합계 논리 검증 | 총 환불액이 원금 이내이며 순매출이 정확히 반영된다. |
| `TC-02` | 당월 순매출 음수 | 환불 과다 발생 케이스 처리 | 현재 구현은 0원 클램프가 아니라 음수 정산값을 그대로 반환한다. |
| `TC-03` | 지급 완료 후 취소 | 정산 상태와 취소 간 상관관계 검증 | 기지급 월 스냅샷은 유지되고 차기 정산 월에 차감이 반영된다. |
| `TC-04` | 수수료율 변경 소급 | 수수료 이력 관리 검증 | 환불분 수수료는 환불 월이 아니라 판매 시점 수수료율 기준으로 계산된다. |
| `TC-05` | 밀리초/초 단위 경계 | KST 시간대 처리 정확성 검증 | `1일 00:00:00.000` 경계 기준으로 월 데이터가 정확히 분리된다. |

## 리뷰어 빠른 확인
- UI: `http://localhost:8080/`
- creators: `GET /api/creators`
- courses: `GET /api/courses`
- sales: `GET /api/sales?creatorId=creator-1&fromDate=2025-03-01&toDate=2025-03-31`
- monthly settlement: `GET /api/settlements/monthly?creatorId=creator-1&yearMonth=2025-03`
- admin settlement: `GET /api/admin/settlements?startDate=2025-03-01&endDate=2025-03-31`
- admin CSV: `GET /api/admin/settlements.csv?startDate=2025-03-01&endDate=2025-03-31`

## 참고 문서
- [README.md](README.md)
- [DATABASE_DESIGN.md](DATABASE_DESIGN.md)
- [REQUIREMENTS.md](REQUIREMENTS.md)
