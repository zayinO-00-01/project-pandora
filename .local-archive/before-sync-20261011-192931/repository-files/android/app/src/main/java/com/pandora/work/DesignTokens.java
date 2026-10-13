package com.pandora.work;

import android.graphics.Color;

/** Shared semantic tokens. Keep screen code free from one-off visual decisions. */
public final class DesignTokens {
    private DesignTokens() {}
    public static final int BACKGROUND = Color.rgb(246, 248, 250);
    public static final int SURFACE = Color.rgb(255, 255, 255);
    public static final int SURFACE_MUTED = Color.rgb(240, 245, 255);
    public static final int PRIMARY = Color.rgb(46, 97, 219);
    public static final int PRIMARY_SOFT = Color.rgb(232, 242, 255);
    public static final int TEXT_PRIMARY = Color.rgb(28, 36, 48);
    public static final int TEXT_SECONDARY = Color.rgb(102, 115, 133);
    public static final int BORDER = Color.rgb(229, 233, 239);
    public static final int SUCCESS = Color.rgb(26, 140, 99);
    public static final int WARNING = Color.rgb(222, 110, 31);
    public static final int ERROR = Color.rgb(200, 70, 70);
    public static final int SURFACE_HOVER = Color.rgb(242, 245, 248);
    public static final int SURFACE_ACTIVE = Color.rgb(232, 237, 243);
    public static final int TEXT_INVERSE = Color.rgb(255, 255, 255);
    public static final int BUTTON_RADIUS = 7;
    public static final int BUTTON_HEIGHT = 40;
    public static final int BUTTON_HORIZONTAL_PADDING = 14;
    public static final int BUTTON_VERTICAL_PADDING = 7;
}
