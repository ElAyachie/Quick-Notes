package com.myapp.quicknotes.search;

// Turns text into a vector whose direction stands for its meaning, so texts about the same thing
// point the same way even when they share no words.
//
// Searches and the notes they look for are embedded by separate methods. They are different kinds
// of text, a few words against a paragraph, and retrieval models treat them differently: some
// take a prefix saying which is which, the one used here needs a different correction for each.
public interface Embedder {
    // Names the model, and changes whenever its vectors would. Stored with every vector.
    String modelId();

    // The vectors have length 1. Both methods are slow the first time, while the model loads, so
    // call them from a background thread. They throw if the model can't be loaded or run here.
    float[] embedQuery(String query);

    float[] embedPassage(String passage);
}
