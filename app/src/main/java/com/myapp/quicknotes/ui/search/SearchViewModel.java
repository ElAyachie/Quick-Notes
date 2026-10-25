package com.myapp.quicknotes.ui.search;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import com.myapp.quicknotes.QuickNotesApp;
import com.myapp.quicknotes.search.SearchRepository;
import com.myapp.quicknotes.search.SearchResult;

import java.util.ArrayList;
import java.util.List;

public class SearchViewModel extends AndroidViewModel {
    private static final String STATE_QUERY = "query";
    // Searching starts once typing pauses this long, rather than on every key.
    private static final long TYPING_PAUSE_MS = 250;

    private final SearchRepository search;
    private final SavedStateHandle state;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable searchNow = this::searchNow;
    private final MutableLiveData<List<SearchResult>> results =
            new MutableLiveData<>(new ArrayList<>());
    // Counts searches, so the answer to an older one that arrives late is ignored.
    private int searchCount;

    public SearchViewModel(@NonNull Application application, @NonNull SavedStateHandle state) {
        super(application);
        this.search = QuickNotesApp.container(application).searchRepository();
        this.state = state;
    }

    public LiveData<List<SearchResult>> getResults() {
        return results;
    }

    public LiveData<Boolean> isIndexing() {
        return search.isIndexing();
    }

    public boolean canSearchByMeaning() {
        return search.canSearchByMeaning();
    }

    @NonNull
    public String getQuery() {
        String query = state.get(STATE_QUERY);
        return query != null ? query : "";
    }

    public void setQuery(@NonNull String query) {
        if (query.equals(getQuery())) {
            return;
        }
        state.set(STATE_QUERY, query);
        handler.removeCallbacks(searchNow);
        handler.postDelayed(searchNow, TYPING_PAUSE_MS);
    }

    // Searches again for the same words: notes may have changed in the editor, or finished
    // indexing.
    public void refresh() {
        searchNow();
    }

    private void searchNow() {
        handler.removeCallbacks(searchNow);
        int number = ++searchCount;
        String query = getQuery();
        if (query.trim().isEmpty()) {
            results.setValue(new ArrayList<>());
            return;
        }
        search.search(query, found -> {
            if (number == searchCount) {
                results.setValue(found);
            }
        });
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacks(searchNow);
    }
}
