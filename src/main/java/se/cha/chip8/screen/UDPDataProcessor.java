package se.cha.chip8.screen;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class UDPDataProcessor implements UDPMulticastMessageListener.UDPPacketDataProcessor {

    @Override
    public void onPacketReceived(byte[] data) {
        try {
            final ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
            final PeripheralState peripheralState = objectMapper.readValue(data, PeripheralState.class);

            final ScreenFrame screenFrame = ScreenFrame.getOrCreateSingleton();

            if (peripheralState.getScreen() != null) {
                screenFrame.setChip8ScreenData(peripheralState.getScreen());
            }

            screenFrame.setChip8KeyState(peripheralState.getKeys());
            screenFrame.setChip8SoundState(peripheralState.isSound());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
