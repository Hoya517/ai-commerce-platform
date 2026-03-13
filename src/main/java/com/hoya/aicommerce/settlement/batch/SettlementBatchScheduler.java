package com.hoya.aicommerce.settlement.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job settlementJob;

    @Scheduled(cron = "0 0 2 1 * *")
    public void runSettlementBatch() {
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        LocalDate targetDate = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("targetDate", targetDate.toString())
                    .addString("runTime", LocalDateTime.now().toString())
                    .toJobParameters();
            jobLauncher.run(settlementJob, params);
            log.info("[Settlement] 정산 배치 완료 — targetDate={}", targetDate);
        } catch (Exception e) {
            log.error("[Settlement] 정산 배치 실패 — targetDate={}", targetDate, e);
        }
    }
}
