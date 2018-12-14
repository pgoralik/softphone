package com.github.pgoralik.softphone.impl.sip;

import org.apache.log4j.Logger;

import javax.sip.SipFactory;
import javax.sip.SipProvider;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.github.pgoralik.softphone.impl.sip.HeaderUtils.getMaxForwardsHeader;
import static com.github.pgoralik.softphone.impl.sip.HeaderUtils.getViaHeaders;

class Registrator {
    private static final Logger LOG = Logger.getLogger(Registrator.class);

    private CallIdHeader callId;
    private String fromTag = "callcentre-" + UUID.randomUUID() + "-softphone";
    private long sequenceNumber = 1;
    private String user;
    private String host;
    private String localHostAddress;
    private int localHostPort;

    private HeaderFactory headerFactory;
    private AddressFactory addressFactory;
    private MessageFactory messageFactory;
    private SipProvider sipProvider;

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    private boolean isLogSIPMessagesEnabled;

    Registrator(SipProvider sipProvider, boolean isLogSIPMessagesEnabled) {
        try {
            this.addressFactory = SipFactory.getInstance().createAddressFactory();
            this.messageFactory = SipFactory.getInstance().createMessageFactory();
            this.headerFactory = SipFactory.getInstance().createHeaderFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.sipProvider = sipProvider;
        this.isLogSIPMessagesEnabled = isLogSIPMessagesEnabled;
    }

    void register(String user, String host, String localHostAddress, int localHostPort) {
        this.host = host;
        this.localHostAddress = localHostAddress;
        this.localHostPort = localHostPort;
        this.user = user;

        callId = sipProvider.getNewCallId();
        sendRegisterRequestWithExpires(3600);

        sequenceNumber++;
    }

    void unregister() {
        sendRegisterRequestWithExpires(0);
    }

    private void sendRegisterRequestWithExpires(int expires) {
        try {
            SipURI sipURI = addressFactory.createSipURI(user, host);
            sipURI.setTransportParam("UDP");
            Address toAddress = addressFactory.createAddress(sipURI);
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(sequenceNumber, Request.REGISTER);
            FromHeader fromHeader = headerFactory.createFromHeader(toAddress, fromTag);
            ToHeader toHeader = headerFactory.createToHeader(toAddress, null);
            Request request = messageFactory.createRequest(sipURI, Request.REGISTER, callId, cSeqHeader,
                    fromHeader, toHeader, getViaHeaders(localHostAddress, localHostPort), getMaxForwardsHeader());
            SipURI contactAddress = addressFactory.createSipURI(user, localHostAddress);
            contactAddress.setPort(localHostPort);
            ContactHeader contactHeader = headerFactory.createContactHeader(addressFactory.createAddress(contactAddress));
            request.addHeader(contactHeader);
            request.addHeader(headerFactory.createExpiresHeader(expires));
            logSIPMessage("Sent:\n" + request);
            sipProvider.sendRequest(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void scheduleReRegistration(int secondsToExpiration) {
        executorService.schedule(() -> register(user, host, localHostAddress, localHostPort), secondsToExpiration, TimeUnit.SECONDS);
    }

    private void logSIPMessage(Object message) {
        if (isLogSIPMessagesEnabled) {
            LOG.info("[" + user + "@" + host + "] " + message);
        }
    }
}
