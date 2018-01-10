package com.ocadotech.callcentre.softphone;

import com.ocadotech.callcentre.softphone.impl.sip.SipClient;

import java.util.concurrent.Future;

public class Softphone {
    private String user;
    private String host;

    private SipClient sipClient = new SipClient();

    public Softphone(String user, String host) {
        this.user = user;
        this.host = host;

        sipClient.register();
    }

    public void call(String phoneNumber) {
        sipClient.initDialog(phoneNumber);
    }

    public void pushOnDialpad() {
        // TODO: Send DTMF via SIP INFO like Cisco Phones
    }

    public void answer() {

    }

    public void hangup() {

    }
}
