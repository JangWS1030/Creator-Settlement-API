package io.github.jangws1030.creatorsettlementapi.sample;

import io.github.jangws1030.creatorsettlementapi.course.domain.Course;
import io.github.jangws1030.creatorsettlementapi.course.repository.CourseRepository;
import io.github.jangws1030.creatorsettlementapi.creator.domain.Creator;
import io.github.jangws1030.creatorsettlementapi.creator.repository.CreatorRepository;
import io.github.jangws1030.creatorsettlementapi.sale.domain.CancellationRecord;
import io.github.jangws1030.creatorsettlementapi.sale.domain.SaleRecord;
import io.github.jangws1030.creatorsettlementapi.sale.repository.CancellationRecordRepository;
import io.github.jangws1030.creatorsettlementapi.sale.repository.SaleRecordRepository;
import io.github.jangws1030.creatorsettlementapi.settlement.application.FeeRateService;
import io.github.jangws1030.creatorsettlementapi.settlement.domain.FeeRateHistory;
import io.github.jangws1030.creatorsettlementapi.settlement.repository.FeeRateHistoryRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleDataService {

    private final CreatorRepository creatorRepository;
    private final CourseRepository courseRepository;
    private final SaleRecordRepository saleRecordRepository;
    private final CancellationRecordRepository cancellationRecordRepository;
    private final FeeRateHistoryRepository feeRateHistoryRepository;

    public SampleDataService(
            CreatorRepository creatorRepository,
            CourseRepository courseRepository,
            SaleRecordRepository saleRecordRepository,
            CancellationRecordRepository cancellationRecordRepository,
            FeeRateHistoryRepository feeRateHistoryRepository
    ) {
        this.creatorRepository = creatorRepository;
        this.courseRepository = courseRepository;
        this.saleRecordRepository = saleRecordRepository;
        this.cancellationRecordRepository = cancellationRecordRepository;
        this.feeRateHistoryRepository = feeRateHistoryRepository;
    }

    @Transactional
    public void seedDefaultDataIfEmpty() {
        upsertFeeRate(LocalDate.of(2020, 1, 1), FeeRateService.DEFAULT_FEE_RATE_PERCENTAGE);

        Creator creator1 = upsertCreator("creator-1", "김강사");
        Creator creator2 = upsertCreator("creator-2", "이강사");
        Creator creator3 = upsertCreator("creator-3", "박강사");
        Creator creator4 = upsertCreator("creator-4", "최강사");
        Creator creator5 = upsertCreator("creator-5", "정강사");
        Creator creator6 = upsertCreator("creator-6", "한강사");

        Course course1 = upsertCourse("course-1", creator1, "Spring Boot 입문");
        Course course2 = upsertCourse("course-2", creator1, "JPA 실전");
        Course course3 = upsertCourse("course-3", creator2, "Kotlin 기초");
        Course course4 = upsertCourse("course-4", creator3, "MSA 설계");
        Course course5 = upsertCourse("course-5", creator4, "Querydsl 튜닝");
        Course course6 = upsertCourse("course-6", creator4, "Docker 배포 자동화");
        Course course7 = upsertCourse("course-7", creator5, "Redis 실전");
        Course course8 = upsertCourse("course-8", creator6, "관측성 입문");
        Course course9 = upsertCourse("course-9", creator6, "배치 처리 워크숍");

        SaleRecord sale1 = upsertSale("sale-1", course1, "student-1", 50_000L, "2025-03-05T10:00:00+09:00");
        SaleRecord sale2 = upsertSale("sale-2", course1, "student-2", 50_000L, "2025-03-15T14:30:00+09:00");
        SaleRecord sale3 = upsertSale("sale-3", course2, "student-3", 80_000L, "2025-03-20T09:00:00+09:00");
        SaleRecord sale4 = upsertSale("sale-4", course2, "student-4", 80_000L, "2025-03-22T11:00:00+09:00");
        SaleRecord sale5 = upsertSale("sale-5", course3, "student-5", 60_000L, "2025-01-31T23:30:00+09:00");
        upsertSale("sale-6", course3, "student-6", 60_000L, "2025-03-10T16:00:00+09:00");
        upsertSale("sale-7", course4, "student-7", 120_000L, "2025-02-14T10:00:00+09:00");
        SaleRecord sale8 = upsertSale("sale-8", course1, "student-8", 100_000L, "2025-04-05T10:00:00+09:00");
        SaleRecord sale9 = upsertSale("sale-9", course2, "student-9", 90_000L, "2025-04-30T23:59:59+09:00");
        upsertSale("sale-10", course3, "student-10", 150_000L, "2026-12-15T09:00:00+09:00");
        SaleRecord sale11 = upsertSale("sale-11", course4, "student-11", 40_000L, "2025-04-10T10:00:00+09:00");
        upsertSale("sale-12", course3, "student-12", 33_333L, "2025-11-11T11:11:00+09:00");
        SaleRecord sale13 = upsertSale("sale-13", course5, "student-13", 95_000L, "2025-06-03T09:15:00+09:00");
        SaleRecord sale14 = upsertSale("sale-14", course6, "student-14", 110_000L, "2025-06-21T20:10:00+09:00");
        SaleRecord sale15 = upsertSale("sale-15", course7, "student-15", 75_000L, "2025-07-08T08:45:00+09:00");
        SaleRecord sale16 = upsertSale("sale-16", course7, "student-16", 88_000L, "2025-07-20T18:20:00+09:00");
        SaleRecord sale17 = upsertSale("sale-17", course8, "student-17", 130_000L, "2025-08-12T10:30:00+09:00");
        SaleRecord sale18 = upsertSale("sale-18", course9, "student-18", 54_000L, "2025-10-02T14:00:00+09:00");
        upsertSale("sale-19", course8, "student-19", 66_000L, "2025-12-03T09:00:00+09:00");

        upsertCancellation("cancel-1", sale3, 80_000L, "2025-03-28T13:00:00+09:00");
        upsertCancellation("cancel-2", sale4, 30_000L, "2025-03-29T15:00:00+09:00");
        upsertCancellation("cancel-3", sale5, 60_000L, "2025-02-01T09:00:00+09:00");
        upsertCancellation("cancel-4", sale8, 20_000L, "2025-04-10T09:00:00+09:00");
        upsertCancellation("cancel-5", sale8, 30_000L, "2025-04-20T15:30:00+09:00");
        upsertCancellation("cancel-6", sale9, 90_000L, "2025-05-01T00:00:00+09:00");
        upsertCancellation("cancel-7", sale11, 40_000L, "2025-04-10T10:00:00+09:00");
        upsertCancellation("cancel-8", sale14, 20_000L, "2025-06-25T13:30:00+09:00");
        upsertCancellation("cancel-9", sale16, 88_000L, "2025-08-01T00:00:00+09:00");
        upsertCancellation("cancel-10", sale17, 30_000L, "2025-08-18T11:00:00+09:00");
        upsertCancellation("cancel-11", sale18, 14_000L, "2025-10-10T16:40:00+09:00");
    }

    private Creator upsertCreator(String id, String name) {
        return creatorRepository.findById(id)
                .map(existing -> {
                    existing.rename(name);
                    return existing;
                })
                .orElseGet(() -> creatorRepository.save(new Creator(id, name)));
    }

    private Course upsertCourse(String id, Creator creator, String title) {
        return courseRepository.findById(id)
                .map(existing -> {
                    existing.reviseCatalog(creator, title);
                    return existing;
                })
                .orElseGet(() -> courseRepository.save(new Course(id, creator, title)));
    }

    private void upsertFeeRate(LocalDate effectiveFrom, int feeRatePercentage) {
        OffsetDateTime createdAt = OffsetDateTime.parse("2020-01-01T00:00:00+09:00");
        feeRateHistoryRepository.findByEffectiveFrom(effectiveFrom)
                .ifPresentOrElse(
                        existing -> existing.revise(feeRatePercentage, createdAt),
                        () -> feeRateHistoryRepository.save(new FeeRateHistory(
                                effectiveFrom,
                                feeRatePercentage,
                                createdAt
                        ))
                );
    }

    private SaleRecord upsertSale(
            String id,
            Course course,
            String studentId,
            long amount,
            String paidAt
    ) {
        OffsetDateTime paidAtValue = OffsetDateTime.parse(paidAt);
        return saleRecordRepository.findById(id)
                .map(existing -> {
                    existing.revise(course, studentId, amount, paidAtValue);
                    return existing;
                })
                .orElseGet(() -> saleRecordRepository.save(new SaleRecord(
                        id,
                        course,
                        studentId,
                        amount,
                        paidAtValue
                )));
    }

    private void upsertCancellation(
            String id,
            SaleRecord saleRecord,
            long refundAmount,
            String cancelledAt
    ) {
        OffsetDateTime cancelledAtValue = OffsetDateTime.parse(cancelledAt);
        cancellationRecordRepository.findById(id)
                .ifPresentOrElse(
                        existing -> existing.revise(saleRecord, refundAmount, cancelledAtValue),
                        () -> cancellationRecordRepository.save(new CancellationRecord(
                                id,
                                saleRecord,
                                refundAmount,
                                cancelledAtValue
                        ))
                );
    }
}
