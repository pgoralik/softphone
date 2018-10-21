package com.github.pgoralik.softphone;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class SoftphoneBuilder {
    private String user;
    private String host;
    private String localHostAddress;
    private boolean isLogSIPMessagesEnabled;

    public SoftphoneBuilder(String user, String host) {
        this.user = user;
        this.host = host;
        this.localHostAddress = defaultLocalHostAddress();
    }

    public SoftphoneBuilder withLocalHostAddress(String localHostAddress) {
        this.localHostAddress = localHostAddress;
        return this;
    }

    public SoftphoneBuilder withLoggingSIPMessagesEnabled() {
        this.isLogSIPMessagesEnabled = true;
        return this;
    }

    public Softphone build() {
        return new Softphone(user, host, localHostAddress, isLogSIPMessagesEnabled);
    }

    private String defaultLocalHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
