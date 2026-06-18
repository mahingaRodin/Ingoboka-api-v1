package com.ingoboka_api.v1.billing.services;

import com.ingoboka_api.v1.billing.models.Bill;
import com.ingoboka_api.v1.billing.models.PremiumSchedule;
import com.ingoboka_api.v1.billing.models.Receipt;
import com.ingoboka_api.v1.common.requests.CreateRefundRequest;
import com.ingoboka_api.v1.common.responses.BillResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.ReceiptResponse;
import com.ingoboka_api.v1.common.responses.ReconciliationResponse;
import com.ingoboka_api.v1.common.responses.RefundResponse;
import com.ingoboka_api.v1.billing.models.Payment;
import java.time.LocalDate;
import java.util.UUID;

public interface BillingFinanceService {

    Bill issueBillForSchedule(PremiumSchedule schedule, UUID organizationId, UUID policyId);

    Receipt issuePaymentReceipt(Payment payment, Bill bill, boolean partial);

    Receipt issueRefundReceipt(UUID refundId);

    RefundResponse createRefund(CreateRefundRequest request);

    PageResponse<RefundResponse> listRefunds(UUID paymentId, int page, int size);

    PageResponse<BillResponse> listPolicyBills(UUID policyId, int page, int size);

    PageResponse<ReceiptResponse> listPolicyReceipts(UUID policyId, int page, int size);

    ReconciliationResponse runReconciliation(LocalDate date);

    PageResponse<ReconciliationResponse> listReconciliations(int page, int size);
}
