package com.hoya.aicommerce.settlement.batch;

import com.hoya.aicommerce.settlement.domain.Settlement;
import com.hoya.aicommerce.settlement.domain.SettlementRepository;
import com.hoya.aicommerce.settlement.domain.SettlementStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SettlementBatchConfig {

    private final SettlementRepository settlementRepository;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job settlementJob() {
        return new JobBuilder("settlementJob", jobRepository)
                .start(completeSettlementsStep())
                .build();
    }

    @Bean
    public Step completeSettlementsStep() {
        return new StepBuilder("completeSettlementsStep", jobRepository)
                .<Settlement, Settlement>chunk(50, transactionManager)
                .reader(pendingSettlementsReader(null))
                .processor(settlementProcessor())
                .writer(settlementWriter())
                .build();
    }

    @Bean
    @StepScope
    public ListItemReader<Settlement> pendingSettlementsReader(
            @Value("#{jobParameters['targetDate']}") String targetDateStr) {
        LocalDate targetDate = targetDateStr != null
                ? LocalDate.parse(targetDateStr)
                : LocalDate.now().minusMonths(1)
                        .withDayOfMonth(LocalDate.now().minusMonths(1).lengthOfMonth());
        List<Settlement> settlements = settlementRepository
                .findByStatusAndPeriodEndLessThanEqual(SettlementStatus.PENDING, targetDate);
        return new ListItemReader<>(settlements);
    }

    @Bean
    public ItemProcessor<Settlement, Settlement> settlementProcessor() {
        return settlement -> {
            settlement.complete();
            return settlement;
        };
    }

    @Bean
    public ItemWriter<Settlement> settlementWriter() {
        return chunk -> settlementRepository.saveAll(chunk.getItems());
    }
}
