package com.ocadotech.callcentre.softphone;

import java.util.concurrent.TimeUnit;

public class Main {

    // DOCKER LOCAL
    static final String ASTERISK_HOST = "192.168.99.100";
    static final String LOCAL_HOST_ADDRESS = "192.168.99.1";
//    static final String ASTERISK_HOST = "10.118.87.155";
//    static final String LOCAL_HOST_ADDRESS = "10.244.252.106";

    public static void main(String[] args) throws Exception {
        Softphone advisorSoftphone = new SoftphoneBuilder("112233445566", ASTERISK_HOST)
                .withLocalHostAddress(LOCAL_HOST_ADDRESS)
                .build();

//        advisorSoftphone.call("*#*");
//        advisorSoftphone.waitMiliseconds(4000);
//
//        advisorSoftphone.call("#6101");
//        advisorSoftphone.waitMiliseconds(3000);
//        advisorSoftphone.pushOnDialpad("01234"); // TODO PG: Why first digit is ignored?

        Softphone customerSoftphone = new SoftphoneBuilder("zoiper", ASTERISK_HOST)
                .withLocalHostAddress(LOCAL_HOST_ADDRESS)
                .build();
        customerSoftphone.call("3000");

        TimeUnit.SECONDS.sleep(17);
        advisorSoftphone.answer();

        TimeUnit.SECONDS.sleep(20);
        advisorSoftphone.hangup();
//        customerSoftphone.hangup();
//        customerSoftphone.close();
//        advisorSoftphone.close();
    }
}
