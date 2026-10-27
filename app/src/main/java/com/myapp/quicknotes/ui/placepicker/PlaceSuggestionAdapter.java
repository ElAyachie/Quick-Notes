package com.myapp.quicknotes.ui.placepicker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.myapp.quicknotes.R;
import com.myapp.quicknotes.databinding.ItemPlaceSuggestionBinding;
import com.myapp.quicknotes.places.PlaceSuggestion;
import com.myapp.quicknotes.ui.common.Formats;

import java.util.Objects;
import java.util.function.Consumer;

// The places in the search list. Tap chooses one; long press offers to save it, or to rename or
// remove a saved one.
public class PlaceSuggestionAdapter
        extends ListAdapter<PlaceSuggestion, PlaceSuggestionAdapter.PlaceViewHolder> {
    private final Consumer<PlaceSuggestion> onClick;
    private final Consumer<PlaceSuggestion> onLongClick;

    public PlaceSuggestionAdapter(Consumer<PlaceSuggestion> onClick,
                                  Consumer<PlaceSuggestion> onLongClick) {
        super(DIFF);
        this.onClick = onClick;
        this.onLongClick = onLongClick;
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PlaceViewHolder(ItemPlaceSuggestionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        PlaceSuggestion place = getItem(position);
        holder.binding.placeIcon.setImageResource(iconFor(place.getKind()));
        holder.binding.placeTitle.setText(place.getName());
        holder.binding.placeAddress.setText(place.getAddress());
        holder.binding.placeAddress.setVisibility(
                place.getAddress() != null ? View.VISIBLE : View.GONE);
        Integer distance = place.getDistanceMeters();
        holder.binding.placeDistance.setText(distance != null
                ? Formats.distance(holder.itemView.getContext(), distance) : null);
        holder.binding.placeDistance.setVisibility(distance != null ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(view -> onClick.accept(place));
        holder.itemView.setOnLongClickListener(view -> {
            onLongClick.accept(place);
            return true;
        });
    }

    static int iconFor(PlaceSuggestion.Kind kind) {
        switch (kind) {
            case SAVED:
                return R.drawable.ic_bookmark;
            case RECENT:
                return R.drawable.ic_history;
            default:
                return R.drawable.ic_place;
        }
    }

    static class PlaceViewHolder extends RecyclerView.ViewHolder {
        final ItemPlaceSuggestionBinding binding;

        PlaceViewHolder(ItemPlaceSuggestionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static final DiffUtil.ItemCallback<PlaceSuggestion> DIFF =
            new DiffUtil.ItemCallback<PlaceSuggestion>() {
                @Override
                public boolean areItemsTheSame(@NonNull PlaceSuggestion a,
                                               @NonNull PlaceSuggestion b) {
                    return a.getKind() == b.getKind() && a.getName().equals(b.getName())
                            && Objects.equals(a.getAddress(), b.getAddress());
                }

                @Override
                public boolean areContentsTheSame(@NonNull PlaceSuggestion a,
                                                  @NonNull PlaceSuggestion b) {
                    return a.equals(b);
                }
            };
}
