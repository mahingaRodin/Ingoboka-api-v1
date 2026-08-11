package com.ingoboka_api.v1.identity.services;

import com.ingoboka_api.v1.identity.models.User;

public interface EmailReverificationService {

    /** Apply a pending email change and send verification OTP + link to the new address. */
    void applyEmailChange(User user, String newEmail);

    /** Send a fresh OTP to the user's current (pending) email address. */
    void sendEmailVerificationOtp(User user);
}
