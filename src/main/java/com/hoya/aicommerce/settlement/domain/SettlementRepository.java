package com.hoya.aicommerce.settlement.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findBySellerIdAndPeriodStartAndPeriodEnd(
            Long sellerId, LocalDate periodStart, LocalDate periodEnd);

    List<Settlement> findBySellerId(Long sellerId);
}
