package com.myapp.quicknotes.places;

import androidx.annotation.WorkerThread;

import org.json.JSONException;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

// Searches OpenStreetMap's places by name or address through komoot's free Photon server, which
// is made for search as you type and needs no key. Used when Google's search can't be (no key,
// the Places API switched off, Google unreachable). It knows fewer businesses than Google.
public class PhotonClient {
    private static final String URL = "https://photon.komoot.io/api/";
    // More are asked for than shown, so the nearest are among them once sorted by distance.
    private static final int ASKED_FOR = 20;
    private static final int SHOWN = 8;
    // The languages Photon can name places in; other languages get the local names.
    private static final List<String> LANGUAGES = Arrays.asList("en", "de", "fr");
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    @WorkerThread
    public List<PlaceSuggestion> search(String input, SearchArea area)
            throws IOException, JSONException {
        StringBuilder url = new StringBuilder(URL)
                .append("?q=").append(URLEncoder.encode(input, "UTF-8"))
                .append("&limit=").append(ASKED_FOR);
        if (area.hasCenter()) {
            url.append(String.format(Locale.US, "&lat=%.5f&lon=%.5f",
                    area.getLatitude(), area.getLongitude()));
        }
        String language = Locale.getDefault().getLanguage();
        if (LANGUAGES.contains(language)) {
            url.append("&lang=").append(language);
        }
        String json = Http.get(url.toString(), Http.noHeaders(), CONNECT_TIMEOUT_MS,
                READ_TIMEOUT_MS);
        List<PlaceSuggestion> places = PhotonParser.parse(json, area);
        return places.size() > SHOWN ? places.subList(0, SHOWN) : places;
    }
}
