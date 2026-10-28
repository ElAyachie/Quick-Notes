package com.myapp.quicknotes.places;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.android.gms.maps.model.LatLng;
import com.myapp.quicknotes.AppExecutors;

import org.json.JSONException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

// Finds places by name or address for the place picker, like the search box of a map app:
// suggestions while the user types, preferring the area on screen, each with its distance from
// the user. Google's Places API gives the results; OpenStreetMap's Photon stands in when Google
// can't (no key, the API switched off, Google unreachable). Only what was typed and the area are
// sent, never note text. Requests run on a thread of their own, one at a time.
public class PlaceSearchRepository {
    private static final String TAG = "PlaceSearch";
    // Google ends a session after a few minutes; a token older than this starts a new one.
    private static final long SESSION_MILLIS = 3 * 60_000;

    public enum Source {
        GOOGLE,
        OPENSTREETMAP,
    }

    // What a search found. `failed` means no service could be asked; an empty list without it
    // means nothing matched.
    public static final class Results {
        @NonNull
        public final String query;
        @NonNull
        public final List<PlaceSuggestion> places;
        @Nullable
        public final Source source;
        public final boolean failed;

        Results(@NonNull String query, @NonNull List<PlaceSuggestion> places,
                @Nullable Source source, boolean failed) {
            this.query = query;
            this.places = places;
            this.source = source;
            this.failed = failed;
        }
    }

    private final GooglePlacesClient google;
    private final PhotonClient photon;
    private final AppExecutors executors;
    private final AtomicReference<String> latestQuery = new AtomicReference<>("");
    // The rest is touched on the place search thread only.
    // Set when Google refused the key or the API is off. That lasts, so Google isn't asked
    // again until the app restarts.
    private boolean googleRefused;
    @Nullable
    private String sessionToken;
    private long sessionStartedAt;

    public PlaceSearchRepository(GooglePlacesClient google, PhotonClient photon,
                                 AppExecutors executors) {
        this.google = google;
        this.photon = photon;
        this.executors = executors;
    }

    public void search(String query, SearchArea area, Consumer<Results> onResults) {
        latestQuery.set(query);
        executors.placeSearch().execute(() -> {
            // Typing queues a search at each pause; only the newest one is worth sending.
            if (!query.equals(latestQuery.get())) {
                return;
            }
            Results results = searchNow(query, area);
            executors.runOnMain(() -> onResults.accept(results));
        });
    }

    // Reports the place's coordinates, or null when they couldn't be found out. Google's
    // suggestions come without coordinates and are asked for them here.
    public void locate(PlaceSuggestion place, Consumer<LatLng> onLocated) {
        if (place.hasLocation()) {
            onLocated.accept(new LatLng(place.getLatitude(), place.getLongitude()));
            return;
        }
        String placeId = place.getGooglePlaceId();
        if (placeId == null) {
            onLocated.accept(null);
            return;
        }
        executors.placeSearch().execute(() -> {
            LatLng location = null;
            try {
                location = google.locate(placeId, session());
            } catch (IOException | JSONException e) {
                Log.w(TAG, "Couldn't locate " + place + ": " + e);
            }
            // Asking for a place's details ends Google's session.
            sessionToken = null;
            LatLng located = location;
            executors.runOnMain(() -> onLocated.accept(located));
        });
    }

    @WorkerThread
    private Results searchNow(String query, SearchArea area) {
        if (!googleRefused && google.hasKey()) {
            try {
                return new Results(query, google.autocomplete(query, area, session()),
                        Source.GOOGLE, false);
            } catch (Http.StatusException e) {
                // 400 is an invalid key; 403 a key not allowed for this app or this API.
                if (e.status == 400 || e.status == 401 || e.status == 403) {
                    googleRefused = true;
                }
                Log.w(TAG, "Google search failed, using OpenStreetMap: " + e.getMessage());
            } catch (IOException | JSONException e) {
                Log.i(TAG, "Google search failed, using OpenStreetMap: " + e);
            }
        }
        try {
            return new Results(query, photon.search(query, area), Source.OPENSTREETMAP, false);
        } catch (IOException | JSONException e) {
            Log.i(TAG, "OpenStreetMap search failed: " + e);
            return new Results(query, Collections.emptyList(), null, true);
        }
    }

    @WorkerThread
    private String session() {
        long now = System.currentTimeMillis();
        if (sessionToken == null || now - sessionStartedAt > SESSION_MILLIS) {
            sessionToken = UUID.randomUUID().toString();
            sessionStartedAt = now;
        }
        return sessionToken;
    }
}
