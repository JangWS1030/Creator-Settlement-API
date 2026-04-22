package io.github.jangws1030.creatorsettlementapi.course.api;

import io.github.jangws1030.creatorsettlementapi.common.exception.NotFoundException;
import io.github.jangws1030.creatorsettlementapi.course.domain.Course;
import io.github.jangws1030.creatorsettlementapi.course.repository.CourseRepository;
import io.github.jangws1030.creatorsettlementapi.creator.repository.CreatorRepository;
import java.util.List;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CreatorRepository creatorRepository;
    private final CourseRepository courseRepository;

    public CatalogController(
            CreatorRepository creatorRepository,
            CourseRepository courseRepository
    ) {
        this.creatorRepository = creatorRepository;
        this.courseRepository = courseRepository;
    }

    @GetMapping("/creators")
    public List<CatalogApiDtos.CreatorResponse> getCreators() {
        return creatorRepository.findAllByOrderByIdAsc()
                .stream()
                .map(creator -> new CatalogApiDtos.CreatorResponse(
                        creator.getId(),
                        creator.getName()
                ))
                .toList();
    }

    @GetMapping("/courses")
    public List<CatalogApiDtos.CourseResponse> getCourses(
            @RequestParam(required = false) String creatorId
    ) {
        List<Course> courses;
        if (StringUtils.hasText(creatorId)) {
            if (!creatorRepository.existsById(creatorId)) {
                throw new NotFoundException("Creator not found: " + creatorId);
            }
            courses = courseRepository.findByCreatorIdWithCreatorOrderByIdAsc(creatorId);
        } else {
            courses = courseRepository.findAllWithCreatorOrderByIdAsc();
        }

        return courses.stream()
                .map(course -> new CatalogApiDtos.CourseResponse(
                        course.getId(),
                        course.getCreator().getId(),
                        course.getCreator().getName(),
                        course.getTitle()
                ))
                .toList();
    }
}
