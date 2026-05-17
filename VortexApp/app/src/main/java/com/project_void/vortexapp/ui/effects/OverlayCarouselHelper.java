package com.project_void.vortexapp.ui.effects;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;
import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;
import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.project_void.vortexapp.R;
import java.util.function.IntConsumer;

public class OverlayCarouselHelper extends RecyclerView.OnScrollListener {
    private final RecyclerView rv;
    private final View overlay;
    private final IntConsumer onSelectedChanged;
    private final int[] tmp = new int[2];
    private int lastSelected = NO_POSITION;
    private int lastBestAbsDy = Integer.MAX_VALUE;
    private int lastScrollDir = 0;
    private boolean snapping = false;
    private static final float MIN_SCALE = 0.75f;
    private static final float ACTIVE_ALPHA = 1f;
    private static final float INACTIVE_ALPHA = 0.55f;
    private static final float CURVE = 1.4f;
    private final int SWITCH_HYSTERESIS_PX = dp(8);
    private final int SNAP_THRESHOLD_PX = dp(1);
    private int INFLUENCE_PX = 0;

    public OverlayCarouselHelper(RecyclerView rv, View overlay, @Nullable IntConsumer onSelectedChanged) {
        this.rv = rv;
        this.overlay = overlay;
        this.onSelectedChanged = onSelectedChanged;
    }

    public int getSelectedAdapterPosition() {
        return lastSelected;
    }

    public void centerOn(int adapterPosition) {
        View target = findChildForAdapterPos(adapterPosition);
        if (target == null) {
            rv.scrollToPosition(adapterPosition);
            rv.post(() -> smoothAlign(adapterPosition));
        } else
            smoothAlign(adapterPosition);
    }

    public void recompute() {
        int overlayH = overlay.getHeight();
        int rvH = rv.getHeight();
        if (overlayH == 0 && rvH == 0) {
            rv.post(this::recompute);
            return;
        }
        INFLUENCE_PX = Math.max((int) (rv.getHeight() * 0.55f), 1);
        updateChildTransformsAndSelection();
    }

    public void maybeSnap() {
        if (rv.getScrollState() != SCROLL_STATE_IDLE || snapping || lastSelected == NO_POSITION)
            return;
        View c = findChildForAdapterPos(lastSelected);
        if (c == null) return;
        int off = centerYInRvSpace(c) - overlayCenterInRvSpace();
        if (Math.abs(off) > SNAP_THRESHOLD_PX) {
            snapping = true;
            rv.smoothScrollBy(0, off);
        }
    }

    @Override
    public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
        lastScrollDir = Integer.signum(dy);
        updateChildTransformsAndSelection();
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
        if (newState == SCROLL_STATE_IDLE) {
            snapping = false;
            maybeSnap();
        }
    }

    private void updateChildTransformsAndSelection() {
        if (INFLUENCE_PX == 0) return;

        int ovCy = overlayCenterInRvSpace();
        int childCount = rv.getChildCount();

        if (childCount == 0) return;

        int bestPos = NO_POSITION;
        int bestAbsDy = Integer.MAX_VALUE;

        for (int i = 0; i < childCount; i++) {
            View c = rv.getChildAt(i);
            int childCy = centerYInRvSpace(c);
            int dy = childCy - ovCy;
            int absDy = Math.abs(dy);

            float t = Math.min(1f, absDy / (float) INFLUENCE_PX);
            float w = (float) Math.pow(1f - t, CURVE);
            float scale = MIN_SCALE + (1f - MIN_SCALE) * w;

            View target = c.findViewById(R.id.card);
            if (target == null) target = c;

            target.setPivotX(target.getWidth() * 0.5f);
            target.setPivotY(target.getHeight() * 0.5f);

            target.setScaleX(scale);
            target.setScaleY(scale);

            if (absDy < bestAbsDy) {
                bestAbsDy = absDy;
                RecyclerView.ViewHolder vh = rv.getChildViewHolder(c);
                if (vh != null) bestPos = vh.getBindingAdapterPosition();
            }
        }

        if (bestPos == NO_POSITION) return;

        if (bestPos != lastSelected) {
            boolean improvedEnough = (bestAbsDy + SWITCH_HYSTERESIS_PX) < lastBestAbsDy;
            if (!improvedEnough) {
                if (lastScrollDir != 0) {
                    return;
                }
            }
            lastSelected = bestPos;
            lastBestAbsDy = bestAbsDy;
            if (onSelectedChanged != null) rv.post(() -> onSelectedChanged.accept(lastSelected));
        } else {
            lastBestAbsDy = bestAbsDy;
        }

        applyDiscreteAlpha();
    }

    private void smoothAlign(int adapterPos) {
        View target = findChildForAdapterPos(adapterPos);
        if (target == null) {
            rv.scrollToPosition(adapterPos);
            rv.post(() -> {
                View t2 = findChildForAdapterPos(adapterPos);
                if (t2 != null) smoothAlignView(t2);
            });
            return;
        }
        smoothAlignView(target);
    }

    private void smoothAlignView(View c) {
        int dy = centerYInRvSpace(c) - overlayCenterInRvSpace();
        if (dy != 0) {
            snapping = true;
            rv.smoothScrollBy(0, dy);
        }
    }

    private void applyDiscreteAlpha() {
        int childCount = rv.getChildCount();
        if (childCount == 0) return;

        for (int i = 0; i < childCount; i++) {
            View item = rv.getChildAt(i);
            RecyclerView.ViewHolder vh = rv.getChildViewHolder(item);
            int pos = (vh != null) ? vh.getBindingAdapterPosition() : NO_POSITION;

            View t = item.findViewById(R.id.card);
            if (t == null) t = item;

            t.setAlpha(pos == lastSelected ? ACTIVE_ALPHA : INACTIVE_ALPHA);

            TextView tw = item.findViewById(R.id.effect_title);
            if (tw == null) tw = item.findViewById(R.id.effect_title);
            if (tw != null) tw.setAlpha(pos == lastSelected ? ACTIVE_ALPHA : INACTIVE_ALPHA);
        }
    }

    private View findChildForAdapterPos(int adapterPos) {
        RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(adapterPos);
        return (vh != null) ? vh.itemView : null;
    }

    private int topOnScreen(View v) {
        v.getLocationOnScreen(tmp);
        return tmp[1];
    }

    private int centerYInRvSpace(View c) {
        int rvTop = topOnScreen(rv);
        int childTop = topOnScreen(c);
        int childTopInRv = childTop - rvTop;
        return childTopInRv + c.getHeight() / 2;
    }

    private int overlayCenterInRvSpace() {
        int rvTop = topOnScreen(rv);
        int overlayTop = topOnScreen(overlay);
        int overlayTopInRv = overlayTop - rvTop;
        return overlayTopInRv + overlay.getHeight() / 2;
    }

    private static int dp(int v) {
        return (int) (v * Resources.getSystem().getDisplayMetrics().density + 0.5f);
    }
}
