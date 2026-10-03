package com.myapp.quicknotes.ui.reminders;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.myapp.quicknotes.R;
import com.myapp.quicknotes.data.ReminderType;
import com.myapp.quicknotes.data.ReminderWithNote;
import com.myapp.quicknotes.databinding.ItemReminderBinding;
import com.myapp.quicknotes.ui.common.Formats;
import com.myapp.quicknotes.ui.common.ReminderText;

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
        Context context = holder.itemView.getContext();
        holder.binding.reminderIcon.setImageResource(
                item.getReminder().getType() == ReminderType.LOCATION
                        ? R.drawable.ic_place : R.drawable.ic_schedule);
        holder.binding.reminderTrigger.setText(ReminderText.trigger(context, item.getReminder()));
        // Only history entries say when they fired.
        Long firedAt = item.getReminder().getFiredAt();
        if (firedAt == null) {
            holder.binding.firedText.setVisibility(View.GONE);
        } else {
            holder.binding.firedText.setText(
                    context.getString(R.string.reminded_on, Formats.dateTime(firedAt)));
            holder.binding.firedText.setVisibility(View.VISIBLE);
        }
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
