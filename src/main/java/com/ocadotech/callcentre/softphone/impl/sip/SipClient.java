package com.ocadotech.callcentre.softphone.impl.sip;

import gov.nist.javax.sip.message.SIPResponse;

import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static com.ocadotech.callcentre.softphone.impl.utils.NetworkUtils.localHostAddress;

public class SipClient implements SipListener {

    private static final String USER = "6001";
    private static final String HOST = "192.168.0.105";

    private SipStack sipStack;
    private SipProvider sipProviderUdp;

    private MessageFactory messageFactory;
    private HeaderFactory headerFactory;
    private AddressFactory addressFactory;

    public SipClient() {
        try {
            SipFactory sipFactory = SipFactory.getInstance();
            Properties properties = new Properties();
            properties.setProperty("javax.sip.STACK_NAME", "SOFTPHONE");
            sipStack = sipFactory.createSipStack(properties);

            ListeningPoint udp = sipStack.createListeningPoint(localHostAddress(), 9999, "udp");
            sipProviderUdp = sipStack.createSipProvider(udp);
            sipProviderUdp.addSipListener(this);

            addressFactory = SipFactory.getInstance().createAddressFactory();
            messageFactory = SipFactory.getInstance().createMessageFactory();
            headerFactory = SipFactory.getInstance().createHeaderFactory();

            sipStack.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void register() {
        try {
            SipURI sipURI = SipFactory.getInstance().createAddressFactory().createSipURI(USER, HOST);
            Address toAddress = SipFactory.getInstance().createAddressFactory().createAddress(sipURI);
            CallIdHeader newCallId = sipProviderUdp.getNewCallId();
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.REGISTER);
            FromHeader fromHeader = headerFactory.createFromHeader(toAddress, null);
            ToHeader toHeader = headerFactory.createToHeader(toAddress, null);
            Request request = messageFactory.createRequest(sipURI, Request.REGISTER, newCallId, cSeqHeader,
                    fromHeader, toHeader, getViaHeaders(), getMaxForwardsHeader());
            sipProviderUdp.sendRequest(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MaxForwardsHeader getMaxForwardsHeader() throws InvalidArgumentException {
        return headerFactory.createMaxForwardsHeader(70);
    }

    public Dialog initDialog(String destUser) {
        try {
            SipURI sipURI = addressFactory.createSipURI(USER, HOST);
            CallIdHeader newCallId = sipProviderUdp.getNewCallId();
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.INVITE);
            Address fromAddress = addressFactory.createAddress(sipURI);
            FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, UUID.randomUUID().toString());
            SipURI toSipURI = addressFactory.createSipURI(destUser, HOST);
            Address toAddress = addressFactory.createAddress(toSipURI);
            ToHeader toHeader = headerFactory.createToHeader(toAddress, null);
            Request request = messageFactory.createRequest(toSipURI, Request.INVITE, newCallId, cSeqHeader, fromHeader, toHeader, getViaHeaders(), getMaxForwardsHeader());
            ContactHeader contactHeader = headerFactory.createContactHeader(fromAddress);
            request.addHeader(contactHeader);
            ClientTransaction clientTransaction = sipProviderUdp.getNewClientTransaction(request);
            clientTransaction.sendRequest();
            return clientTransaction.getDialog();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<ViaHeader> getViaHeaders() throws ParseException, InvalidArgumentException {
        List<ViaHeader> viaHeaders = new ArrayList<>();
        viaHeaders.add(headerFactory.createViaHeader(localHostAddress(), 9999, "udp", null));
        return viaHeaders;
    }

    public void close() {
        sipStack.stop();
    }

    @Override
    public void processRequest(RequestEvent requestEvent) {}

    @Override
    public void processResponse(ResponseEvent responseEvent) {
        SIPResponse response = (SIPResponse) responseEvent.getResponse();
        System.out.println("processResponse " + response.getStatusCode());
        System.out.println(response);
        Dialog dialog = responseEvent.getDialog();
        if(dialog != null && response.isFinalResponse() && response.getStatusCode() == Response.OK) {
            try {
                // TODO: Maybe there is a better way to create ACK request when we have a dialog
                Address remotePartyAddress = responseEvent.getDialog().getRemoteParty();
                HeaderFactory headerFactory = SipFactory.getInstance().createHeaderFactory();
                CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.ACK);
                Request ackRequest = SipFactory.getInstance().createMessageFactory().createRequest(remotePartyAddress.getURI(), Request.ACK, dialog.getCallId(), cSeqHeader,
                        headerFactory.createFromHeader(dialog.getLocalParty(), dialog.getLocalTag()),
                        headerFactory.createToHeader(remotePartyAddress, dialog.getRemoteTag()),
                        response.getViaHeaders(), headerFactory.createMaxForwardsHeader(70));
                System.out.println("Sending ACK");
                System.out.println(ackRequest);
                dialog.sendAck(ackRequest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {}

    @Override
    public void processIOException(IOExceptionEvent exceptionEvent) {}

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {}

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {}
}
