package se.cha.chip8.screen;

import org.apache.commons.cli.*;

import java.awt.*;
import java.net.*;

public class Chip8Screen {

    public static void main(String[] args) throws UnknownHostException {
        final Configuration configuration = parseArguments(args);

        final ScreenFrame screenFrame = ScreenFrame.getOrCreateSingleton();
        screenFrame.initialize(configuration);

        System.out.println("Currently running CHIP-8 screen on IP:       " + InetAddress.getLocalHost().getHostAddress());
        System.out.println("Listening for CHIP-8 screen updates on port: " + configuration.getListenerPort());
        System.out.println();
        System.out.println("Using configuration: " + configuration);
        System.out.println();

        BeepGenerator.startBeepGenerator();
        final UdpDataProcessor renderMessageProcessor = new UdpDataProcessor();
        final UdpPacketMessageListener dataListener = new UdpPacketMessageListener(renderMessageProcessor, configuration.getListenerPort());

        final Thread messageThread = new Thread(dataListener);
        messageThread.start();
    }

    private static Configuration parseArguments(String[] args) {
        final Option lpOption = new Option("lp", "listener-port", true,
                "The listener port where the screen application listen for UDP packets with screen and sound updates." +
                        " Default, if not specified, is 9999.");
        final Option caOption = new Option("ca", "chip8-address", true,
                "The UDP address for the CHIP-8 application. The address should be an IPv4 address including port like \"127.0.0.1:9998\"." +
                        " This is the address and port where the CHIP-8 application listen for keypress status messages." +
                        " Default, if not specified, is \"localhost:9998\".");
        final Option cOption = new Option("c", "color", true,
                "The RGB hex color for the bright (lit) color on the monochrome screen. Format for the RGB color value is \"#RRGGBB\". Default value is \"#\"");
        final Option hOption = new Option("h", "help", false,
                "Show this help");

        final Options options = new Options();
        options.addOption(lpOption);
        options.addOption(caOption);
        options.addOption(cOption);
        options.addOption(hOption);

        final CommandLineParser parser = new DefaultParser();
        final CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        int listenPort = 9999;
        final String lpValue = cmd.getOptionValue(lpOption, Integer.toString(listenPort));
        try {
            listenPort = Integer.parseInt(lpValue);
        } catch (NumberFormatException e) {
            System.err.println("Could not parse listener port \"" + lpValue + "\".");
            printCommandHelp(options);
            System.exit(1);
        }

        String chip8AddressText = "localhost:9998";
        SocketAddress chip8SocketAddress = null;
        try {
            chip8AddressText = cmd.getOptionValue(caOption, chip8AddressText);
            chip8SocketAddress = getSocketAddress(chip8AddressText);
        } catch (URISyntaxException e) {
            System.err.println("Could not parse chip-8 address (and port) \"" + chip8AddressText + "\".");
            printCommandHelp(options);
            System.exit(1);
        }

        Color brightColor = new Color(0x33, 0x99, 0x00, 0xFF);
        Color darkColor = new Color(0x08, 0x18, 0x00, 0x40);

        final String cValue = cmd.getOptionValue(cOption);
        try {
            int colorRgbInt = brightColor.getRGB();
            if (cValue != null) {
                colorRgbInt = Integer.parseInt(cValue.toLowerCase().replace("#", "").trim(), 16);
                colorRgbInt |= 0xFF000000; // Set full opacity (i.e. 0 transparency)
            }

            brightColor = new Color(colorRgbInt);
            darkColor = new Color(0.15f * brightColor.getRed() / 255.0f, 0.15f * brightColor.getGreen() / 255.0f, 0.15f * brightColor.getBlue() / 255.0f);
        } catch (NumberFormatException e) {
            System.err.println("Could not parse RGB color value \"" + cValue + "\", expected format \"#RRGGBB\".");
            printCommandHelp(options);
            System.exit(1);
        }


        if (cmd.hasOption(hOption)) {
            printCommandHelp(options);
        }

        return new Configuration(listenPort, chip8SocketAddress, brightColor, darkColor);
    }

    private static void printCommandHelp(Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Chip8Screen", options, true);
    }

    private static SocketAddress getSocketAddress(String socketAddressText) throws URISyntaxException {
        // WORKAROUND: add any scheme to make the resulting URI valid.
        final URI uri = new URI("chip8://" + socketAddressText); // may throw URISyntaxException
        final String host = uri.getHost();
        final int port = uri.getPort();

        if (uri.getHost() == null || uri.getPort() == -1) {
            throw new URISyntaxException(uri.toString(), "chipAddress must specify both host and port on format \"host:port\", for example \"127.0.0.1:9998\".");
        }

        return new InetSocketAddress(host, port);
    }

}
