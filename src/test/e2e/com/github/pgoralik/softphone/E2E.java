package com.github.pgoralik.softphone;

import com.github.pgoralik.softphone.impl.status.StatusHandlerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/*
 * How to run E2E tests:
 * To run those tests one needs to start Asterisk manually with configuration presented in asterisk-conf directory before
 * TODO: Try to build working docker image
 */
class E2E {
    private static final String ASTERISK_HOST = "192.168.56.101";
    private static final String LOCAL_HOST_ADDRESS = "192.168.56.1";

    private Softphone caller;
    private Softphone callee;

    @BeforeEach
    void setUp() {
        caller = new SoftphoneBuilder("6001", ASTERISK_HOST)
                .withLocalHostAddress(LOCAL_HOST_ADDRESS)
                .withLoggingSIPMessagesEnabled()
                .build();

        callee = new SoftphoneBuilder("6002", ASTERISK_HOST)
                .withLocalHostAddress(LOCAL_HOST_ADDRESS)
                .withLoggingSIPMessagesEnabled()
                .build();

        caller.register();
        callee.register();
    }

    @AfterEach
    void tearDown() {
        caller.close();
        callee.close();
    }

    @Test
    void callInitiatedAndEndedByTheSamePeer() {
        AtomicBoolean callerEndedSuccessfully = new AtomicBoolean(false);
        AtomicBoolean calleeEndedSuccessfully = new AtomicBoolean(false);

        caller.setStatusHandler(new StatusHandlerBuilder()
                .withOnRegistered(thisPhone -> thisPhone.call("6002"))
                .withOnCallAnswered(Softphone::hangup)
                .withOnCallEnded(thisPhone -> callerEndedSuccessfully.set(true))
                .build());

        callee.setStatusHandler(new StatusHandlerBuilder()
                .withOnRinging(Softphone::answer)
                .withOnCallEnded(thisPhone -> calleeEndedSuccessfully.set(true))
                .build());

        await("Caller's call ended successfully").atMost(10, SECONDS).untilTrue(callerEndedSuccessfully);
//        await("Callee's call ended successfully").atMost(10, SECONDS).untilTrue(calleeEndedSuccessfully);
    }

    @Test
    void callInitiatedByFirstPeerAndEndedBySecondPeer() {
        AtomicBoolean callerEndedSuccessfully = new AtomicBoolean(false);
        AtomicBoolean calleeEndedSuccessfully = new AtomicBoolean(false);

        caller.setStatusHandler(new StatusHandlerBuilder()
                .withOnRegistered(thisPhone -> thisPhone.call("6002"))
                .withOnCallEnded(thisPhone -> callerEndedSuccessfully.set(true))
                .build());

        callee.setStatusHandler(new StatusHandlerBuilder()
                .withOnRinging(thisPhone -> {
                    thisPhone.answer();
                    thisPhone.hangup();
                })
                .withOnCallEnded(thisPhone -> calleeEndedSuccessfully.set(true))
                .build());

//        await("Caller's call ended successfully").atMost(10, SECONDS).untilTrue(callerEndedSuccessfully);
        await("Callee's call ended successfully").atMost(10, SECONDS).untilTrue(calleeEndedSuccessfully);
    }

    @Test
    void sendDTMF() {
        AtomicBoolean dtmfWasSentSuccessfully = new AtomicBoolean(false);

        caller.setStatusHandler(new StatusHandlerBuilder()
                .withOnRegistered(thisPhone -> thisPhone.call("67"))
                .withOnCallAnswered(thisPhone -> {
                    thisPhone.pushKeysOnDialpad("123#");
                })
                .build());

        callee.setStatusHandler(new StatusHandlerBuilder()
                .withOnRinging(thisPhone -> {
                    thisPhone.reject();
                    dtmfWasSentSuccessfully.set(true);
                })
                .build());

        await().atMost(10, SECONDS).untilTrue(dtmfWasSentSuccessfully);
    }
}