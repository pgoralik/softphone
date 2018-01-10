package com.ocadotech.callcentre.softphone;

import com.ocadotech.callcentre.softphone.impl.sip.SipClient;

import javax.sip.Dialog;
import javax.sip.DialogState;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws Exception {
        Softphone customerSoftphone = new Softphone("6001", "192.168.0.105");
        customerSoftphone.call("200");
    }
}
