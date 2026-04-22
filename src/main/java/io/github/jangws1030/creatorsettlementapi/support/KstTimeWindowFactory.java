package io.github.jangws1030.creatorsettlementapi.support;

import io.github.jangws1030.creatorsettlementapi.common.exception.BadRequestException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class KstTimeWindowFactory {

    private static final ZoneOffset KST_OFFSET = ZoneOffset.ofHours(9);
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private KstTimeWindowFactory() {
    }

    public static YearMonth parseYearMonth(String yearMonthText) {
        try {
            return YearMonth.parse(yearMonthText, YEAR_MONTH_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BadRequestException("yearMonth must follow yyyy-MM format.");
        }
    }

    public static TimeWindow month(String yearMonthText) {
        YearMonth yearMonth = parseYearMonth(yearMonthText);
        OffsetDateTime start = atStartOfDay(yearMonth.atDay(1));
        OffsetDateTime endExclusive = atStartOfDay(yearMonth.plusMonths(1).atDay(1));
        return new TimeWindow(start, endExclusive);
    }

    public static TimeWindow dateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("startDate and endDate are required.");
        }

        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("startDate must be before or equal to endDate.");
        }

        return new TimeWindow(
                atStartOfDay(startDate),
                atStartOfDay(endDate.plusDays(1))
        );
    }

    public static OffsetDateTime atStartOfDay(LocalDate date) {
        return LocalDateTime.of(date, java.time.LocalTime.MIN).atOffset(KST_OFFSET);
    }
}
