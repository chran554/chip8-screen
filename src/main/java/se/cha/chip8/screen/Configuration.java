package se.cha.chip8.screen;

import lombok.Value;

import java.awt.*;
import java.net.SocketAddress;

@Value
public class Configuration {

    int listenerPort;
    SocketAddress chip8Address;
    Color brightColor;
    Color darkColor;

    @Override
    public String toString() {
        return "Configuration{" +
                "screen update listen port: " + listenerPort +
                ", chip8 emulator socket key state address:" + chip8Address +
                ", color: #" + Integer.toHexString(brightColor.getRGB()) +
                '}';
    }
}
