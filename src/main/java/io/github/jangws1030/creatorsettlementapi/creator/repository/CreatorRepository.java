package io.github.jangws1030.creatorsettlementapi.creator.repository;

import io.github.jangws1030.creatorsettlementapi.creator.domain.Creator;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatorRepository extends JpaRepository<Creator, String> {

    List<Creator> findAllByOrderByIdAsc();
}
