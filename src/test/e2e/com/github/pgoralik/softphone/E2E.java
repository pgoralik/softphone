package com.github.pgoralik.softphone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
    static final String ASTERISK_HOST = "192.168.56.101";
    static final String LOCAL_HOST_ADDRESS = "192.168.56.1";

    volatile boolean callFlowEndedSuccessfully;

    Softphone caller;
    Softphone callee;

    @BeforeEach
    void setUp() {
        callFlowEndedSuccessfully = false;
    }

    @AfterEach
    void tearDown() {
        caller.close();
        callee.close();
    }

    @Test
    void callInitiatedAndEndedByTheSamePeer() {
        caller = new SoftphoneBuilder("6001", ASTERISK_HOST)
                .withLocalHostAddress(LOCAL_HOST_ADDRESS)
                .withStatusListener(new StatusHandler() {
                    @Override
                    public void onCallAnswered(Softphone thisPhone) {
                        thisPhone.waitMiliseconds(5000);
//                        thisPhone.hangup();
                        thisPhone.waitMiliseconds(100);
                    }
                })
                .build();

        callee = new SoftphoneBuilder("6002", ASTERISK_HOST)
                .withLocalHostAddress(LOCAL_HOST_ADDRESS)
                .withStatusListener(new StatusHandler() {
                    @Override
                    public void onRinging(Softphone thisPhone) {
                        thisPhone.waitMiliseconds(100);
                        thisPhone.answer();
                        thisPhone.waitMiliseconds(200);
                    }

                    @Override
                    public void onCallEnded(Softphone softphone) {
                        callFlowEndedSuccessfully = true;
                    }
                })
                .build();

        caller.waitMiliseconds(1000); // TODO: Wait until phones got registered - Improvement: add onRegistered() to StatusHandler?
        caller.call("6002");

        await().atMost(60, SECONDS).untilAsserted(() -> assertTrue(callFlowEndedSuccessfully));
    }

    @Test
    void callInitiatedByFirstPeerAndEndedBySecondPeer() {
        caller = new SoftphoneBuilder("6001", ASTERISK_HOST)
                .withLocalHostAddress(LOCAL_HOST_ADDRESS)
                .withStatusListener(new StatusHandler() {
                    @Override
                    public void onCallEnded(Softphone thisPhone) {
                        callFlowEndedSuccessfully = true;
                    }
                })
                .build();

        callee = new SoftphoneBuilder("6002", ASTERISK_HOST)
                .withLocalHostAddress(LOCAL_HOST_ADDRESS)
                .withStatusListener(new StatusHandler() {
                    @Override
                    public void onRinging(Softphone thisPhone) {
                        thisPhone.answer();
                        thisPhone.waitMiliseconds(500);
                        thisPhone.hangup();
                    }
                })
                .build();

        caller.waitMiliseconds(2000); // TODO: Wait until phones got registered - Improvement: add onRegistered() to StatusHandler?
        caller.call("6002");

        await().atMost(60, SECONDS).untilAsserted(() -> assertTrue(callFlowEndedSuccessfully));
    }

    @Test
    void sendDTMF() {
        fail("Not implemented yet");
    }
}
