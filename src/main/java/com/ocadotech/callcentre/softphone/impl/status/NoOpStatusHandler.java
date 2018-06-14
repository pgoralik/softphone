package com.ocadotech.callcentre.softphone.impl.status;

import com.ocadotech.callcentre.softphone.Softphone;
import com.ocadotech.callcentre.softphone.StatusHandler;

public class NoOpStatusHandler implements StatusHandler {
    @Override
    public void onRinging(Softphone thisPhone) {
    }

    @Override
    public void onCallAnswered(Softphone thisPhone) {
    }

    @Override
    public void onCallEnded(Softphone thisPhone) {
    }
}
