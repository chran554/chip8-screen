package se.cha.chip8.screen;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class UdpPacketMessageListener implements Runnable {

    private static final int BYTE_BUFFER_SIZE = 1024; // 1kb read buffer

    int port;
    byte[] receiveData;
    DatagramSocket udpListeningSocket;
    UDPPacketDataProcessor processor;
    boolean continueListen = true;

    /**
     * In IPv4, any address between 224.0.0.0 to 239.255.255.255 can be used as a multicast address.
     */
    public UdpPacketMessageListener(UDPPacketDataProcessor listener, int listenerPort) {
        port = listenerPort;
        processor = listener;
        receiveData = new byte[BYTE_BUFFER_SIZE];
        try {
            udpListeningSocket = new DatagramSocket(port);
        } catch (IOException e) {
            System.err.println("Listen socket setup: " + port);
            e.printStackTrace();
        }
    }

    public void stop() {
        continueListen = false;
        //udpListeningSocket.disconnect();
        udpListeningSocket.close();
    }

    @Override
    public void run() {
        while (continueListen) {
            final DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                udpListeningSocket.receive(receivedPacket);
                final byte[] data = Arrays.copyOfRange(receivedPacket.getData(), receivedPacket.getOffset(), receivedPacket.getLength());
                // System.out.println("Received packet of length " + data.length + " bytes: '" + new String(data, 0, data.length, StandardCharsets.UTF_8) + "'");
                // System.out.println("Base64 data:      " + Base64.getEncoder().encodeToString(data));
                processor.onPacketReceived(data);
            } catch (IOException e) {
                if (continueListen) {
                    System.out.println("UDP socket listener caught an exception during receive: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        System.out.println("Ending UDP listener thread.");
    }

    public interface UDPPacketDataProcessor {
        void onPacketReceived(byte[] data);
    }
}

