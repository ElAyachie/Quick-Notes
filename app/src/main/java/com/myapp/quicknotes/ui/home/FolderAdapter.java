package com.myapp.quicknotes.ui.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.myapp.quicknotes.data.Folder;
import com.myapp.quicknotes.databinding.ItemFolderBinding;

import java.util.function.Consumer;

public class FolderAdapter extends ListAdapter<Folder, FolderAdapter.FolderViewHolder> {
    private final Consumer<Folder> onClick;
    private final Consumer<Folder> onLongClick;

    public FolderAdapter(Consumer<Folder> onClick, Consumer<Folder> onLongClick) {
        super(DIFF);
        this.onClick = onClick;
        this.onLongClick = onLongClick;
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FolderViewHolder(ItemFolderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        Folder folder = getItem(position);
        holder.binding.folderName.setText(folder.getName());
        holder.itemView.setOnClickListener(view -> onClick.accept(folder));
        holder.itemView.setOnLongClickListener(view -> {
            onLongClick.accept(folder);
            return true;
        });
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        final ItemFolderBinding binding;

        FolderViewHolder(ItemFolderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static final DiffUtil.ItemCallback<Folder> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Folder oldItem, @NonNull Folder newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Folder oldItem, @NonNull Folder newItem) {
            return oldItem.equals(newItem);
        }
    };
}
