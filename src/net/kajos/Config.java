package net.kajos;

public class Config {
    public static final int WEB_PORT = 7578;

    public static final int MAX_VIEWERS = 4;

    public static final int FPS = 30;

    public static final int MAX_BANDWIDTH_BYTES_FRAME = 15*1024*1024/8/Config.FPS;
    public static final int MAX_BANDWIDTH_VIEWER_BYTES_FRAME = MAX_BANDWIDTH_BYTES_FRAME;

    public static float FRAME_SWING = 1000 / FPS;
    public static float FRAMETIME_ALPHA = 0.01f;
    public static float QUALITY_ALPHA = 1f;
    public static float QUALITY_ADJUST = .05f;
    public static float KEYFRAME_THRESHOLD = .3f;
    public static float KEYFRAME_THRESHOLD2 = 3.3f;

    public static final String HIGH_FORMAT = Constants.PNG;
    public static final String LOW_FORMAT = Constants.JPEG;
    public static final String INTERFRAME_FORMAT = Constants.JPEG;

    public static final float IGNORE_DIFFERENCE = 0.000005f;

    public static final int MAX_FRAME_SKIP = 2;

    public static final float MIN_QUALITY = .3f;
    public static final float MAX_QUALITY = .9f;
}
