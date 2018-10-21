package com.github.pgoralik.softphone.impl.status;

import com.github.pgoralik.softphone.Softphone;

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
