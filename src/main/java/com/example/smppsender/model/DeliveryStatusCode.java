package com.example.smppsender.model;

public enum DeliveryStatusCode {
    ACCEPTD, DELIVRD, UNDELIV, EXPIRED, FAILED, REJECTD, DELETED, UNKNOWN;

    public static DeliveryStatusCode fromString(String code) {
        try {
            return DeliveryStatusCode.valueOf(code.toUpperCase());
        } catch (Exception e) {
            return UNKNOWN;
        }
    }
}
