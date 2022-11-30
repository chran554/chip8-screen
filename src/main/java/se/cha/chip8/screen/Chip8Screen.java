package se.cha.chip8.screen;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Chip8Screen {

    public static void main(String[] args) throws UnknownHostException {
        //final String multicastAddress = "230.0.0.0";
        final String multicastAddress = "224.0.0.8";
        final int multicastPort = 9999;

        System.out.println("Currently running CHIP-8 screen pad on IP: " + InetAddress.getLocalHost());
        System.out.println("Listening CHIP-8 screen broadcast on multicast address: " + multicastAddress + ":" + multicastPort + "...");

        BeepGenerator.startBeepGenerator();
        final UDPDataProcessor renderMessageProcessor = new UDPDataProcessor();
        final UDPMulticastMessageListener dataListener = new UDPMulticastMessageListener(renderMessageProcessor, multicastAddress, multicastPort);

        final Thread messageThread = new Thread(dataListener);
        messageThread.start();
    }

}
