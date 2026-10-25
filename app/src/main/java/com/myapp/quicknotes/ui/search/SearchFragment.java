package com.myapp.quicknotes.ui.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.myapp.quicknotes.R;
import com.myapp.quicknotes.databinding.FragmentSearchBinding;
import com.myapp.quicknotes.search.SearchResult;
import com.myapp.quicknotes.ui.common.Screens;
import com.myapp.quicknotes.ui.editor.NoteEditorViewModel;

import java.util.List;

// Search across all folders. Results appear as the user types, each with the passage that matched.
public class SearchFragment extends Fragment {
    private FragmentSearchBinding binding;
    private SearchViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        SearchResultAdapter adapter = new SearchResultAdapter(this::openNote);
        binding.resultList.setAdapter(adapter);

        // Filled before listening, so restoring the query doesn't count as typing it.
        binding.queryInput.setText(viewModel.getQuery());
        binding.queryInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setQuery(s.toString());
            }
        });
        binding.queryInput.setOnEditorActionListener((field, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                Screens.hideKeyboard(field);
                viewModel.refresh();
                return true;
            }
            return false;
        });
        // Scrolling the results puts the keyboard away, so more of them fit on screen.
        binding.resultList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView list, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    Screens.hideKeyboard(list);
                }
            }
        });
        if (viewModel.getQuery().isEmpty()) {
            binding.queryInput.requestFocus();
            WindowCompat.getInsetsController(requireActivity().getWindow(), binding.queryInput)
                    .show(WindowInsetsCompat.Type.ime());
        }

        viewModel.getResults().observe(getViewLifecycleOwner(), results -> {
            adapter.submitList(results);
            showEmptyState(results);
            showOsmCredit(results);
            showStatus();
        });
        viewModel.isIndexing().observe(getViewLifecycleOwner(), indexing -> {
            showStatus();
            // Also searches again on returning from a note, which may have been changed.
            if (!indexing) {
                viewModel.refresh();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Credit OpenStreetMap while a result shows a place recognised from its data.
    private void showOsmCredit(List<SearchResult> results) {
        boolean shown = false;
        for (SearchResult result : results) {
            if (result.getReminderMatch() != null
                    && result.getReminderMatch().nearbyName != null) {
                shown = true;
                break;
            }
        }
        binding.osmCredit.setVisibility(shown ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(List<SearchResult> results) {
        binding.emptyState.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
        binding.emptyText.setText(viewModel.getQuery().trim().isEmpty()
                ? R.string.search_prompt : R.string.search_no_results);
    }

    private void showStatus() {
        int status = !viewModel.canSearchByMeaning() ? R.string.search_by_words_only
                : Boolean.TRUE.equals(viewModel.isIndexing().getValue()) ? R.string.search_indexing
                : 0;
        binding.searchStatus.setVisibility(status != 0 ? View.VISIBLE : View.GONE);
        if (status != 0) {
            binding.searchStatus.setText(status);
        }
    }

    private void openNote(SearchResult result) {
        Screens.hideKeyboard(binding.queryInput);
        Screens.open(this, R.id.searchFragment, R.id.noteEditorFragment,
                NoteEditorViewModel.args(result.getNoteId(), result.getFolderId()));
    }
}
