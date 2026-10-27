package com.myapp.quicknotes.places;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.myapp.quicknotes.data.NearbyPlace;

import org.json.JSONException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

// Asks OpenStreetMap's public Overpass servers what is near a place. Only the place's coordinates
// and radius are sent, never note text. The servers are free and need no key; they ask that apps
// say who they are and don't send many requests at once, so lookups are made one at a time.
public class OverpassClient {
    private static final String TAG = "OverpassClient";
    // Tried in order. The main server can take most of a minute to answer when busy.
    private static final String[] SERVERS = {
            "https://overpass-api.de/api/interpreter",
            "https://overpass.private.coffee/api/interpreter",
    };
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 75_000;

    // The places around the circle, or null when no server could be reached; then the lookup is
    // tried again later. An empty list means the servers answered and found nothing.
    @Nullable
    @WorkerThread
    public List<NearbyPlace> lookUp(String placeKey, double latitude, double longitude,
                                    float radiusMeters) {
        String query = OverpassParser.query(latitude, longitude, radiusMeters);
        for (String server : SERVERS) {
            try {
                String json = post(server, query);
                return OverpassParser.parse(json, placeKey, latitude, longitude, radiusMeters);
            } catch (IOException | JSONException e) {
                Log.i(TAG, "No answer from " + server + ": " + e);
            }
        }
        return null;
    }

    private static String post(String server, String query) throws IOException {
        byte[] body = ("data=" + URLEncoder.encode(query, "UTF-8"))
                .getBytes(StandardCharsets.UTF_8);
        return Http.post(server, Http.noHeaders(), "application/x-www-form-urlencoded", body,
                CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
    }
}
