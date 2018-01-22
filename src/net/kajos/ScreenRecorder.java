package net.kajos;

import net.kajos.Manager.Manager;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.direct.BufferFormat;
import uk.co.caprica.vlcj.player.direct.BufferFormatCallback;
import uk.co.caprica.vlcj.player.direct.DirectMediaPlayer;
import uk.co.caprica.vlcj.player.direct.format.RV32BufferFormat;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ScreenRecorder {
    private Manager manager;

    public ScreenRecorder(Manager manager) {
        this.manager = manager;
    }

    private final int offsetX = 1920;
    private final int offsetY = 0;
    private final int width = 1920; // Display two - right
    private final int height = 1080;

    private BufferedImage image;
    private MediaPlayerFactory factory;
    private DirectMediaPlayer mediaPlayer;
    private RenderCallback callback;

    public void start() {
        image = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(width, height);
        image.setAccelerationPriority(1.0f);

        String mrl = "screen://";
        String[] options = {
                ":screen-fps=" + Config.FPS,
                ":live-caching=0",
                ":screen-width=" + width,
                ":screen-height=" + height,
                ":screen-left=" + offsetX,
                ":screen-top=" + offsetY
        };
        factory = new MediaPlayerFactory();
        callback = new RenderCallback(image, manager);
        mediaPlayer = factory.newDirectMediaPlayer(new TestBufferFormatCallback(), callback);
        mediaPlayer.playMedia(mrl, options);

        System.out.println("Recorder set up");
    }

    private final class TestBufferFormatCallback implements BufferFormatCallback {

        @Override
        public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
            return new RV32BufferFormat(sourceWidth, sourceHeight);
        }

    }

}
