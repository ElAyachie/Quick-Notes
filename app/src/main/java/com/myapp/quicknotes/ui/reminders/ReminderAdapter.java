package com.myapp.quicknotes.ui.reminders;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.myapp.quicknotes.data.ReminderWithNote;
import com.myapp.quicknotes.databinding.ItemReminderBinding;
import com.myapp.quicknotes.ui.common.Formats;

import java.util.function.Consumer;

public class ReminderAdapter
        extends ListAdapter<ReminderWithNote, ReminderAdapter.ReminderViewHolder> {
    private final Consumer<ReminderWithNote> onClick;
    private final Consumer<ReminderWithNote> onLongClick;

    public ReminderAdapter(Consumer<ReminderWithNote> onClick,
                           Consumer<ReminderWithNote> onLongClick) {
        super(DIFF);
        this.onClick = onClick;
        this.onLongClick = onLongClick;
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ReminderViewHolder(ItemReminderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        ReminderWithNote item = getItem(position);
        holder.binding.noteTitle.setText(item.getNoteTitle());
        holder.binding.reminderTime.setText(Formats.dateTime(item.getReminder().getTriggerAt()));
        holder.itemView.setOnClickListener(view -> onClick.accept(item));
        holder.itemView.setOnLongClickListener(view -> {
            onLongClick.accept(item);
            return true;
        });
    }

    static class ReminderViewHolder extends RecyclerView.ViewHolder {
        final ItemReminderBinding binding;

        ReminderViewHolder(ItemReminderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static final DiffUtil.ItemCallback<ReminderWithNote> DIFF =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull ReminderWithNote oldItem,
                                               @NonNull ReminderWithNote newItem) {
                    return oldItem.getReminder().getId() == newItem.getReminder().getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull ReminderWithNote oldItem,
                                                  @NonNull ReminderWithNote newItem) {
                    return oldItem.equals(newItem);
                }
            };
}
