package com.myapp.quicknotes.ui.settings;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.myapp.quicknotes.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        ListPreference theme = findPreference(ThemeSettings.KEY);
        if (theme != null) {
            theme.setOnPreferenceChangeListener((preference, newValue) -> {
                ThemeSettings.apply((String) newValue);
                return true;
            });
        }
    }
}
