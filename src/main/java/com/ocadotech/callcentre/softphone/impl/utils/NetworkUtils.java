package com.ocadotech.callcentre.softphone.impl.utils;

public class NetworkUtils {

    public static String localHostAddress() {
        // TODO: Extract ip address - InetAddress.getLocalHost().getHostAddress() is not enough when e.g. VirtualBox is installed on that host
        return "192.168.0.101";
    }
}
