package com.project_void.vortexapp.ui.effects;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.project_void.vortexapp.R;
import com.project_void.vortexapp.domain.effects.Effect;

import java.util.List;

final class EffectsAdapter extends RecyclerView.Adapter<EffectsAdapter.VH> {
    interface OnItemClick {
        void onItemClick(int adapterPos, Effect effect);
    }

    private final List<Effect> data;
    private final OnItemClick onClick;
    private int selected = RecyclerView.NO_POSITION;

    EffectsAdapter(List<Effect> data, OnItemClick onClick) {
        this.data = data;
        this.onClick = onClick;
        setHasStableIds(true);
    }

    void setSelected(int pos) {
        if (pos == selected) return;
        int old = selected;
        selected = pos;
        if (old != RecyclerView.NO_POSITION) notifyItemChanged(old, "sel");
        if (selected != RecyclerView.NO_POSITION) notifyItemChanged(selected, "sel");
    }

    @Override
    public long getItemId(int position) {
        return data.get(position).id();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_effect_button, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Effect e = data.get(position);
        h.title.setText(e.name());
        boolean isSelected = (position == selected);
        h.itemView.setAlpha(isSelected ? 1.0f : 0.7f);
        h.itemView.setScaleX(isSelected ? 1.0f : 0.92f);
        h.itemView.setScaleY(isSelected ? 1.0f : 0.92f);

        h.itemView.setOnClickListener(v -> onClick.onItemClick(h.getBindingAdapterPosition(), e));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            boolean isSelected = (position == selected);
            holder.itemView.setAlpha(isSelected ? 1.0f : 0.7f);
            holder.itemView.setScaleX(isSelected ? 1.0f : 0.92f);
            holder.itemView.setScaleY(isSelected ? 1.0f : 0.92f);
            return;
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static final class VH extends RecyclerView.ViewHolder {
        final TextView title;

        VH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.effect_title);
        }
    }
}
