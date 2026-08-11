package com.ingoboka_api.v1.claim.events;

import java.util.UUID;

public record ClaimSubmittedEvent(UUID claimId) {}
