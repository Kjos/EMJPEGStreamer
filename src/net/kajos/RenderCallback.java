package net.kajos;

import net.kajos.Manager.Manager;
import net.kajos.Manager.Viewer;
import net.kajos.Manager.Quality;
import uk.co.caprica.vlcj.player.direct.DirectMediaPlayer;
import uk.co.caprica.vlcj.player.direct.RenderCallbackAdapter;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RenderCallback extends RenderCallbackAdapter {
    private int frameCount = 0;

    private ExecutorService exec;
    private ImagePool pool;

    private LowPassFilter frameTime = new LowPassFilter(Config.FRAMETIME_ALPHA,
            1000f / (float)Config.FPS);

    private int width, height;
    private Manager manager;

    public RenderCallback(BufferedImage image, Manager manager) {
        super(((DataBufferInt) image.getRaster().getDataBuffer()).getData());
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.manager = manager;

        exec = Executors.newFixedThreadPool(Config.MAX_VIEWERS);

        // Interlaced, so height divided by two
        pool = new ImagePool(width,
                height / 2, BufferedImage.TYPE_INT_RGB);

    }

    private long prevTimestamp = -1;

    private boolean missingKeyframe(Viewer viewer) {
        return viewer.rgb == null || viewer.rgb[0][0].length != width ||
                viewer.rgb[0].length != height;
    }

    @Override
    public void onDisplay(DirectMediaPlayer mediaPlayer, int[] frameData) {
        long timestamp = System.currentTimeMillis();
        if (prevTimestamp != -1) frameTime.get(timestamp - prevTimestamp);
        prevTimestamp = timestamp;

        frameCount++;

        Iterator<Viewer> plIt = manager.viewers.values().iterator();

        while (plIt.hasNext()) {
            final Viewer viewer = plIt.next();

            exec.submit(new Runnable() {

                private void runFrame() {
                    boolean isKeyFrame = viewer.frameCount == 0;
                    boolean missingKeyFrame = missingKeyframe(viewer);

                    int viewerBytes;
                    if (isKeyFrame || missingKeyFrame) {
                        viewerBytes = interlaceKeyFrame(viewer, frameData, missingKeyFrame);
                    } else {
                        viewerBytes = interlaceFrame(viewer, frameData);
                    }

                    int bandwidth = (int)viewer.bandwidth.get(viewerBytes);

                    Quality quality = viewer.quality;
                    if (Math.abs(viewer.frameTime - frameTime.get()) > Config.FRAME_SWING ||
                            bandwidth > Config.MAX_BANDWIDTH_VIEWER_BYTES_FRAME) {
                        quality.lower();
                    } else {
                        float sumBandwidth = manager.getSumBandwidth();
                        if (sumBandwidth > Config.MAX_BANDWIDTH_BYTES_FRAME) {
                            if (bandwidth > sumBandwidth / manager.viewers.size() ||
                                    manager.viewers.size() == 1) {
                                quality.lower();
                            }
                        } else {
                            quality.raise();
                        }
                    }
                }

                @Override
                public void run() {
                    boolean lateEncoding = !viewer.frameSem.tryAcquire();

                    if (lateEncoding) {
                        manager.sendEmptyImage(viewer.hash);
                        viewer.bandwidth.get(0);
                        System.out.println("Busy encoding");
                    } else if(frameCount % viewer.quality.frameSkip != 0) {
                        manager.sendEmptyImage(viewer.hash);
                        viewer.bandwidth.get(0);
                        System.out.println("Skipped frame");
                        viewer.frameSem.release();
                    } else {
                        runFrame();
                        viewer.frameSem.release();
                    }
                }
            });
        }
    }

    private int interlaceKeyFrame(Viewer viewer, int[] frameData, boolean missingKeyFrame) {
        Quality quality = viewer.quality;

        viewer.keyFrameToggle = !viewer.keyFrameToggle;

        boolean interpol = viewer.keyFrameToggle;
        int code = interpol ? 1 : 2;

        ImageWrapper img = pool.get();

        int startX = 0;
        int startY = 0;
        if (interpol) startY += 1;
        // Interlaced, skip one extra
        int skipW = width * 2 - img.width;

        int p = startX + startY * width;
        int p2 = 0;

        int[] pixels = img.pixels;

        if (missingKeyFrame) {
            viewer.rgb = new int[3][height][width];
            System.out.println("Create framebuffer");
        }

        int[][] r = viewer.rgb[0];
        int[][] g = viewer.rgb[1];
        int[][] b = viewer.rgb[2];

        for (int y = 0; y < img.height; y++, p+=skipW) {
            for (int x = 0; x < img.width; x++, p++, p2++) {
                int c = frameData[p];
                int cr = (c >> 16) & 0xff;
                int cg = (c >> 8) & 0xff;
                int cb = c & 0xff;

                r[y][x] = cr;
                g[y][x] = cg;
                b[y][x] = cb;
                pixels[p2] = c;
            }
        }

        quality.lastKeyFrameFormat = quality.frameFormat;
        quality.frameFormat = Config.LOW_FORMAT;
        viewer.lastDifference = 0;
        viewer.sumDifference = 0f;
        viewer.lastInterFrameSize = 0;
        viewer.skipInterlace2 = false;

        byte[] data = img.getCompressedBytes(code, quality.jpegQuality.get(),
                quality.frameFormat);

        System.out.println("Keyframe " + code + ": " + quality.frameFormat + ", size: " + data.length);

        manager.sendImage(viewer.hash, data);
        viewer.frameCount++;
        viewer.lastKeyFrameSize = data.length;

        pool.put(img);

        return data.length;
    }

    private int interlaceFrame(Viewer viewer, int[] frameData) {
        Quality quality = viewer.quality;

        int viewerBytes = 0;

        int ip = viewer.frameCount % 2;
        boolean interpol = ip == 0;

        ImageWrapper img = pool.get();

        int startX = 0;
        int startY = 0;
        if (interpol) startY += 1;
        // Interlaced, skip one extra
        int skipW = width * 2 - img.width;

        int p = startX + startY * width;
        int p2 = 0;

        int[] pixels = img.pixels;

        viewer.frameCount++;

        if (interpol == viewer.keyFrameToggle) {
            viewer.skipInterlace2 = false;

            float difference = 0;

            for (int y = 0; y < img.height; y++, p += skipW) {
                int[] r = viewer.rgb[0][y];
                int[] g = viewer.rgb[1][y];
                int[] b = viewer.rgb[2][y];
                for (int x = 0; x < img.width; x++, p++, p2++) {
                    int c = frameData[p];
                    int cr = (c >> 16) & 0xff;
                    int cg = (c >> 8) & 0xff;
                    int cb = c & 0xff;

                    cr = cr - r[x];
                    cg = cg - g[x];
                    cb = cb - b[x];

                    cr /= 2;
                    cg /= 2;
                    cb /= 2;

                    difference += Math.abs(cr);
                    difference += Math.abs(cg);
                    difference += Math.abs(cb);

                    cr = 127 + cr;
                    cg = 127 + cg;
                    cb = 127 + cb;

                    pixels[p2] = (cr << 16) | (cg << 8) | cb;
                }
            }

            difference /= (float)(img.width * img.height * 127);

            viewer.sumDifference += difference;

            float diff = Math.abs(viewer.lastDifference - difference);

            if (diff < Config.IGNORE_DIFFERENCE) {
                //System.out.println(diff);
                manager.sendEmptyImage(viewer.hash);
                viewer.skipInterlace2 = true;
                if (quality.lastKeyFrameFormat != Config.HIGH_FORMAT) {
                    //viewer.frameCount += Config.B_FRAME_SPEED_UP;
                    quality.frameFormat = Config.HIGH_FORMAT;
                }
            } else {

                byte[] data = img.getCompressedBytes(ip + 3, quality.jpegQuality.get(),
                        quality.interImageFormat);

                System.out.println("Interframe: " + quality.interImageFormat + ", size: " + data.length +
                    ", diff.:" + diff + ", sum diff.:" + viewer.sumDifference);

                viewerBytes = data.length;
                manager.sendImage(viewer.hash, data);

                // Frame is not going back to keyframe
                if (data.length > viewer.lastKeyFrameSize) {
                    //System.out.print("Keyframe smaller than interframe");
                    System.out.println("Bframes: " + viewer.frameCount);
                    viewer.frameCount = 0;
                    quality.frameFormat = Config.LOW_FORMAT;
                } else if (viewer.lastDifference < difference &&
                        viewer.lastInterFrameSize < data.length) {

                    if (difference > Config.KEYFRAME_THRESHOLD ||
                            data.length > viewer.lastKeyFrameSize) {
                        System.out.println("Bframes: " + viewer.frameCount);
                        viewer.frameCount = 0;
                        quality.frameFormat = Config.LOW_FORMAT;
                    }
                }

                viewer.lastInterFrameSize = data.length;
            }

            if (viewer.sumDifference > Config.KEYFRAME_THRESHOLD2) {
                System.out.println("Bframes: " + viewer.frameCount + " Sum diff: " + viewer.sumDifference);
                viewer.frameCount = 0;
            }

            viewer.lastDifference = difference;
        } else {
            if (!viewer.skipInterlace2) {
                for (int y = 0; y < img.height; y++, p += skipW) {
                    int[] r = viewer.rgb[0][y];
                    int[] g = viewer.rgb[1][y];
                    int[] b = viewer.rgb[2][y];
                    for (int x = 0; x < img.width; x++, p++, p2++) {
                        int c = frameData[p];
                        int cr = (c >> 16) & 0xff;
                        int cg = (c >> 8) & 0xff;
                        int cb = c & 0xff;

                        cr = cr - r[x];
                        cg = cg - g[x];
                        cb = cb - b[x];

                        cr /= 2;
                        cg /= 2;
                        cb /= 2;

                        cr = 127 + cr;
                        cg = 127 + cg;
                        cb = 127 + cb;

                        pixels[p2] = (cr << 16) | (cg << 8) | cb;
                    }
                }
                byte[] data = img.getCompressedBytes(ip + 3, quality.jpegQuality.get(),
                        quality.interImageFormat);
                viewerBytes = data.length;
                manager.sendImage(viewer.hash, data);

                System.out.println("Interframe 2: " + quality.interImageFormat + ", size: " + data.length);

            } else {
                manager.sendEmptyImage(viewer.hash);
            }
        }

        pool.put(img);

        return viewerBytes;
    }
}
