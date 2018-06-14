package com.ocadotech.callcentre.softphone;

public class Main {

    //    static final String ASTERISK_HOST = "192.168.1.4";
//    static final String LOCAL_HOST_ADDRESS = "192.168.1.2";
    static final String ASTERISK_HOST = "192.168.1.7";
    static final String LOCAL_HOST_ADDRESS = "192.168.1.5";

    public static void main(String[] args) throws Exception {
//        Softphone advisorSoftphone = new SoftphoneBuilder("112233445577", ASTERISK_HOST)
//                .withLocalHostAddress(LOCAL_HOST_ADDRESS)
//                .build();
//
//        advisorSoftphone.call("*#*");
//        advisorSoftphone.waitMiliseconds(2000);
//
//        advisorSoftphone.call("#6100");
//        advisorSoftphone.waitMiliseconds(1500);
//        advisorSoftphone.pushOnDialpad("01234"); // TODO PG: Why first digit is ignored?
//
//        advisorSoftphone.waitMiliseconds(5000);
//        advisorSoftphone.call("6101");

        Softphone customerSoftphone2 = new SoftphoneBuilder("6002", ASTERISK_HOST)
                .withLocalHostAddress(LOCAL_HOST_ADDRESS)
                .build();

        Softphone customerSoftphone = new SoftphoneBuilder("6001", ASTERISK_HOST)
                .withLocalHostAddress(LOCAL_HOST_ADDRESS)
                .build();

        customerSoftphone.waitMiliseconds(2000);
        customerSoftphone.call("6002");

        customerSoftphone2.waitMiliseconds(5000);
        customerSoftphone2.answer();
        customerSoftphone.waitMiliseconds(2000);
        customerSoftphone.hangup();

    }
}
