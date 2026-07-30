package com.ingoboka_api.v1.ussd.session;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class UssdSession {

    private String sessionId;
    private String phoneNumber;
    private String serviceCode;
    private String step = "MAIN";
    private String language = "rw";
    private Map<String, String> data = new HashMap<>();

    public void put(String key, String value) {
        data.put(key, value);
    }

    public String get(String key) {
        return data.get(key);
    }
}
