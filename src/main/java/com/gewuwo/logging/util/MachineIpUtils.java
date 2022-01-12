package com.gewuwo.logging.util;


import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * 机器ip
 *
 * @author jishan.guo
 * @version 1.0
 * @since 2022/1/5 10:39 上午
 */
public class MachineIpUtils {

    private static volatile String ip;


    /**
     * 获取ip
     *
     * @return ip
     */
    public static String getIp() {
        if (null != ip) {
            return ip;
        } else {
            Enumeration<NetworkInterface> netInterfaces;
            try {
                netInterfaces = NetworkInterface.getNetworkInterfaces();
            } catch (SocketException var6) {
                throw new RuntimeException(var6);
            }

            String localIpAddress = null;

            while (netInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = netInterfaces.nextElement();
                Enumeration<InetAddress> ipAddresses = netInterface.getInetAddresses();

                while (ipAddresses.hasMoreElements()) {
                    InetAddress ipAddress = ipAddresses.nextElement();
                    if (isPublicIpAddress(ipAddress)) {
                        String publicIpAddress = ipAddress.getHostAddress();
                        ip = publicIpAddress;
                        return publicIpAddress;
                    }

                    if (isLocalIpAddress(ipAddress)) {
                        localIpAddress = ipAddress.getHostAddress();
                    }
                }
            }

            ip = localIpAddress;
            return localIpAddress;
        }
    }

    private static boolean isPublicIpAddress(InetAddress ipAddress) {
        return !ipAddress.isSiteLocalAddress() && !ipAddress.isLoopbackAddress() && notV6IpAddress(ipAddress);
    }

    private static boolean isLocalIpAddress(InetAddress ipAddress) {
        return ipAddress.isSiteLocalAddress() && !ipAddress.isLoopbackAddress() && notV6IpAddress(ipAddress);
    }

    private static boolean notV6IpAddress(InetAddress ipAddress) {
        return !ipAddress.getHostAddress().contains(":");
    }

    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException var1) {
            throw new RuntimeException(var1);
        }
    }
}
