package com.pandora.work;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;

/** Small shared primitives used by every screen. */
public final class UiComponents {
    private UiComponents() {}
    private static int dp(Context context, int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }

    public static TextView text(Context context, String value, float size, int color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setIncludeFontPadding(true);
        view.setLineSpacing(dp(context, 2), 1f);
        view.setPadding(0, dp(context, 4), 0, dp(context, 4));
        view.setMaxLines(3);
        view.setEllipsize(TextUtils.TruncateAt.END);
        return view;
    }

    public static Button button(Context context, String label, int color, int foreground, int minHeight) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(context, Math.max(minHeight, DesignTokens.BUTTON_HEIGHT)));
        button.setMinimumHeight(dp(context, Math.max(minHeight, DesignTokens.BUTTON_HEIGHT)));
        button.setPadding(dp(context, DesignTokens.BUTTON_HORIZONTAL_PADDING), dp(context, DesignTokens.BUTTON_VERTICAL_PADDING), dp(context, DesignTokens.BUTTON_HORIZONTAL_PADDING), dp(context, DesignTokens.BUTTON_VERTICAL_PADDING));
        button.setTextColor(foreground);
        button.setGravity(Gravity.CENTER);
        button.setIncludeFontPadding(true);
        button.setSingleLine(true);
        button.setMaxLines(1);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setBackground(surface(color, dp(context, DesignTokens.BUTTON_RADIUS), DesignTokens.BORDER));
        return button;
    }

    public static Button filterButton(Context context, String label) {
        Button button = button(context, label, DesignTokens.SURFACE, DesignTokens.TEXT_PRIMARY, DesignTokens.BUTTON_HEIGHT);
        button.setTextSize(14);
        button.setTextColor(DesignTokens.TEXT_SECONDARY);
        button.setBackground(surface(DesignTokens.SURFACE, dp(context, DesignTokens.BUTTON_RADIUS), DesignTokens.BORDER));
        return button;
    }

    public static GradientDrawable surface(int color, int radius, int border) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(1, border);
        return drawable;
    }
}
