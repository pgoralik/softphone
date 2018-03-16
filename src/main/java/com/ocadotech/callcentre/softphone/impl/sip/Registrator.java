package com.ocadotech.callcentre.softphone.impl.sip;

import javax.sip.SipProvider;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ToHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.ocadotech.callcentre.softphone.impl.sip.HeaderUtils.getMaxForwardsHeader;
import static com.ocadotech.callcentre.softphone.impl.sip.HeaderUtils.getViaHeaders;

class Registrator {
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

    Registrator(HeaderFactory headerFactory, AddressFactory addressFactory, MessageFactory messageFactory, SipProvider sipProvider) {
        this.headerFactory = headerFactory;
        this.addressFactory = addressFactory;
        this.messageFactory = messageFactory;
        this.sipProvider = sipProvider;
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
            sipProvider.sendRequest(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void scheduleReRegistration(int secondsToExpiration) {
        executorService.schedule(() -> register(user, host, localHostAddress, localHostPort), secondsToExpiration, TimeUnit.SECONDS);
    }
}
