package com.project_void.vortexapp.ui.common.help;

import android.view.View;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public final class CoachTargetResolver {
    private CoachTargetResolver() {}

    public static CoachStep.TargetResolver byId(@IdRes int id) {
        return v -> v.findViewById(id);
    }

    public interface ChildSelector {
        @Nullable
        View select(RecyclerView rv);
    }

    public static CoachStep.TargetResolver recyclerChild(@IdRes int recyclerId, ChildSelector selector) {
        return v -> {
            RecyclerView rv = v.findViewById(recyclerId);
            if (rv == null) return null;
            return selector.select(rv);
        };
    }
}
