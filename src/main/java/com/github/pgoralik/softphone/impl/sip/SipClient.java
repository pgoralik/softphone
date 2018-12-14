package com.github.pgoralik.softphone.impl.sip;

import com.github.pgoralik.softphone.Softphone;
import com.github.pgoralik.softphone.impl.sdp.SdpUtil;
import com.github.pgoralik.softphone.impl.status.NoOpStatusHandler;
import com.github.pgoralik.softphone.impl.status.Status;
import com.github.pgoralik.softphone.impl.status.StatusHandler;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import org.apache.log4j.Logger;

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
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

import static com.github.pgoralik.softphone.impl.sip.HeaderUtils.getMaxForwardsHeader;
import static com.github.pgoralik.softphone.impl.sip.HeaderUtils.getViaHeaders;

public class SipClient implements SipListener {

    private static final Logger LOG = Logger.getLogger(SipClient.class);

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
    private StatusHandler statusHandler = new NoOpStatusHandler();
    private Softphone softphone;

    private long cseq = 1;

    private Registrator registrator;
    private ServerTransaction serverTransaction;
    private ClientTransaction clientTransaction;

    private boolean isLogSIPMessagesEnabled;

    private void log(Object message) {
        LOG.info("[" + user + "@" + host + "] " + message);
    }

    private void logSIPMessage(Object message) {
        if (isLogSIPMessagesEnabled) {
            LOG.info("[" + user + "@" + host + "] " + message);
        }
    }

