package io.github.jangws1030.creatorsettlementapi.course.repository;

import io.github.jangws1030.creatorsettlementapi.course.domain.Course;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, String> {

    @Query("""
            select c
            from Course c
            join fetch c.creator
            order by c.id asc
            """)
    List<Course> findAllWithCreatorOrderByIdAsc();

    @Query("""
            select c
            from Course c
            join fetch c.creator
            where c.creator.id = :creatorId
            order by c.id asc
            """)
    List<Course> findByCreatorIdWithCreatorOrderByIdAsc(@Param("creatorId") String creatorId);
}
