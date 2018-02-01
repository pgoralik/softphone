package com.ocadotech.callcentre.softphone;

public class Main {

    static final String ASTERISK_HOST = "192.168.0.100";
    static final String LOCAL_HOST_ADDRESS = "192.168.0.104";

    public static void main(String[] args) throws Exception {
        Softphone advisorSoftphone = new SoftphoneBuilder("6002", ASTERISK_HOST)
                .withLocalHostAddress(LOCAL_HOST_ADDRESS)
                .build();
        advisorSoftphone.whenCalled(Softphone::answer);

//        Softphone customerSoftphone = new SoftphoneBuilder("6001", ASTERISK_HOST)
//                .withLocalHostAddress(LOCAL_HOST_ADDRESS)
//                .build();
//        customerSoftphone.call("200");

    }
}
