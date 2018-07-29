package com.github.pgoralik.softphone;

import com.github.pgoralik.softphone.impl.status.NoOpStatusHandler;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class SoftphoneBuilder {
    private String user;
    private String host;
    private String localHostAddress;
    private StatusHandler statusHandler;

    public SoftphoneBuilder(String user, String host) {
        this.user = user;
        this.host = host;
        this.localHostAddress = defaultLocalHostAddress();
        this.statusHandler = new NoOpStatusHandler();
    }

    public SoftphoneBuilder withLocalHostAddress(String localHostAddress) {
        this.localHostAddress = localHostAddress;
        return this;
    }

    // TODO: Defining behavior should be moved out from builder. It will allow to reuse softphone instance in between different test scenarios
    public SoftphoneBuilder withStatusListener(StatusHandler statusHandler) {
        this.statusHandler = statusHandler;
        return this;
    }

    public Softphone build() {
        return new Softphone(user, host, localHostAddress, statusHandler);
    }

    private String defaultLocalHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
