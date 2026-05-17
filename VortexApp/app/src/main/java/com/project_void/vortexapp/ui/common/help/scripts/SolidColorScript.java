package com.project_void.vortexapp.ui.common.help.scripts;

import android.app.Activity;
import com.project_void.vortexapp.R;
import com.project_void.vortexapp.ui.common.help.CoachPlacement;
import com.project_void.vortexapp.ui.common.help.CoachScript;
import com.project_void.vortexapp.ui.common.help.CoachStep;
import com.project_void.vortexapp.ui.common.help.CoachTargetResolver;
import java.util.ArrayList;
import java.util.List;

public final class SolidColorScript implements CoachScript {
    private final Activity a;

    private SolidColorScript(Activity a) {
        this.a = a;
    }

    public static CoachScript build(Activity a) {
        return new SolidColorScript(a);
    }

    @Override
    public List<CoachStep> steps() {
        final List<CoachStep> s = new ArrayList<>();

        s.add(new CoachStep.Builder()
                .id("solid_intro")
                .title(a.getString(R.string.coach_solid_intro_title))
                .text(a.getString(R.string.coach_solid_intro_text))
                .placement(CoachPlacement.CENTER)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("solid_preview")
                .title(a.getString(R.string.coach_solid_preview_title))
                .text(a.getString(R.string.coach_solid_preview_text))
                .target(CoachTargetResolver.byId(R.id.color_preview))
                .placement(CoachPlacement.ABOVE)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("solid_picker")
                .title(a.getString(R.string.coach_solid_picker_title))
                .text(a.getString(R.string.coach_solid_picker_text))
                .target(CoachTargetResolver.byId(R.id.color_picker_view))
                .placement(CoachPlacement.ABOVE)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("solid_hex")
                .title(a.getString(R.string.coach_solid_hex_title))
                .text(a.getString(R.string.coach_solid_hex_text))
                .target(CoachTargetResolver.byId(R.id.hex_input))
                .placement(CoachPlacement.BELOW)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("solid_rgb")
                .title(a.getString(R.string.coach_solid_rgb_title))
                .text(a.getString(R.string.coach_solid_rgb_text))
                .target(CoachTargetResolver.byId(R.id.rgbContainer))
                .placement(CoachPlacement.BELOW)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("solid_apply")
                .title(a.getString(R.string.coach_solid_apply_title))
                .text(a.getString(R.string.coach_solid_apply_text))
                .target(CoachTargetResolver.byId(R.id.apply_color_button))
                .placement(CoachPlacement.ABOVE)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("solid_saved")
                .title(a.getString(R.string.coach_solid_saved_title))
                .text(a.getString(R.string.coach_solid_saved_text))
                .target(CoachTargetResolver.byId(R.id.saved_colors_grid))
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
