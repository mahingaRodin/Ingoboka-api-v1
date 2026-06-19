package com.ingoboka_api.v1.messaging.services;

public interface SmsDeliveryService {

    void send(String phoneNumber, String message);
}
