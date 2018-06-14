package com.ocadotech.callcentre.softphone;

public interface StatusHandler {
    default void onRinging(Softphone thisPhone) {
    }

    default void onCallAnswered(Softphone thisPhone) {
    }

    default void onCallEnded(Softphone thisPhone) {
    }
}
