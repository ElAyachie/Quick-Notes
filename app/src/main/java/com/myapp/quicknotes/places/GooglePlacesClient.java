package com.myapp.quicknotes.places;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Asks Google's Places API (New) for places matching what the user typed, the way Google Maps'
// search box suggests them, and for the coordinates of the one they choose. Uses the same key as
// the map (MAP_API_KEY), which must have the Places API (New) enabled.
//
// Google bills autocomplete by session: the suggestions shown while typing and the details of
// the place chosen are one charge when they carry the same session token. Only the place's
// location is asked for, which keeps the details request in the cheapest tier.
public class GooglePlacesClient {
    private static final String AUTOCOMPLETE_URL =
            "https://places.googleapis.com/v1/places:autocomplete";
    private static final String DETAILS_URL = "https://places.googleapis.com/v1/places/";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    private final Context context;
    // Read on first use, off the main thread.
    @Nullable
    private Map<String, String> headers;

    public GooglePlacesClient(Context context) {
        this.context = context.getApplicationContext();
    }

    // False when the app was built without a key; then there is no point asking Google.
    @WorkerThread
    public boolean hasKey() {
        return !headers().get("X-Goog-Api-Key").isEmpty();
    }

    @WorkerThread
    public List<PlaceSuggestion> autocomplete(String input, SearchArea area,
                                           String sessionToken)
            throws IOException, JSONException {
        String body = GooglePlacesParser.autocompleteRequest(input, area, sessionToken,
                Locale.getDefault().toLanguageTag());
        String json = Http.post(AUTOCOMPLETE_URL, headers(), "application/json",
                body.getBytes(StandardCharsets.UTF_8), CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
        return GooglePlacesParser.parseAutocomplete(json);
    }

    // The coordinates of a suggested place, or null when Google has none. Ends the session.
    @Nullable
    @WorkerThread
    public LatLng locate(String placeId, String sessionToken) throws IOException, JSONException {
        Map<String, String> headers = new HashMap<>(headers());
        headers.put("X-Goog-FieldMask", "location");
        String url = DETAILS_URL + URLEncoder.encode(placeId, "UTF-8")
                + "?sessionToken=" + URLEncoder.encode(sessionToken, "UTF-8");
        return GooglePlacesParser.parseLocation(
                Http.get(url, headers, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS));
    }

    // The key, and the app's package and signing certificate. Google checks the last two when
    // the key is restricted to this app, as a key shipped inside an APK should be.
    private synchronized Map<String, String> headers() {
        if (headers == null) {
            Map<String, String> found = new HashMap<>();
            found.put("X-Goog-Api-Key", apiKey());
            found.put("X-Android-Package", context.getPackageName());
            String certificate = certificateSha1();
            if (certificate != null) {
                found.put("X-Android-Cert", certificate);
            }
            headers = found;
        }
        return headers;
    }

    private String apiKey() {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            String key = info.metaData != null
                    ? info.metaData.getString("com.google.android.geo.API_KEY") : null;
            return key != null ? key.trim() : "";
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    // The SHA-1 fingerprint of the certificate the app is signed with, in capitals without
    // colons, as Google expects it.
    @Nullable
    @SuppressWarnings("deprecation")
    private String certificateSha1() {
        try {
            PackageManager packageManager = context.getPackageManager();
            Signature[] signatures;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo info = packageManager.getPackageInfo(context.getPackageName(),
                        PackageManager.GET_SIGNING_CERTIFICATES);
                signatures = info.signingInfo != null
                        ? info.signingInfo.getApkContentsSigners() : null;
            } else {
                signatures = packageManager.getPackageInfo(context.getPackageName(),
                        PackageManager.GET_SIGNATURES).signatures;
            }
            if (signatures == null || signatures.length == 0) {
                return null;
            }
            byte[] digest = MessageDigest.getInstance("SHA-1")
                    .digest(signatures[0].toByteArray());
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format(Locale.US, "%02X", b));
            }
            return hex.toString();
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
            return null;
        }
    }
}
