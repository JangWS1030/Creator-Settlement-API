package io.github.jangws1030.creatorsettlementapi.support;

import java.time.OffsetDateTime;

public record TimeWindow(
        OffsetDateTime start,
        OffsetDateTime endExclusive
) {
}
