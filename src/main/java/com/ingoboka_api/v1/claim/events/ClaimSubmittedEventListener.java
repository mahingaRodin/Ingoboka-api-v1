package com.ingoboka_api.v1.claim.events;

import com.ingoboka_api.v1.claim.impls.ClaimSubmitNotificationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimSubmittedEventListener {

    private final ClaimSubmitNotificationHandler notificationHandler;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClaimSubmitted(ClaimSubmittedEvent event) {
        try {
            notificationHandler.sendSubmitNotifications(event.claimId());
        } catch (Exception ex) {
            log.error(
                    "Failed to send claim submit notifications for {}: {}",
                    event.claimId(),
                    ex.getMessage(),
                    ex);
        }
    }
}
