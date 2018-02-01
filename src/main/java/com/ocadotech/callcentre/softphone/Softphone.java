package com.ocadotech.callcentre.softphone;

import com.ocadotech.callcentre.softphone.impl.sip.SipClient;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Softphone {
    private SipClient sipClient;

    Softphone(String user, String host, String localhostAddress) {
        sipClient = new SipClient(user, host, localhostAddress);
        sipClient.register();
    }

    public void call(String phoneNumber) {
        sipClient.initDialog(phoneNumber);
    }

    public void pushOnDialpad(String buttonSequence) {
        for(char button : buttonSequence.toCharArray()) {
           sipClient.info(button);
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void answer() {

    }

    public void hangup() {

    }

    public void waitMiliseconds(int ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    public void whenCalled(Consumer<Softphone> action) {
    }
}
