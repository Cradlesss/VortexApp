package com.project_void.vortexapp.ui.selectdevice.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.project_void.vortexapp.R;
import com.project_void.vortexapp.ble.model.BleSessionState;
import com.project_void.vortexapp.ui.selectdevice.device_info.DeviceItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.VH> {
    public interface onClick {
        void onItemClick(DeviceItem item);

        void onSettingsClick(DeviceItem item);
    }

    private final onClick onClick;
    private final List<DeviceItem> items = new ArrayList<>();
    private BleSessionState bleState;

    public DeviceListAdapter(onClick onClick) {
        this.onClick = onClick;
    }

    public void setBleState(BleSessionState state) {
        this.bleState = state;
        notifyDataSetChanged();
    }

    public void submit(List<DeviceItem> newItems) {
        DiffUtil.DiffResult d = DiffUtil.calculateDiff(new Diff(items, newItems));
        items.clear();
        items.addAll(newItems);
        d.dispatchUpdatesTo(this);
    }

    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_select_device, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DeviceItem it = items.get(position);
        h.icon.setImageResource(it.iconRes);
        h.name.setText(it.displayName);
        h.addr.setText(it.address);
        h.status.setText(statusFor(it));
        h.itemView.setOnClickListener(v -> onClick.onItemClick(it));
        h.settings.setOnClickListener(v -> onClick.onSettingsClick(it));
    }

    private String statusFor(DeviceItem it) {
        if (bleState == null) return it.bonded ? "" : v(R.string.not_paired);

        String addr = bleState.deviceAddress();

        if (addr == null || !Objects.equals(addr, it.address))
            return it.bonded ? "" : v(R.string.not_paired);

        return switch (bleState.status()) {
            case CONNECTED -> v(R.string.connected);
            case CONNECTING -> v(R.string.connecting);
            case DISCONNECTING -> v(R.string.disconnecting);
            default -> "";
        };
    }

    private String v(int resId) {
        return items.isEmpty() ? "" : items.get(0).context.getString(resId);
    }

    static final class Diff extends DiffUtil.Callback {
        final List<DeviceItem> a, b;

        Diff(List<DeviceItem> a, List<DeviceItem> b) {
            this.a = a;
            this.b = b;
        }

        public int getOldListSize() {
            return a.size();
        }

        public int getNewListSize() {
            return b.size();
        }

        public boolean areItemsTheSame(int i, int j) {
            return a.get(i).address.equals(b.get(j).address);
        }

        public boolean areContentsTheSame(int i, int j) {
            return a.get(i).equals(b.get(j));
        }
    }

    static final class VH extends RecyclerView.ViewHolder {
        ImageView icon, settings;
        TextView name, addr, status;

        VH(@NonNull View v) {
            super(v);
            icon = v.findViewById(R.id.device_icon);
            name = v.findViewById(R.id.device_name);
            addr = v.findViewById(R.id.device_addr);
            status = v.findViewById(R.id.device_status);
            settings = v.findViewById(R.id.device_settings);
        }
    }

    public List<DeviceItem> snapshot() {
        return new ArrayList<>(items);
    }
}
