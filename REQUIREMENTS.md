# Requirements

## 1. 프로젝트 목적
크리에이터 강의 판매/취소 데이터를 관리하고, 월별 정산 결과와 운영자 집계 결과를 조회하는 백엔드 API를 제공한다.

## 2. 기능 요구사항

### 2.1 판매 이력 관리
- 판매 이력을 등록할 수 있어야 한다.
- 판매 등록 시 아래 정보를 받는다.
  - 판매 ID
  - 강의 ID
  - 수강생 ID
  - 결제 금액
  - 결제 일시
- 강사별 판매 이력을 조회할 수 있어야 한다.
- 판매 이력 조회 시 기간 필터를 지원해야 한다.

### 2.2 취소 이력 관리
- 기존 판매 건에 대해 취소 이력을 등록할 수 있어야 한다.
- 취소 등록 시 아래 정보를 받는다.
  - 취소 ID
  - 환불 금액
  - 취소 일시
- 한 판매 건에 여러 취소 이력이 연결될 수 있어야 한다.
- 누적 환불 금액은 원 결제 금액을 초과할 수 없다.
- 취소 일시는 결제 일시보다 빠를 수 없다.

### 2.3 월별 정산 조회
- 강사 ID와 조회 연월을 입력받아 월별 정산 결과를 조회할 수 있어야 한다.
- 응답에는 아래 항목이 포함되어야 한다.
  - 총 판매 금액
  - 취소/환불 금액
  - 순매출 금액
  - 플랫폼 수수료
  - 정산 예정 금액
  - 판매 건수
  - 취소 건수
- 기본 수수료율은 20%다.

### 2.4 운영자 기간 집계 조회
- 시작일과 종료일을 입력받아 기간 내 전체 강사의 정산 현황을 조회할 수 있어야 한다.
- 응답에는 강사별 집계와 전체 합계가 포함되어야 한다.

### 2.5 시간 규칙
- 정산 기준 시간대는 KST(+09:00)다.
- 판매는 `paidAt` 기준 월에 반영한다.
- 취소는 `cancelledAt` 기준 월에 반영한다.
- 월 범위는 `[해당 월 1일 00:00:00, 다음 월 1일 00:00:00)` 규칙을 사용한다.

### 2.6 샘플 데이터
- 애플리케이션 시작 시 기본 샘플 데이터를 자동 적재해야 한다.
- 샘플 데이터는 creators, courses, sales, cancellations를 포함해야 한다.
- 샘플 데이터만으로 아래 시나리오를 검증할 수 있어야 한다.
  - 기본 월정산
  - 부분 취소
  - 전액 취소
  - 월경계 취소
  - 빈 월 조회
  - 같은 시각 취소
  - 미래 월 조회
  - HALF_UP 반올림
  - 추가 강사별 정산

## 3. 비기능 요구사항
- Java 21 기반 Spring Boot 프로젝트여야 한다.
- JPA를 사용해야 한다.
- 운영 DB는 MySQL을 사용해야 한다.
- 로컬 실행은 Docker Compose 기반 MySQL로 가능해야 한다.
- 테스트 코드는 자동 실행 가능해야 한다.
- 예외 상황은 JSON 에러 응답으로 반환해야 한다.

## 4. API 요구사항
- `GET /api/creators`
- `GET /api/courses`
- `POST /api/sales`
- `POST /api/sales/{saleId}/cancellations`
- `GET /api/sales`
- `GET /api/settlements/monthly`
- `GET /api/admin/settlements`

추가 구현:
- `POST /api/settlements`
- `PATCH /api/settlements/{settlementId}/confirm`
- `PATCH /api/settlements/{settlementId}/pay`
- `GET /api/admin/settlements.csv`
- `GET /api/admin/fee-rates`
- `POST /api/admin/fee-rates`

## 5. 데이터 모델 요구사항
- `Creator`
- `Course`
- `SaleRecord`
- `CancellationRecord`
- `Settlement`
- `FeeRateHistory`

관계:
- `Creator 1:N Course`
- `Course 1:N SaleRecord`
- `SaleRecord 1:N CancellationRecord`
- `Creator 1:N Settlement`

## 6. 검증 요구사항
- 존재하지 않는 creator/course/sale 요청은 오류를 반환해야 한다.
- 잘못된 `yearMonth` 형식은 400이어야 한다.
- 잘못된 날짜 범위는 400이어야 한다.
- 환불 금액 초과는 400이어야 한다.
- 중복 판매 ID / 취소 ID는 막아야 한다.
- 정산 상태 전이는 순서를 지켜야 한다.

## 7. 현재 구현에 추가된 운영 지원 요소
- 정적 제출용 UI
- 운영자 CSV 다운로드
- 수수료율 이력 관리
- MySQL reporting view / function 자동 생성

## 8. 현재 환경 기준
- Docker MySQL 컨테이너 이름: `creator-java-mysql`
- 기본 데이터베이스 이름: `creator_java`
- 기본 DB 포트: `3306`
- 기본 애플리케이션 포트: `8080`
