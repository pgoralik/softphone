package com.ocadotech.callcentre.softphone;

public class Main {

    static final String ASTERISK_HOST = "192.168.99.100";
    static final String LOCAL_HOST_ADDRESS = "192.168.99.1";

    public static void main(String[] args) throws Exception {
        Softphone advisorSoftphone = new SoftphoneBuilder("112233445577", ASTERISK_HOST)
                .withLocalHostAddress(LOCAL_HOST_ADDRESS)
                .build();

        advisorSoftphone.call("*#*");
        advisorSoftphone.waitMiliseconds(2000);

        advisorSoftphone.call("#6100");
        advisorSoftphone.waitMiliseconds(1500);
        advisorSoftphone.pushOnDialpad("01234"); // TODO PG: Why first digit is ignored?

        advisorSoftphone.waitMiliseconds(5000);
        advisorSoftphone.call("6101");

//        Softphone customerSoftphone = new SoftphoneBuilder("zoiper", ASTERISK_HOST)
//                .withLocalHostAddress(LOCAL_HOST_ADDRESS)
//                .build();
//        customerSoftphone.call("3000");

    }
}
