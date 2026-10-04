package com.myapp.quicknotes.ui.home;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.myapp.quicknotes.R;
import com.myapp.quicknotes.data.Folder;
import com.myapp.quicknotes.data.FolderWithNoteCount;
import com.myapp.quicknotes.databinding.ItemFolderBinding;

import java.util.function.Consumer;

public class FolderAdapter extends ListAdapter<FolderWithNoteCount, FolderAdapter.FolderViewHolder> {
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
        FolderWithNoteCount item = getItem(position);
        Folder folder = item.getFolder();
        Resources resources = holder.itemView.getResources();
        holder.binding.folderName.setText(folder.getName());
        holder.binding.folderCount.setText(item.getNoteCount() == 0
                ? resources.getString(R.string.no_notes_in_folder)
                : resources.getQuantityString(R.plurals.note_count,
                        item.getNoteCount(), item.getNoteCount()));
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

    private static final DiffUtil.ItemCallback<FolderWithNoteCount> DIFF =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull FolderWithNoteCount oldItem,
                                               @NonNull FolderWithNoteCount newItem) {
                    return oldItem.getFolder().getId() == newItem.getFolder().getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull FolderWithNoteCount oldItem,
                                                  @NonNull FolderWithNoteCount newItem) {
                    return oldItem.equals(newItem);
                }
            };
}
