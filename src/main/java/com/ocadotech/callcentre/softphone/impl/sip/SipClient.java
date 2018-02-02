package com.ocadotech.callcentre.softphone.impl.sip;

import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;

import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.io.IOException;
import java.net.ServerSocket;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class SipClient implements SipListener {

    private String user;
    private String host;
    private String localHostAddress;
    private int port;

    private SipStack sipStack;
    private SipProvider sipProviderUdp;

    private MessageFactory messageFactory;
    private HeaderFactory headerFactory;
    private AddressFactory addressFactory;

    private Dialog currentDialog;

    private AtomicLong cseqGenerator = new AtomicLong(1);
    private AtomicLong cseqGeneratorRegister = new AtomicLong(1);

    public SipClient(String user, String host, String localHostAddress) {
        this.user = user;
        this.host = host;
        this.localHostAddress = localHostAddress;
        this.port = randomPort();
        System.out.println("SIP CLIENT PORT " + this.port);

        try {
            SipFactory sipFactory = SipFactory.getInstance();
            Properties properties = new Properties();
            properties.setProperty("javax.sip.STACK_NAME", user + "_SOFTPHONE");
            sipStack = sipFactory.createSipStack(properties);

            ListeningPoint udp = sipStack.createListeningPoint(localHostAddress, this.port, "udp");
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

    private int randomPort() {
        try(ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void unregisterAllBindings() {
        try {
            SipURI sipURI = addressFactory.createSipURI(user, host);
            Address toAddress = addressFactory.createAddress(sipURI);
            CallIdHeader newCallId = sipProviderUdp.getNewCallId();
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(cseqGeneratorRegister.getAndIncrement(), Request.REGISTER);
            FromHeader fromHeader = headerFactory.createFromHeader(toAddress, null);
            ToHeader toHeader = headerFactory.createToHeader(toAddress, null);
            Request request = messageFactory.createRequest(sipURI, Request.REGISTER, newCallId, cSeqHeader,
                    fromHeader, toHeader, getViaHeaders(), getMaxForwardsHeader());
            SipURI contactAddress = addressFactory.createSipURI(user, localHostAddress);
            contactAddress.setPort(this.port);
            request.addHeader(headerFactory.createExpiresHeader(0));
            request.addHeader(headerFactory.createContactHeader());
            System.out.println("Sending UNREGISTER");
            System.out.println(request);
            sipProviderUdp.sendRequest(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void register() {
        try {
            SipURI sipURI = addressFactory.createSipURI(user, host);
            Address toAddress = addressFactory.createAddress(sipURI);
            CallIdHeader newCallId = sipProviderUdp.getNewCallId();
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(cseqGeneratorRegister.getAndIncrement(), Request.REGISTER);
            FromHeader fromHeader = headerFactory.createFromHeader(toAddress, null);
            ToHeader toHeader = headerFactory.createToHeader(toAddress, null);
            Request request = messageFactory.createRequest(sipURI, Request.REGISTER, newCallId, cSeqHeader,
                    fromHeader, toHeader, getViaHeaders(), getMaxForwardsHeader());
            SipURI contactAddress = addressFactory.createSipURI(user, localHostAddress);
            contactAddress.setPort(this.port);
            ContactHeader contactHeader = headerFactory.createContactHeader(addressFactory.createAddress(contactAddress));
            request.addHeader(contactHeader);
            request.addHeader(headerFactory.createExpiresHeader(10));
            System.out.println("Sending REGISTER");
            System.out.println(request);
            sipProviderUdp.sendRequest(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MaxForwardsHeader getMaxForwardsHeader() throws InvalidArgumentException {
        return headerFactory.createMaxForwardsHeader(70);
    }

    public void initDialog(String destUser) {
        try {
            SipURI sipURI = addressFactory.createSipURI(user, host);
            CallIdHeader newCallId = sipProviderUdp.getNewCallId();
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.INVITE);
            Address fromAddress = addressFactory.createAddress(sipURI);
            FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, UUID.randomUUID().toString());
            SipURI toSipURI = addressFactory.createSipURI(destUser, host);
            Address toAddress = addressFactory.createAddress(toSipURI);
            ToHeader toHeader = headerFactory.createToHeader(toAddress, null);
            Request request = messageFactory.createRequest(toSipURI, Request.INVITE, newCallId, cSeqHeader, fromHeader, toHeader, getViaHeaders(), getMaxForwardsHeader());
            ContactHeader contactHeader = headerFactory.createContactHeader(fromAddress);
            request.addHeader(contactHeader);
            ClientTransaction clientTransaction = sipProviderUdp.getNewClientTransaction(request);
            clientTransaction.sendRequest();
            currentDialog = clientTransaction.getDialog();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<ViaHeader> getViaHeaders() throws ParseException, InvalidArgumentException {
        List<ViaHeader> viaHeaders = new ArrayList<>();
        viaHeaders.add(headerFactory.createViaHeader(localHostAddress, this.port, "udp", null));
        return viaHeaders;
    }

    public void close() {
        sipStack.stop();
    }

    @Override
    public void processRequest(RequestEvent requestEvent) {
        SIPRequest request = (SIPRequest) requestEvent.getRequest();
        System.out.println("processRequest");
        System.out.println(request);

        // TODO PG: Handle requests

        if(request.getMethod().equals(Request.BYE)) {
            SIPResponse response = request.createResponse(Response.OK);
            try {
                System.out.println("Sending response to BYE");
                System.out.println(response);
                requestEvent.getServerTransaction().sendResponse(response);
            } catch (SipException | InvalidArgumentException e) {
                e.printStackTrace();
            }
        }

    }

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

    public void info(Object content) {
        if(currentDialog == null) {
            throw new IllegalStateException("There is no dialog. Call initDialog method first.");
        }

        try {
            Request request = currentDialog.createRequest(Request.INFO);
            request.setContent(content, headerFactory.createContentTypeHeader("application", "dtmf"));
            request.setHeader(headerFactory.createCSeqHeader(cseqGenerator.getAndIncrement(), Request.INFO));
            sipProviderUdp.sendRequest(request);
        }  catch(Exception e) {
            e.printStackTrace();
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
