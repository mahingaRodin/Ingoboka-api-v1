package com.ingoboka_api.v1.enrollment.repositories;

import com.ingoboka_api.v1.enrollment.models.QuoteAnswer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteAnswerRepository extends JpaRepository<QuoteAnswer, UUID> {

    List<QuoteAnswer> findByQuoteId(UUID quoteId);
}
