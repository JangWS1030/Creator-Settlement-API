package io.github.jangws1030.creatorsettlementapi.creator.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "creators")
public class Creator {

    @Id
    @Column(nullable = false, updatable = false, length = 50)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    protected Creator() {
    }

    public Creator(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public void rename(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
