package io.github.jangws1030.creatorsettlementapi.settlement.repository;

import io.github.jangws1030.creatorsettlementapi.settlement.domain.Settlement;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRepository extends JpaRepository<Settlement, String> {

    boolean existsByCreatorIdAndYearMonth(String creatorId, String yearMonth);

    Optional<Settlement> findByCreatorIdAndYearMonth(String creatorId, String yearMonth);
}
