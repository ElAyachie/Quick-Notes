package com.myapp.quicknotes;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// Where work runs: database access on one background thread, results back on the main thread.
// Search has a thread of its own, so embedding a long note never holds up saving another, and so
// do requests over the network, which can take a minute.
public class AppExecutors {
    private final Executor io = Executors.newSingleThreadExecutor();
    private final Executor search = Executors.newSingleThreadExecutor();
    private final Executor network = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public Executor io() {
        return io;
    }

    public Executor search() {
        return search;
    }

    public Executor network() {
        return network;
    }

    public void runOnMain(Runnable task) {
        mainHandler.post(task);
    }
}
