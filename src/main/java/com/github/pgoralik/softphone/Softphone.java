package com.github.pgoralik.softphone;

import com.github.pgoralik.softphone.impl.sip.SipClient;
import com.github.pgoralik.softphone.impl.status.Status;

import java.util.concurrent.TimeUnit;

public class Softphone implements AutoCloseable {
    private SipClient sipClient;

    Softphone(String user, String host, String localhostAddress) {
        sipClient = new SipClient(user, host, localhostAddress, this);
    }

    public void setStatusHandler(StatusHandler statusHandler) {
        sipClient.setStatusHandler(statusHandler);
    }

    @Override
    public void close() {
        sipClient.close();
    }

    public void call(String phoneNumber) {
        sipClient.initDialog(phoneNumber);
    }

    public void register() {
        sipClient.register();
    }

    public void pushOnDialpad(String buttonSequence) {
        for (char button : buttonSequence.toCharArray()) {
            sipClient.info(button);
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Status getStatus() {
        throw new RuntimeException("Not implemented yet.");
    }

    public void answer() {
        sipClient.answerOkToInvite();
    }

    public void reject() {
        sipClient.answerBusyToInvite();
    }

    public void hangup() {
        sipClient.bye();
    }

    public void waitMiliseconds(int ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
