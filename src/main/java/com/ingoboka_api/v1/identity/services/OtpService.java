package com.ingoboka_api.v1.identity.services;

public interface OtpService {

    String generateAndStore(String purpose, String destination);

    boolean verify(String purpose, String destination, String otp);

    void clear(String purpose, String destination);
}
