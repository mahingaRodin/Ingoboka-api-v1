package com.ingoboka_api.v1.enrollment.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "quote_answers")
public class QuoteAnswer {

    @Id
    private UUID id;

    @Column(name = "quote_id", nullable = false)
    private UUID quoteId;

    @Column(name = "question_key", nullable = false, length = 120)
    private String questionKey;

    @Column(name = "answer_value", nullable = false, columnDefinition = "TEXT")
    private String answerValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