    public SipClient(String user, String host, String localHostAddress, boolean isLogSIPMessagesEnabled, boolean isDebugEnabled, Softphone softphone) {
        this.user = user;
        this.host = host;
        this.localHostAddress = localHostAddress;
        this.port = randomPort();
        this.softphone = softphone;

        this.isLogSIPMessagesEnabled = isLogSIPMessagesEnabled;

        try {
            SipFactory sipFactory = SipFactory.getInstance();
            Properties properties = new Properties();
            properties.setProperty("javax.sip.STACK_NAME", user + "_SOFTPHONE");
            if (isDebugEnabled) {
                properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");
            }

            sipStack = sipFactory.createSipStack(properties);

            ListeningPoint udp = sipStack.createListeningPoint(localHostAddress, this.port, "udp");
            sipProviderUdp = sipStack.createSipProvider(udp);
            sipProviderUdp.addSipListener(this);
            addressFactory = SipFactory.getInstance().createAddressFactory();
            messageFactory = SipFactory.getInstance().createMessageFactory();
            headerFactory = SipFactory.getInstance().createHeaderFactory();

            registrator = new Registrator(sipProviderUdp, isLogSIPMessagesEnabled);

            sipStack.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setStatusHandler(StatusHandler statusHandler) {
        this.statusHandler = statusHandler;
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

        Request originalRequest = serverTransaction.getRequest();
        try {
            Response response = messageFactory.createResponse(Response.OK, originalRequest);
            ToHeader responseToHeader = (ToHeader) response.getHeader("To");
            responseToHeader.setTag("454326");
            Address contactAddress = addressFactory.createAddress("sip:" + localHostAddress + ":" + port);
            ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
            response.addHeader(contactHeader);

            String sdpBody = SdpUtil.createSdpBody(user, localHostAddress);

            // --- supported header copied from X-Lite
            response.addHeader(headerFactory.createSupportedHeader("replaces, norefersub, extended-refer, timer, outbound, path, X-cisco-serviceuri"));
            response.addHeader(headerFactory.createContentLengthHeader(sdpBody.length()));
            response.setContent(sdpBody, headerFactory.createContentTypeHeader("application", "sdp"));

            logSIPMessage("Sent:\n" + response);
            serverTransaction.sendResponse(response);
            currentDialog = serverTransaction.getDialog();
        } catch (ParseException | InvalidArgumentException | SipException e) {
            e.printStackTrace();
        }
    }

    public void answerBusyToInvite() {
        if (status != Status.RINGING) {
            throw new IllegalStateException("Can not reject because phone is not ringing");
        }

        Request originalRequest = serverTransaction.getRequest();
        try {
            Response response = messageFactory.createResponse(Response.BUSY_HERE, originalRequest);
            ToHeader responseToHeader = (ToHeader) response.getHeader("To");
            responseToHeader.setTag("454326");

            logSIPMessage("Sent:\n" + response);
            serverTransaction.sendResponse(response);
            currentDialog = serverTransaction.getDialog();
        } catch (ParseException | InvalidArgumentException | SipException e) {
            e.printStackTrace();
        }
    }

    public void bye() {
        try {
            Request byeRequest = currentDialog.createRequest(Request.BYE);
            ClientTransaction clientTransaction = sipProviderUdp.getNewClientTransaction(byeRequest);
            logSIPMessage("Sent:\n" + byeRequest);
            currentDialog.sendRequest(clientTransaction);
        } catch (Exception e) {
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
            Request request = messageFactory.createRequest(toSipURI, Request.INVITE, newCallId, cSeqHeader, fromHeader, toHeader, getViaHeaders(localHostAddress, port), getMaxForwardsHeader());
            ContactHeader contactHeader = headerFactory.createContactHeader(fromAddress);
            request.addHeader(contactHeader);

            String sdpBody = SdpUtil.createSdpBody(user, localHostAddress);
            request.addHeader(headerFactory.createContentLengthHeader(sdpBody.length()));
            request.setContent(sdpBody, headerFactory.createContentTypeHeader("application", "sdp"));

            clientTransaction = sipProviderUdp.getNewClientTransaction(request);
            logSIPMessage("Sent:\n" + request);
            clientTransaction.sendRequest();
            currentDialog = clientTransaction.getDialog();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processRequest(RequestEvent requestEvent) {
        SIPRequest request = (SIPRequest) requestEvent.getRequest();

        if (isLogSIPMessagesEnabled) {
            logSIPMessage("Got request:\n" + request);
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
                    logSIPMessage("Sent:\n" + optionsResponse);
                    sipProviderUdp.sendResponse(optionsResponse);
                    break;
                case Request.INVITE:
                    if (status == Status.READY) {
                        serverTransaction = sipProviderUdp.getNewServerTransaction(request);
                        Response response = messageFactory.createResponse(Response.RINGING, request);
                        ToHeader responseToHeader = (ToHeader) response.getHeader("To");
                        responseToHeader.setTag("454326"); // TODO: Should it be a random tag? (it's reused, watch out)
                        Address contactAddress = addressFactory.createAddress("sip:" + localHostAddress + ":" + port);
                        ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
                        response.addHeader(contactHeader);
                        serverTransaction.sendResponse(response);
                        currentDialog = serverTransaction.getDialog();
                        logSIPMessage("Sent:\n" + response);
                        status = Status.RINGING;
                        statusHandler.onRinging(softphone);
                    }
                    break;
                case Request.BYE:
                    SIPResponse byeResponse = request.createResponse(Response.OK);
                    logSIPMessage("Sent:\n" + byeResponse);
                    ServerTransaction serverTransaction = requestEvent.getServerTransaction();
                    serverTransaction.sendResponse(byeResponse);
                    statusHandler.onCallEnded(softphone);
                    status = Status.READY;

                    break;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    @Override
    public void processResponse(ResponseEvent responseEvent) {
        SIPResponse response = (SIPResponse) responseEvent.getResponse();

        if (isLogSIPMessagesEnabled) {
            logSIPMessage("Got response:\n" + response);
        }

        if (response.getCSeq().getMethod().equals(Request.REGISTER)) {
            int expiresSetByServer = response.getExpires().getExpires();
            statusHandler.onRegistered(softphone);
            registrator.scheduleReRegistration(expiresSetByServer);
            return;
        }

        clientTransaction = responseEvent.getClientTransaction();
        currentDialog = clientTransaction.getDialog();

        if (response.isFinalResponse()) {
            try {
                if (response.getCSeq().getMethod().equals(Request.INVITE)) {
                    Request ackRequest = currentDialog.createAck(1);
                    Address contactAddress = addressFactory.createAddress("sip:" + localHostAddress + ":" + port);
                    ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
                    ackRequest.addHeader(contactHeader);
                    logSIPMessage("Sent:\n" + ackRequest);
                    currentDialog.sendAck(ackRequest);

                    status = Status.ON_CALL;
                    statusHandler.onCallAnswered(softphone);
                }

                if (response.getCSeq().getMethod().equals(Request.BYE)) {
                    // TODO PG: This is to inform the party which sends BYE, I don't know yet how to do it for the party who receive BYE
                    status = Status.READY;
                    statusHandler.onCallEnded(softphone);
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
            request.setHeader(headerFactory.createCSeqHeader(1000 + cseq++, Request.INFO));
            logSIPMessage("Sent:\n" + request);
            sipProviderUdp.sendRequest(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {
        log("processTimeout " + timeoutEvent);
    }

    @Override
    public void processIOException(IOExceptionEvent exceptionEvent) {
        log("processIOException " + exceptionEvent);
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
        log("processTransactionTerminated " + transactionTerminatedEvent);
    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        log("processDialogTerminated " + dialogTerminatedEvent);
    }
}