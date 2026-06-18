package com.ingoboka_api.v1.enrollment.repositories;

import com.ingoboka_api.v1.enrollment.models.Quote;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteRepository extends JpaRepository<Quote, UUID> {

    Optional<Quote> findByQuoteReference(String quoteReference);
}
