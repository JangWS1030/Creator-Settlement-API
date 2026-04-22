package io.github.jangws1030.creatorsettlementapi.course.api;

public final class CatalogApiDtos {

    private CatalogApiDtos() {
    }

    public record CreatorResponse(
            String id,
            String name
    ) {
    }

    public record CourseResponse(
            String id,
            String creatorId,
            String creatorName,
            String title
    ) {
    }
}
