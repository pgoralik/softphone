package com.github.pgoralik.softphone;

import com.github.pgoralik.softphone.impl.sip.SipClient;
import com.github.pgoralik.softphone.impl.status.StatusHandler;

import java.util.concurrent.TimeUnit;

public class Softphone implements AutoCloseable {
    private SipClient sipClient;

    Softphone(String user, String host, String localhostAddress, boolean isLogSIPMessagesEnabled) {
        sipClient = new SipClient(user, host, localhostAddress, isLogSIPMessagesEnabled, this);
    }

    public void setStatusHandler(StatusHandler statusHandler) {
        sipClient.setStatusHandler(statusHandler);
    }

    public void call(String phoneNumber) {
        sipClient.initDialog(phoneNumber);
    }

    public void register() {
        sipClient.register();
    }

    public void pushKeysOnDialpad(String buttonSequence) {
        for (char button : buttonSequence.toCharArray()) {
            sipClient.info(button);
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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

    @Override
    public void close() {
        sipClient.close();
    }
}
