package com.ingoboka_api.v1.revenue.services;

import com.ingoboka_api.v1.billing.models.Payment;
import com.ingoboka_api.v1.common.requests.CreateContractPriceRuleRequest;
import com.ingoboka_api.v1.common.responses.ContractPriceRuleResponse;
import com.ingoboka_api.v1.common.responses.InvoiceResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.RevenueLedgerResponse;
import java.time.LocalDate;
import java.util.UUID;

public interface RevenueCommissionService {

    ContractPriceRuleResponse createPriceRule(CreateContractPriceRuleRequest request);

    PageResponse<ContractPriceRuleResponse> listPriceRules(int page, int size);

    PageResponse<RevenueLedgerResponse> listLedger(int page, int size);

    PageResponse<InvoiceResponse> listInvoices(int page, int size);

    InvoiceResponse generateInvoice(LocalDate periodStart, LocalDate periodEnd);

    void recordCommissionForPayment(Payment payment);
}
