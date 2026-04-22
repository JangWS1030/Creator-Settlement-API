package io.github.jangws1030.creatorsettlementapi.course.domain;

import io.github.jangws1030.creatorsettlementapi.creator.domain.Creator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "courses",
        indexes = {
                @Index(name = "idx_courses_creator_id", columnList = "creator_id")
        }
)
public class Course {

    @Id
    @Column(nullable = false, updatable = false, length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;

    @Column(nullable = false, length = 150)
    private String title;

    protected Course() {
    }

    public Course(String id, Creator creator, String title) {
        this.id = id;
        this.creator = creator;
        this.title = title;
    }

    public void reviseCatalog(Creator creator, String title) {
        this.creator = creator;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public Creator getCreator() {
        return creator;
    }

    public String getTitle() {
        return title;
    }
}
