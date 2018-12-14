package com.github.pgoralik.softphone.impl.sdp;

public class SdpUtil {
    public static String createSdpBody(String user, String localHostAddress) {
        // TODO PG: It is suggested that it should be NTP time format
        long sessionIdAndVersion = System.currentTimeMillis();
        return
                "v=0\n" +
                        // TODO PG: Think about putting in here user's login from host instead of '-'
                        "o=- " + sessionIdAndVersion + " " + sessionIdAndVersion + " IN IP4 " + localHostAddress + "\n" +
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
    }
}
