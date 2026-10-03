package com.myapp.quicknotes.ui.settings;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

// The light/dark choice, stored by the settings screen and applied to the whole app.
public final class ThemeSettings {
    public static final String KEY = "theme";
    private static final String LIGHT = "light";
    private static final String DARK = "dark";
    private static final String SYSTEM = "system";

    private ThemeSettings() {
    }

    public static void apply(Context context) {
        apply(PreferenceManager.getDefaultSharedPreferences(context).getString(KEY, SYSTEM));
    }

    public static void apply(String theme) {
        int mode;
        if (LIGHT.equals(theme)) {
            mode = AppCompatDelegate.MODE_NIGHT_NO;
        } else if (DARK.equals(theme)) {
            mode = AppCompatDelegate.MODE_NIGHT_YES;
        } else {
            mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}
