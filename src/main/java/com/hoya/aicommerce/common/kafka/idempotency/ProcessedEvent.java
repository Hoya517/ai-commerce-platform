package com.hoya.aicommerce.common.kafka.idempotency;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_event")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public static ProcessedEvent of(String eventId) {
        ProcessedEvent e = new ProcessedEvent();
        e.eventId = eventId;
        e.processedAt = LocalDateTime.now();
        return e;
    }
}
