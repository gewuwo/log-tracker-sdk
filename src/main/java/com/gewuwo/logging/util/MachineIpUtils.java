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

    private static volatile String ipAddr;

    public static String getHostIp() {

        String sIP = "";
        InetAddress ip = null;
        try {
            boolean bFindIP = false;
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                if (bFindIP) {
                    break;
                }

                NetworkInterface ni = netInterfaces.nextElement();
                Enumeration<InetAddress> ips = ni.getInetAddresses();
                while (ips.hasMoreElements()) {
                    ip = ips.nextElement();
                    if (!ip.isLoopbackAddress()
                        && ip.getHostAddress().matches("(\\d{1,3}\\.){3}\\d{1,3}")) {
                        bFindIP = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (null != ip) {
            sIP = ip.getHostAddress();
        }
        return sIP;
    }

    public static String getIp() {
        if (null != ipAddr) {
            return ipAddr;
        } else {
            Enumeration netInterfaces;
            try {
                netInterfaces = NetworkInterface.getNetworkInterfaces();
            } catch (SocketException var6) {
                throw new RuntimeException(var6);
            }

            String localIpAddress = null;

            while (netInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) netInterfaces.nextElement();
                Enumeration ipAddresses = netInterface.getInetAddresses();

                while (ipAddresses.hasMoreElements()) {
                    InetAddress ipAddress = (InetAddress) ipAddresses.nextElement();
                    if (isPublicIpAddress(ipAddress)) {
                        String publicIpAddress = ipAddress.getHostAddress();
                        ipAddr = publicIpAddress;
                        return publicIpAddress;
                    }

                    if (isLocalIpAddress(ipAddress)) {
                        localIpAddress = ipAddress.getHostAddress();
                    }
                }
            }

            ipAddr = localIpAddress;
            return localIpAddress;
        }
    }

    private static boolean isPublicIpAddress(InetAddress ipAddress) {
        return !ipAddress.isSiteLocalAddress() && !ipAddress.isLoopbackAddress() && !isV6IpAddress(ipAddress);
    }

    private static boolean isLocalIpAddress(InetAddress ipAddress) {
        return ipAddress.isSiteLocalAddress() && !ipAddress.isLoopbackAddress() && !isV6IpAddress(ipAddress);
    }

    private static boolean isV6IpAddress(InetAddress ipAddress) {
        return ipAddress.getHostAddress().contains(":");
    }

    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException var1) {
            throw new RuntimeException(var1);
        }
    }
}
