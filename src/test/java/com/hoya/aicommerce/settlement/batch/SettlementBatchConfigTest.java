package com.hoya.aicommerce.settlement.batch;

import com.hoya.aicommerce.settlement.domain.Settlement;
import com.hoya.aicommerce.settlement.domain.SettlementRepository;
import com.hoya.aicommerce.settlement.domain.SettlementStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SettlementBatchConfigTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job settlementJob;

    @Autowired
    private SettlementRepository settlementRepository;

    @BeforeEach
    void setUp() {
        settlementRepository.deleteAll();
    }

    @Test
    void PENDING_정산이_배치_실행_후_COMPLETED_상태가_된다() throws Exception {
        LocalDate periodStart = LocalDate.of(2026, 2, 1);
        LocalDate periodEnd = LocalDate.of(2026, 2, 28);
        Settlement settlement = Settlement.create(1L, periodStart, periodEnd);
        settlement.addPayment(com.hoya.aicommerce.common.domain.Money.of(100_000L),
                new java.math.BigDecimal("0.10"));
        settlementRepository.save(settlement);

        JobParameters params = new JobParametersBuilder()
                .addString("targetDate", "2026-02-28")
                .addString("runTime", LocalDateTime.now().toString())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(settlementJob, params);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        Settlement updated = settlementRepository.findById(settlement.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
    }

    @Test
    void 이미_COMPLETED_정산은_배치에서_처리되지_않는다() throws Exception {
        LocalDate periodStart = LocalDate.of(2026, 1, 1);
        LocalDate periodEnd = LocalDate.of(2026, 1, 31);
        Settlement settlement = Settlement.create(2L, periodStart, periodEnd);
        settlement.addPayment(com.hoya.aicommerce.common.domain.Money.of(50_000L),
                new java.math.BigDecimal("0.10"));
        settlement.complete();
        settlementRepository.save(settlement);

        JobParameters params = new JobParametersBuilder()
                .addString("targetDate", "2026-01-31")
                .addString("runTime", LocalDateTime.now().toString() + "-completed-test")
                .toJobParameters();

        JobExecution execution = jobLauncher.run(settlementJob, params);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        int writeCount = execution.getStepExecutions().stream()
                .mapToInt(s -> (int) s.getWriteCount())
                .sum();
        assertThat(writeCount).isEqualTo(0);
    }

    @Test
    void targetDate_이후_기간의_정산은_처리되지_않는다() throws Exception {
        // periodEnd = 2026-03-31 이지만 targetDate = 2026-02-28 → 처리 안 됨
        LocalDate periodStart = LocalDate.of(2026, 3, 1);
        LocalDate periodEnd = LocalDate.of(2026, 3, 31);
        Settlement settlement = Settlement.create(3L, periodStart, periodEnd);
        settlement.addPayment(com.hoya.aicommerce.common.domain.Money.of(80_000L),
                new java.math.BigDecimal("0.10"));
        settlementRepository.save(settlement);

        JobParameters params = new JobParametersBuilder()
                .addString("targetDate", "2026-02-28")
                .addString("runTime", LocalDateTime.now().toString() + "-future-test")
                .toJobParameters();

        JobExecution execution = jobLauncher.run(settlementJob, params);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        Settlement unchanged = settlementRepository.findById(settlement.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(SettlementStatus.PENDING);
    }
}
