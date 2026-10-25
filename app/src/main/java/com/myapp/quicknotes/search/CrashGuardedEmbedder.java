package com.myapp.quicknotes.search;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

// Keeps a model that crashes the app from crashing it again and again.
//
// The model runs in native code, and a crash there ends the process: Java can't catch it. Since
// notes are indexed at every start, one such crash would become a crash at every start. So the
// first embedding on a device is written down before it runs and marked as done after. If the
// app finds the mark still there, the attempt never finished, and search by meaning is given up
// on this device (search by words still works). A new app version tries again.
//
// Seen on an x86 emulator running the ARM build through its ARM translation, which crashed inside
// MediaPipe on the first embedding.
public class CrashGuardedEmbedder implements Embedder {
    private static final String PREFERENCES = "embedder_health";
    private static final String TRYING = "trying";
    private static final String WORKS = "works";
    private static final String BROKEN = "broken";

    private final Embedder model;
    private final SharedPreferences preferences;
    // Which model on which app version the state below is about.
    private final String key;
    private volatile boolean knownToWork;

    public CrashGuardedEmbedder(Embedder model, Context appContext, long appVersion) {
        this.model = model;
        this.preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        this.key = model.modelId() + "@" + appVersion;
    }

    @Override
    public String modelId() {
        return model.modelId();
    }

    @Override
    public float[] embedQuery(String query) {
        checkHealth();
        return succeeded(model.embedQuery(query));
    }

    @Override
    public float[] embedPassage(String passage) {
        checkHealth();
        return succeeded(model.embedPassage(passage));
    }

    // commit() rather than apply(): the state has to be on storage before the model runs.
    @SuppressLint("ApplySharedPref")
    private synchronized void checkHealth() {
        if (knownToWork) {
            return;
        }
        String state = preferences.getString(key, null);
        if (WORKS.equals(state)) {
            knownToWork = true;
            return;
        }
        if (TRYING.equals(state) || BROKEN.equals(state)) {
            // The last attempt ended the app before it finished.
            preferences.edit().putString(key, BROKEN).commit();
            throw new IllegalStateException("The search model crashed this app before");
        }
        // Written to storage now, not later: the next moment may be a crash.
        preferences.edit().putString(key, TRYING).commit();
    }

    private float[] succeeded(float[] vector) {
        if (!knownToWork) {
            knownToWork = true;
            preferences.edit().putString(key, WORKS).apply();
        }
        return vector;
    }
}
