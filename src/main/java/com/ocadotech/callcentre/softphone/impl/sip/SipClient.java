package com.ocadotech.callcentre.softphone.impl.sip;

import com.ocadotech.callcentre.softphone.Softphone;
import com.ocadotech.callcentre.softphone.StatusHandler;
import com.ocadotech.callcentre.softphone.impl.status.Status;
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
import java.util.*;

import static com.ocadotech.callcentre.softphone.impl.sip.HeaderUtils.getMaxForwardsHeader;

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
    private Status status = Status.READY;
    private StatusHandler statusHandler;
    private Softphone softphone;

    private long cseq = 1;

    private Registrator registrator;
    private SIPResponse preparedOkResponseForInvite;
    private SIPResponse preparedBusyResponseForInvite;

    public SipClient(String user, String host, String localHostAddress, StatusHandler statusHandler, Softphone softphone) {
        this.user = user;
        this.host = host;
        this.localHostAddress = localHostAddress;
        this.port = randomPort();
        System.out.println("SIP CLIENT PORT " + this.port);
        this.statusHandler = statusHandler;
        this.softphone = softphone;

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

            registrator = new Registrator(headerFactory, addressFactory, messageFactory, sipProviderUdp);

            sipStack.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        registrator.unregister();
        sipStack.stop();
    }

    private int randomPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void answerOkToInvite() {
        if (status != Status.RINGING) {
            throw new IllegalStateException("Can not answer because phone is not ringing");
        }

        System.out.println("Sending response to INVITE");
        System.out.println(preparedOkResponseForInvite);
        try {
            sipProviderUdp.sendResponse(preparedOkResponseForInvite);
        } catch (SipException e) {
            e.printStackTrace();
        }
    }

    public void answerBusyToInvite() {
        if (status != Status.RINGING) {
            throw new IllegalStateException("Can not reject because phone is not ringing");
        }

        System.out.println("Sending response to INVITE");
        System.out.println(preparedBusyResponseForInvite);
    }

    public void bye() {
        try {
            Request byeRequest = currentDialog.createRequest(Request.BYE);
            sipProviderUdp.sendRequest(byeRequest);
        } catch (SipException e) {
            throw new RuntimeException(e);
        }
    }

    public void register() {
        registrator.register(user, host, localHostAddress, port);
    }

    public void initDialog(String destUser) {
        try {
            SipURI sipURI = addressFactory.createSipURI(user, host);
            CallIdHeader newCallId = sipProviderUdp.getNewCallId();
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(cseq, Request.INVITE);
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

    @Override
    public void processRequest(RequestEvent requestEvent) {
        SIPRequest request = (SIPRequest) requestEvent.getRequest();
        if (!request.getCSeq().getMethod().equals(Request.OPTIONS)) {
            System.out.println("processRequest");
            System.out.println(request);
        }

        try {
            switch (request.getMethod()) {
                case Request.OPTIONS:
                    SIPResponse optionsResponse = request.createResponse(Response.OK);
                    // This are headers copied from Zoiper response to that request ------------------------------------------------------------------------------
                    optionsResponse.addHeader(headerFactory.createAcceptHeader("application", "sdp"));
                    optionsResponse.addHeader(headerFactory.createAllowHeader("INVITE, ACK, CANCEL, BYE, NOTIFY, REFER, MESSAGE, OPTIONS, INFO, SUBSCRIBE"));
                    optionsResponse.addHeader(headerFactory.createSupportedHeader("replaces, norefersub, extended-refer, timer, outbound, path, X-cisco-serviceuri"));
                    optionsResponse.addHeader(headerFactory.createAcceptLanguageHeader(Locale.ENGLISH));
                    optionsResponse.addHeader(headerFactory.createAllowEventsHeader("presence, kpml, talk"));
                    // -------------------------------------------------------------------------------------------------------------------------------------------
                    sipProviderUdp.sendResponse(optionsResponse);
                    break;
                case Request.INVITE:
                    if (status != Status.ON_CALL) {
                        prepareBusyResponseForInvite(request);
                        prepareOkResponseForInvite(request);
                        SIPResponse inviteResponse = request.createResponse(Response.RINGING);
                        System.out.println("Sending response to INVITE");
                        System.out.println(inviteResponse);
                        currentDialog = requestEvent.getDialog();
                        if (requestEvent.getServerTransaction() != null) {
                            requestEvent.getServerTransaction().sendResponse(inviteResponse);
                        } else {
                            sipProviderUdp.sendResponse(inviteResponse);
                        }
                        status = Status.RINGING;
                        statusHandler.onRinging(softphone);
                    }
                    break;
                case Request.BYE:
                    SIPResponse byeResponse = request.createResponse(Response.OK);
                    System.out.println("Sending response to BYE");
                    System.out.println(byeResponse);
                    requestEvent.getServerTransaction().sendResponse(byeResponse);
                    status = Status.READY;
                    statusHandler.onCallEnded(softphone);
                    break;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    private void prepareOkResponseForInvite(SIPRequest request) throws InvalidArgumentException, ParseException {
        preparedOkResponseForInvite = request.createResponse(Response.OK);
        // TODO PG: It is suggested that it should be NTP time format
        long sessionIdAndVersion = System.currentTimeMillis();
        String sdpBody =
                "v=0\n" +
                        // TODO PG: Think about putting in here user's login from host instead of '-'
                        "o= - " + sessionIdAndVersion + " " + sessionIdAndVersion + " IN IP4 + " + localHostAddress + "\n" +
                        "s=" + user + " Session\n" +
                        "c=IN IP4 " + localHostAddress + "\n" +
                        "t=0 0\n" +
                        "m=audio 20008 RTP/AVP 0 101\n" +
                        "a=rtpmap:0 PCMU/8000\n" +
                        "a=rtpmap:101 telephone-event/8000\n" +
                        "a=fmtp:101 0-16\n" +
                        "a=ptime:20\n" +
                        "a=maxptime:150\n" +
                        "a=sendrecv\n";

        // --- supported header copied from X-Lite
        preparedOkResponseForInvite.addHeader(headerFactory.createSupportedHeader("replaces, norefersub, extended-refer, timer, outbound, path, X-cisco-serviceuri"));
        preparedOkResponseForInvite.addHeader(headerFactory.createContentLengthHeader(sdpBody.length()));
        preparedOkResponseForInvite.setContent(sdpBody, headerFactory.createContentTypeHeader("application", "sdp"));
    }

    private void prepareBusyResponseForInvite(SIPRequest request) {
        preparedBusyResponseForInvite = request.createResponse(Response.BUSY_HERE);
    }

    @Override
    public void processResponse(ResponseEvent responseEvent) {
        SIPResponse response = (SIPResponse) responseEvent.getResponse();

        if (!response.getCSeq().getMethod().equals(Request.REGISTER)) {
            System.out.println("processResponse " + response.getStatusCode());
            System.out.println(response);
        }

        Dialog dialog = responseEvent.getDialog();

        if (response.getCSeq().getMethod().equals(Request.REGISTER)) {
            int expiresSetByServer = response.getExpires().getExpires();
            registrator.scheduleReRegistration(expiresSetByServer);
            return;
        }

        if (dialog != null && response.isFinalResponse() && response.getStatusCode() == Response.OK) {
            try {
                // TODO: Maybe there is a better way to create ACK request when we have a dialog
                Address remotePartyAddress = responseEvent.getDialog().getRemoteParty();
                HeaderFactory headerFactory = SipFactory.getInstance().createHeaderFactory();
                CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.ACK);
                Request ackRequest = SipFactory.getInstance().createMessageFactory().createRequest(remotePartyAddress.getURI(), Request.ACK, dialog.getCallId(), cSeqHeader,
                        headerFactory.createFromHeader(dialog.getLocalParty(), dialog.getLocalTag()),
                        headerFactory.createToHeader(remotePartyAddress, dialog.getRemoteTag()),
                        response.getViaHeaders(), getMaxForwardsHeader());
                System.out.println("Sending ACK");
                System.out.println(ackRequest);
                dialog.sendAck(ackRequest);

                if (response.getCSeq().getMethod().equals(Request.INVITE)) {
                    status = Status.ON_CALL;
                    statusHandler.onCallAnswered(softphone);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void info(Object content) {
        if (currentDialog == null) {
            throw new IllegalStateException("There is no dialog. Call initDialog method first.");
        }

        try {
            Request request = currentDialog.createRequest(Request.INFO);
            request.setContent(content, headerFactory.createContentTypeHeader("application", "dtmf"));
            request.setHeader(headerFactory.createCSeqHeader(cseq++, Request.INFO));
            sipProviderUdp.sendRequest(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {
    }

    @Override
    public void processIOException(IOExceptionEvent exceptionEvent) {
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
    }
}
