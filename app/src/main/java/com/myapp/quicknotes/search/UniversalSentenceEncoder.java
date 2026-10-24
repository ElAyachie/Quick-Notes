package com.myapp.quicknotes.search;

import android.content.Context;

import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder;
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

// Google's Universal Sentence Encoder, run on the phone by MediaPipe. The model ships inside the
// app (6 MB, in assets), so nothing is downloaded and no note text leaves the device. It
// understands English only.
//
// MediaPipe's version of the model gives every text the vector of its "response" encoder, and
// those vectors share one large common direction: any two texts, related or not, come out 0.7 to
// 0.9 similar, and the differences that carry meaning are small beside it. Subtracting the average
// vector of typical texts removes that common part. Searches and note passages each have their
// own average, because short searches and longer passages sit in different places. Measured with
// SearchQualityTest, this took the share of searches finding a right note among the first three,
// by meaning alone, from 57% to 80%.
//
// The averages are in assets (CENTERING_ASSET). They were computed from the calibration texts in
// androidTest/assets/search_calibration.txt, separate from the texts search quality is measured
// on, and CenteringVectorsTest checks that they still match.
public class UniversalSentenceEncoder implements Embedder {
    private static final String MODEL_ASSET = "universal_sentence_encoder.tflite";
    static final String CENTERING_ASSET = "universal_sentence_encoder_centering.json";

    private final Context appContext;
    // Loaded on first use rather than at app start, which it would slow down.
    private TextEmbedder embedder;
    private float[] queryMean;
    private float[] passageMean;

    public UniversalSentenceEncoder(Context appContext) {
        this.appContext = appContext;
    }

    @Override
    public String modelId() {
        // The model file's name and date on Google's model storage, and the version of the
        // centering. Change the last part whenever the centering vectors change.
        return "universal_sentence_encoder/float32/2023-04-26+centering-1";
    }

    @Override
    public float[] embedQuery(String query) {
        return centered(embedPlain(query), means()[0]);
    }

    @Override
    public float[] embedPassage(String passage) {
        return centered(embedPlain(passage), means()[1]);
    }

    // The model's own vector, scaled to length 1 but not centered. For CenteringVectorsTest.
    synchronized float[] embedPlain(String text) {
        if (embedder == null) {
            embedder = TextEmbedder.createFromOptions(appContext, TextEmbedderOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL_ASSET).build())
                    .build());
        }
        return Vectors.normalized(
                embedder.embed(text).embeddingResult().embeddings().get(0).floatEmbedding());
    }

    private static float[] centered(float[] vector, float[] mean) {
        float[] result = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            result[i] = vector[i] - mean[i];
        }
        return Vectors.normalized(result);
    }

    // The query and passage averages, read from assets the first time.
    private synchronized float[][] means() {
        if (queryMean == null) {
            try (InputStream in = appContext.getAssets().open(CENTERING_ASSET)) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    bytes.write(buffer, 0, read);
                }
                JSONObject json = new JSONObject(bytes.toString(StandardCharsets.UTF_8.name()));
                queryMean = floats(json.getJSONArray("query_mean"));
                passageMean = floats(json.getJSONArray("passage_mean"));
            } catch (IOException | JSONException e) {
                throw new IllegalStateException("Can't read " + CENTERING_ASSET, e);
            }
        }
        return new float[][]{queryMean, passageMean};
    }

    private static float[] floats(JSONArray array) throws JSONException {
        float[] values = new float[array.length()];
        for (int i = 0; i < values.length; i++) {
            values[i] = (float) array.getDouble(i);
        }
        return values;
    }
}
