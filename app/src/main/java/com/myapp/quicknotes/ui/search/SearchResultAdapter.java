package com.myapp.quicknotes.ui.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.myapp.quicknotes.R;
import com.myapp.quicknotes.databinding.ItemSearchResultBinding;
import com.myapp.quicknotes.search.SearchResult;
import com.myapp.quicknotes.ui.common.Formats;

import java.util.function.Consumer;

public class SearchResultAdapter extends ListAdapter<SearchResult, SearchResultAdapter.ResultViewHolder> {
    private final Consumer<SearchResult> onClick;

    public SearchResultAdapter(Consumer<SearchResult> onClick) {
        super(DIFF);
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ResultViewHolder(ItemSearchResultBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        SearchResult result = getItem(position);
        holder.binding.resultTitle.setText(result.getTitle());
        holder.binding.resultSnippet.setText(result.getSnippet());
        holder.binding.resultSnippet.setVisibility(
                result.getSnippet().isEmpty() ? View.GONE : View.VISIBLE);
        holder.binding.resultDetails.setText(holder.itemView.getContext().getString(
                R.string.search_result_details, result.getFolderName(),
                Formats.date(result.getUpdatedAt())));
        holder.itemView.setOnClickListener(view -> onClick.accept(result));
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        final ItemSearchResultBinding binding;

        ResultViewHolder(ItemSearchResultBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static final DiffUtil.ItemCallback<SearchResult> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull SearchResult oldItem,
                                       @NonNull SearchResult newItem) {
            return oldItem.getNoteId() == newItem.getNoteId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull SearchResult oldItem,
                                          @NonNull SearchResult newItem) {
            return oldItem.equals(newItem);
        }
    };
}
