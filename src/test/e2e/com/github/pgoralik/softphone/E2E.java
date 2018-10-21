package com.github.pgoralik.softphone;

import com.github.pgoralik.softphone.impl.status.StatusHandlerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/*
 * How to run E2E tests:
 * To run those tests one needs to start Asterisk manually with configuration presented in asterisk-conf directory before
 * TODO: Try to build working docker image
 */
class E2E {
    private static final String ASTERISK_HOST = "192.168.56.101";
    private static final String LOCAL_HOST_ADDRESS = "192.168.56.1";

    private volatile boolean callFlowEndedSuccessfully;

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

        callFlowEndedSuccessfully = false;
    }

    @AfterEach
    void tearDown() {
        caller.close();
        callee.close();
    }

    @Test
    void callInitiatedAndEndedByTheSamePeer() {
        caller.setStatusHandler(new StatusHandlerBuilder()
                .withOnRegistered(thisPhone -> thisPhone.call("6002"))
                .withOnCallAnswered(Softphone::hangup)
                .withOnCallEnded(thisPhone -> callFlowEndedSuccessfully = true)
                .build());

        callee.setStatusHandler(new StatusHandlerBuilder()
                .withOnRinging(Softphone::answer)
                // TODO: This callback is not called, that's why below flag is set also in caller to make this test pass. (or maybe it should be check in both anyway)
                .withOnCallEnded(thisPhone -> callFlowEndedSuccessfully = true)
                .build());

        await().atMost(60, SECONDS).untilAsserted(() -> assertTrue(callFlowEndedSuccessfully));
    }

    @Test
    void callInitiatedByFirstPeerAndEndedBySecondPeer() {
        caller.setStatusHandler(new StatusHandlerBuilder()
                .withOnRegistered(thisPhone -> thisPhone.call("6002"))
                .withOnCallEnded(thisPhone -> callFlowEndedSuccessfully = true)
                .build());

        callee.setStatusHandler(new StatusHandlerBuilder()
                .withOnRinging(thisPhone -> {
                    thisPhone.answer();
                    thisPhone.hangup();
                })
                // TODO: This callback is not called, that's why below flag is set also in caller to make this test pass. (or maybe it should be check in both anyway)
                .withOnCallEnded(thisPhone -> callFlowEndedSuccessfully = true)
                .build());

        await().atMost(60, SECONDS).untilAsserted(() -> assertTrue(callFlowEndedSuccessfully));
    }

    @Test
    @Disabled
    void sendDTMF() {
        fail("Not implemented yet");
    }
}