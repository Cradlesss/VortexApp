package com.project_void.vortexapp.ui.common.help.scripts;

import android.app.Activity;
import com.project_void.vortexapp.R;
import com.project_void.vortexapp.ui.common.help.CoachPlacement;
import com.project_void.vortexapp.ui.common.help.CoachScript;
import com.project_void.vortexapp.ui.common.help.CoachStep;
import com.project_void.vortexapp.ui.common.help.CoachTargetResolver;
import java.util.ArrayList;
import java.util.List;

public final class EffectsScreenScript implements CoachScript {
    private final Activity a;

    private EffectsScreenScript(Activity a) {
        this.a = a;
    }

    public static CoachScript build(Activity a) {
        return new EffectsScreenScript(a);
    }

    @Override
    public List<CoachStep> steps() {
        final List<CoachStep> s = new ArrayList<>();

        s.add(new CoachStep.Builder()
                .id("effects_intro")
                .title(a.getString(R.string.coach_effects_intro_title))
                .text(a.getString(R.string.coach_effects_intro_text))
                .placement(CoachPlacement.CENTER)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("effects_list")
                .title(a.getString(R.string.coach_effects_list_title))
                .text(a.getString(R.string.coach_effects_list_text))
                .target(CoachTargetResolver.byId(R.id.effects_recycler))
                .placement(CoachPlacement.ABOVE)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("effects_focus")
                .title(a.getString(R.string.coach_effects_focus_title))
                .text(a.getString(R.string.coach_effects_focus_text))
                .target(CoachTargetResolver.byId(R.id.effects_overlay))
                .placement(CoachPlacement.ABOVE)
                .build()
        );

        return s;
    }

    @Override
    public boolean allowSkipWhenTargetMissing() {
        return false;
    }
}
