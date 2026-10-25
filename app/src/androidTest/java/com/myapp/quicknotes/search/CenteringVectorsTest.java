package com.myapp.quicknotes.search;

import static org.junit.Assert.fail;

import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// The centering vectors shipped in the app's assets must be the averages of the calibration texts
// (androidTest/assets/search_calibration.txt) as the model embeds them. When the model or the
// calibration texts change, this fails and logs the new file contents (tag CenteringVectors) to
// put in app/src/main/assets; change the "centering" part of UniversalSentenceEncoder.modelId
// at the same time, so stored notes are embedded again.
@RunWith(AndroidJUnit4.class)
public class CenteringVectorsTest {
    private static final String TAG = "CenteringVectors";
    private static final float TOLERANCE = 1e-4f;

    @Test
    public void shippedCenteringVectorsAreTheAveragesOfTheCalibrationTexts() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        UniversalSentenceEncoder model = new UniversalSentenceEncoder(context);
        List<String> queries = new ArrayList<>();
        List<String> passages = new ArrayList<>();
        readCalibrationTexts(queries, passages);
        float[] queryMean = average(model, queries);
        float[] passageMean = average(model, passages);

        JSONObject expected = new JSONObject()
                .put("query_mean", json(queryMean))
                .put("passage_mean", json(passageMean));
        JSONObject shipped;
        try (InputStream in = context.getAssets().open(UniversalSentenceEncoder.CENTERING_ASSET)) {
            shipped = new JSONObject(read(in));
        } catch (java.io.IOException missing) {
            shipped = null;
        }
        if (shipped == null || !close(shipped.getJSONArray("query_mean"), queryMean)
                || !close(shipped.getJSONArray("passage_mean"), passageMean)) {
            Log.i(TAG, expected.toString());
            fail("Centering vectors are out of date; the new ones are in the log (tag " + TAG + ")");
        }
    }

    private static void readCalibrationTexts(List<String> queries, List<String> passages)
            throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                InstrumentationRegistry.getInstrumentation().getContext().getAssets()
                        .open("search_calibration.txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Q: ")) {
                    queries.add(line.substring(3));
                } else if (line.startsWith("P: ")) {
                    passages.add(line.substring(3).replace(" | ", "\n"));
                }
            }
        }
    }

    private static float[] average(UniversalSentenceEncoder model, List<String> texts) {
        float[] sum = null;
        for (String text : texts) {
            float[] vector = model.embedPlain(text);
            if (sum == null) {
                sum = new float[vector.length];
            }
            for (int i = 0; i < vector.length; i++) {
                sum[i] += vector[i];
            }
        }
        for (int i = 0; i < sum.length; i++) {
            sum[i] /= texts.size();
        }
        return sum;
    }

    private static JSONArray json(float[] vector) throws Exception {
        JSONArray array = new JSONArray();
        for (float value : vector) {
            array.put(Double.parseDouble(String.format(Locale.US, "%.6f", value)));
        }
        return array;
    }

    private static boolean close(JSONArray shipped, float[] computed) throws Exception {
        if (shipped.length() != computed.length) {
            return false;
        }
        for (int i = 0; i < computed.length; i++) {
            if (Math.abs(shipped.getDouble(i) - computed[i]) > TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    private static String read(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toString(StandardCharsets.UTF_8.name());
    }
}
