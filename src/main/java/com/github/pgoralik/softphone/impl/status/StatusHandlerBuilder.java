package com.github.pgoralik.softphone.impl.status;

import com.github.pgoralik.softphone.Softphone;

import java.util.function.Consumer;

public class StatusHandlerBuilder {
    private Consumer<Softphone> onRegistered = (thisPhone) -> {
    };
    private Consumer<Softphone> onRinging = (thisPhone) -> {
    };
    private Consumer<Softphone> onCallAnswered = (thisPhone) -> {
    };
    private Consumer<Softphone> onCallEnded = (thisPhone) -> {
    };

    public StatusHandlerBuilder withOnRegistered(Consumer<Softphone> callback) {
        this.onRegistered = callback;
        return this;
    }

    public StatusHandlerBuilder withOnRinging(Consumer<Softphone> callback) {
        this.onRinging = callback;
        return this;
    }

    public StatusHandlerBuilder withOnCallAnswered(Consumer<Softphone> callback) {
        this.onCallAnswered = callback;
        return this;
    }

    public StatusHandlerBuilder withOnCallEnded(Consumer<Softphone> callback) {
        this.onCallEnded = callback;
        return this;
    }

    public StatusHandler build() {
        return new StatusHandler() {
            @Override
            public void onRegistered(Softphone thisPhone) {
                onRegistered.accept(thisPhone);
            }

            @Override
            public void onRinging(Softphone thisPhone) {
                onRinging.accept(thisPhone);
            }

            @Override
            public void onCallAnswered(Softphone thisPhone) {
                onCallAnswered.accept(thisPhone);
            }

            @Override
            public void onCallEnded(Softphone thisPhone) {
                onCallEnded.accept(thisPhone);
            }
        };
    }
}
