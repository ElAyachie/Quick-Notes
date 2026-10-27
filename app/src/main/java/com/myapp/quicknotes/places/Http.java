package com.myapp.quicknotes.places;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

// The plain HTTPS requests the place lookups make. Called on a background thread only.
final class Http {
    // Who is asking. OpenStreetMap's free servers ask apps to say so.
    static final String USER_AGENT = "QuickNotes (+https://github.com/ElAyachie/Quick-Notes)";

    private Http() {
    }

    // A server answered, but not with success. The status tells a refused key apart from a busy
    // server.
    static final class StatusException extends IOException {
        final int status;

        StatusException(int status, String body) {
            super("HTTP " + status + (body.isEmpty() ? "" : ": " + body));
            this.status = status;
        }
    }

    @WorkerThread
    static String get(String url, Map<String, String> headers, int connectTimeoutMs,
                      int readTimeoutMs) throws IOException {
        return request("GET", url, headers, null, null, connectTimeoutMs, readTimeoutMs);
    }

    @WorkerThread
    static String post(String url, Map<String, String> headers, String contentType, byte[] body,
                       int connectTimeoutMs, int readTimeoutMs) throws IOException {
        return request("POST", url, headers, contentType, body, connectTimeoutMs, readTimeoutMs);
    }

    static Map<String, String> noHeaders() {
        return Collections.emptyMap();
    }

    private static String request(String method, String url, Map<String, String> headers,
                                  @Nullable String contentType, @Nullable byte[] body,
                                  int connectTimeoutMs, int readTimeoutMs) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setRequestMethod(method);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            if (body != null) {
                connection.setRequestProperty("Content-Type", contentType);
                connection.setDoOutput(true);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(body);
                }
            }
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new StatusException(status, readQuietly(connection.getErrorStream()));
            }
            try (InputStream in = connection.getInputStream()) {
                return read(in);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream in) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[16_384];
        int read;
        while ((read = in.read(buffer)) != -1) {
            bytes.write(buffer, 0, read);
        }
        return bytes.toString(StandardCharsets.UTF_8.name());
    }

    // An error's explanation, for the log. Shortened, since some servers send a whole page.
    private static String readQuietly(@Nullable InputStream in) {
        if (in == null) {
            return "";
        }
        try (InputStream stream = in) {
            String text = read(stream).trim();
            return text.length() > 300 ? text.substring(0, 300) : text;
        } catch (IOException e) {
            return "";
        }
    }
}
