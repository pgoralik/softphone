package com.ocadotech.callcentre.softphone.impl.sip;

import javax.sip.InvalidArgumentException;
import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ViaHeader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

class HeaderUtils {
    private static final int DEFAULT_MAX_FORWARD = 70;
    private static HeaderFactory headerFactory;

    static {
        try {
            headerFactory = SipFactory.getInstance().createHeaderFactory();
        } catch (PeerUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    static List<ViaHeader> getViaHeaders(String localHostAddress, int port) {
        List<ViaHeader> viaHeaders = new ArrayList<>();
        try {
            viaHeaders.add(headerFactory.createViaHeader(localHostAddress, port, "udp", null));
        } catch (ParseException | InvalidArgumentException e) {
            throw new RuntimeException(e);
        }
        return viaHeaders;
    }

    static MaxForwardsHeader getMaxForwardsHeader() {
        try {
            return headerFactory.createMaxForwardsHeader(DEFAULT_MAX_FORWARD);
        } catch (InvalidArgumentException e) {
            throw new IllegalStateException(e);
        }
    }
}
