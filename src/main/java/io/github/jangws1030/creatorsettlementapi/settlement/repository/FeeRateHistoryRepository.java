package io.github.jangws1030.creatorsettlementapi.settlement.repository;

import io.github.jangws1030.creatorsettlementapi.settlement.domain.FeeRateHistory;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeRateHistoryRepository extends JpaRepository<FeeRateHistory, Long> {

    boolean existsByEffectiveFrom(LocalDate effectiveFrom);

    Optional<FeeRateHistory> findByEffectiveFrom(LocalDate effectiveFrom);

    Optional<FeeRateHistory> findTopByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDate targetDate);

    List<FeeRateHistory> findAllByOrderByEffectiveFromAsc();
}
