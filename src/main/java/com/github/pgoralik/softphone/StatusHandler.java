package com.github.pgoralik.softphone;

public interface StatusHandler {
    default void onRegistered(Softphone thisPhone) {
    }

    default void onRinging(Softphone thisPhone) {
    }

    default void onCallAnswered(Softphone thisPhone) {
    }

    default void onCallEnded(Softphone thisPhone) {
    }
}
