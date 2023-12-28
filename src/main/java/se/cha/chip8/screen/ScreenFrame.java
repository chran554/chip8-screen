package se.cha.chip8.screen;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

public class ScreenFrame extends JFrame {

    private static ScreenFrame singleton = null;

    private final JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    private BufferedImage image = null;
    private BufferedImage doubleBufferImage;
    private BufferedImage bufferImageRenderSize;


    private BufferedImage crtImage = null;
    private BufferedImage crtGlareImage = null;

    private int[] bufferImageData;
    private BufferedImage bufferImage;
    private BufferedImage phosphorImage;
    private BufferedImage fadeImage = null;
    private BufferedImage activeLightsImage = null;

    private int keyState = 0x0000;
    private boolean soundState = false;

    private final int width = 64;
    private final int height = 32;
    private Configuration configuration;

    private ScreenFrame() {
        super("CHIP-8");
    }

    public static ScreenFrame getOrCreateSingleton() {
        if (singleton == null) {
            singleton = new ScreenFrame();
        }

        return singleton;
    }


    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;

        final ImageIcon imageIcon = new ImageIcon(image);
        final JLabel label = new JLabel(imageIcon);
        scrollPane.setViewportView(label);

        resizeFrame();
    }


    public synchronized void setChip8ScreenData(byte[] imageBitData) {
        final Color brightColor = configuration.getBrightColor();
        final int pixelOn = new Color(brightColor.getRed(), brightColor.getGreen(), brightColor.getBlue(), 0xFF).getRGB();
        final int pixelOff = new Color(brightColor.getRed(), brightColor.getGreen(), brightColor.getBlue(), 0x00).getRGB();

        // Convert bit array (of bytes) with one bit per pixel to array of int with one int per pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int streamPixelIndex = x + (y * width);
                final int streamByteIndex = streamPixelIndex / 8;
                final int streamBitInByteIndex = streamPixelIndex % 8;

                final int streamBitValue = (imageBitData[streamByteIndex] >> (7 - streamBitInByteIndex)) & 0x00000001;
                bufferImageData[streamPixelIndex] = streamBitValue == 1 ? pixelOn : pixelOff;
            }
        }

        // printScreen(bufferImageData);
        bufferImage.setRGB(0, 0, width, height, bufferImageData, 0, width);
    }

    private void printScreen(int[] screenBuffer) {
        final StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int pixelValue = screenBuffer[x + y * width];
                sb.append((pixelValue != 0) ? "██" : "░░");
            }
            sb.append("\n");
        }

        System.out.println(sb);
    }

    public void setChip8KeyState(int keyState) {
        if (keyState != this.keyState) {
            //System.out.println("New chip 8 key pad state:     " + KeyPad.leftPad(Integer.toBinaryString(keyState), "0", 16));
            this.keyState = keyState;
        }
    }

    public void setChip8SoundState(boolean soundState) {
        if (soundState != this.soundState) {
            // System.out.println("New chip 8 sound state: " + (soundState ? "on" : "off"));
            this.soundState = soundState;

            if (this.soundState) {
                BeepGenerator.playBeep();
            } else {
                BeepGenerator.pauseBeep();
            }
        }
    }

    private void updateCrt() {
        final Graphics2D bufferImageRenderSizeGraphics = (Graphics2D) bufferImageRenderSize.getGraphics();
        final int bufferImagewidth = bufferImageRenderSize.getWidth();
        final int bufferImageheight = bufferImageRenderSize.getHeight();
        bufferImageRenderSize.setRGB(0, 0, bufferImagewidth, bufferImageheight, new int[bufferImagewidth * bufferImageheight], 0, bufferImagewidth);
        bufferImageRenderSizeGraphics.drawImage(bufferImage, 40, 40, 860, 640, null);
        bufferImageRenderSizeGraphics.dispose();

        final Graphics phosphorImageGraphics = phosphorImage.getGraphics();
        phosphorImageGraphics.drawImage(bufferImageRenderSize, 0, 0, null);
        phosphorImageGraphics.drawImage(fadeImage, 0, 0, null);
        phosphorImageGraphics.dispose();

        //final BufferedImage phosphorGlowImage = Blur2.blur(bufferImageRenderSize, 30, true);
        //final int[] phosphorGlowImageRGB = phosphorGlowImage.getRGB(0, 0, phosphorGlowImage.getWidth(), phosphorGlowImage.getHeight(), null, 0, phosphorGlowImage.getWidth());
        //for (int i = 0; i < phosphorGlowImageRGB.length; i++) {
        //    phosphorGlowImageRGB[i] = (phosphorGlowImageRGB[i] & 0x00FFFFFF) | (0x44 << 24); // Glare amount
        //}
        //phosphorGlowImage.setRGB(0, 0, phosphorGlowImage.getWidth(), phosphorGlowImage.getHeight(), phosphorGlowImageRGB, 0, phosphorGlowImage.getWidth());

        final Graphics doubleBufferImageGraphics = doubleBufferImage.getGraphics();
        doubleBufferImageGraphics.setColor(Color.BLACK);
        doubleBufferImageGraphics.fillRect(0, 0, doubleBufferImage.getWidth(), doubleBufferImage.getHeight());

        doubleBufferImageGraphics.drawImage(phosphorImage, 220 - 40, 200 - 40, null);
        //doubleBufferImageGraphics.drawImage(phosphorGlowImage, 220 - 40, 200 - 40, null);
        doubleBufferImageGraphics.drawImage(crtGlareImage, 0, 0, null);
        doubleBufferImageGraphics.drawImage(crtImage, 0, 0, null);

        drawLights(doubleBufferImageGraphics, doubleBufferImage.getWidth(), doubleBufferImage.getHeight());

        doubleBufferImageGraphics.dispose();

        final Graphics2D g = (Graphics2D) getImage().getGraphics();
        g.drawImage(doubleBufferImage, 0, 0, null);
        g.dispose();

        repaint();
    }

    private void drawLights(Graphics doubleBufferImageGraphics, int width, int height) {
        if (soundState) {
            final int ledX = width - 195;
            final int ledY = 220;
            final int ledSize = 35;
            doubleBufferImageGraphics.drawImage(activeLightsImage, ledX, ledY, ledX + ledSize, ledY + ledSize, ledX, ledY, ledX + ledSize, ledY + ledSize, null);
        }

        if (keyState > 0) {
            final int xs = width - 240;
            final int ys = height - 315;
            final int kw = 31;

            for (int keyBitIndex = 0; keyBitIndex <= 0xF; keyBitIndex++) {
                final boolean keyPressed = ((keyState >>> keyBitIndex) & 1) > 0;
                if (keyPressed) {
                    int kx = 0;
                    int ky = 0;
                    if ((keyBitIndex >= 0x1) && (keyBitIndex <= 0x9)) {
                        kx = (keyBitIndex - 1) % 3;
                        ky = (keyBitIndex - 1) / 3;
                    } else if ((keyBitIndex >= 0xC) && (keyBitIndex <= 0xF)) {
                        kx = 3;
                        ky = keyBitIndex - 0xC;
                    } else if (keyBitIndex == 0xA) {
                        kx = 0;
                        ky = 3;
                    } else if (keyBitIndex == 0x0) {
                        kx = 1;
                        ky = 3;
                    } else if (keyBitIndex == 0xB) {
                        kx = 2;
                        ky = 3;
                    }

                    // doubleBufferImageGraphics.setColor(Color.RED);
                    // doubleBufferImageGraphics.drawRect(xs + kx * kw, ys + ky * kw, kw, kw);
                    doubleBufferImageGraphics.drawImage(activeLightsImage, xs + kx * kw, ys + ky * kw, xs + kx * kw + kw, ys + ky * kw + kw, xs + kx * kw, ys + ky * kw, xs + kx * kw + kw, ys + ky * kw + kw, null);
                }
            }
        }
    }

    public void initialize(Configuration configuration) {
        this.configuration = configuration;
        addKeyListener(new KeyPad(configuration.getChip8Address()));

        setImage(new BufferedImage(1432, 1071, BufferedImage.TYPE_INT_ARGB));

        bufferImageRenderSize = new BufferedImage(940, 720, BufferedImage.TYPE_INT_ARGB);
        doubleBufferImage = new BufferedImage(1432, 1071, BufferedImage.TYPE_INT_ARGB);

        try {
            crtImage = ImageIO.read(Objects.requireNonNull(ClassLoader.getSystemResourceAsStream("CHIP-8 monitor.png")));
            crtGlareImage = ImageIO.read(Objects.requireNonNull(ClassLoader.getSystemResourceAsStream("CHIP-8 monitor - glare.png")));
            activeLightsImage = ImageIO.read(Objects.requireNonNull(ClassLoader.getSystemResourceAsStream("CHIP-8 monitor - active.png")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final Color darkColor = configuration.getDarkColor();

        fadeImage = new BufferedImage(940, 720, BufferedImage.TYPE_INT_ARGB);
        final Graphics fadeGraphics = fadeImage.getGraphics();
        fadeGraphics.setColor(new Color(darkColor.getRed(), darkColor.getGreen(), darkColor.getBlue(), 0x40));
        fadeGraphics.fillRect(0, 0, fadeImage.getWidth() - 1, fadeImage.getHeight() - 1);
        fadeGraphics.setColor(new Color(darkColor.getRed(), darkColor.getGreen(), darkColor.getBlue(), 0x80));
        for (int crtY = 0; crtY < fadeImage.getHeight(); crtY += 3) {
            fadeGraphics.drawLine(0, crtY, fadeImage.getWidth(), crtY);
        }
        fadeGraphics.dispose();

        bufferImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        bufferImageData = new int[width * height];

        phosphorImage = new BufferedImage(940, 720, BufferedImage.TYPE_INT_ARGB);
        final Graphics phosphorImageGraphics = phosphorImage.getGraphics();
        phosphorImageGraphics.setColor(Color.BLACK);
        phosphorImageGraphics.fillRect(0, 0, phosphorImage.getWidth(), phosphorImage.getHeight());
        phosphorImageGraphics.dispose();

        final Graphics graphics = image.getGraphics();
        graphics.setColor(java.awt.Color.BLACK);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();

        final Thread thread = new Thread(() -> {
            while (true) {
                updateCrt();

                try {
                    Thread.sleep(1000 / 60);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();

        add(scrollPane);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        pack();
        centerFrame();
        setVisible(true);
    }

    protected void resizeFrame() {
        // Make sure window fit within desktop area
        final Rectangle maxWindowBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        final int width = getWidth();
        final int height = getHeight();
        setSize(Math.min(width, maxWindowBounds.width), Math.min(height, maxWindowBounds.height));
    }

    public void centerFrame() {
        setLocationRelativeTo(null);
    }
}