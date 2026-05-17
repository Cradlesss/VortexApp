package com.project_void.vortexapp.ui.common.help.scripts;

import android.app.Activity;
import android.view.View;
import com.project_void.vortexapp.R;
import com.project_void.vortexapp.ui.common.help.CoachPlacement;
import com.project_void.vortexapp.ui.common.help.CoachScript;
import com.project_void.vortexapp.ui.common.help.CoachStep;
import com.project_void.vortexapp.ui.common.help.CoachTargetResolver;
import java.util.ArrayList;
import java.util.List;

public final class HomeScreenScript implements CoachScript {
    private final Activity a;

    private HomeScreenScript(Activity a) {
        this.a = a;
    }

    public static CoachScript build(Activity a) {
        return new HomeScreenScript(a);
    }

    @Override
    public List<CoachStep> steps() {
        final List<CoachStep> s = new ArrayList<>();
        final View bottomNav = a.findViewById(R.id.bottom_navigation);

        s.add(new CoachStep.Builder()
                .id("home_intro")
                .title(a.getString(R.string.coach_home_intro_title))
                .text(a.getString(R.string.coach_home_intro_text))
                .placement(CoachPlacement.CENTER)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("home_status")
                .title(a.getString(R.string.coach_home_status_title))
                .text(a.getString(R.string.coach_home_status_text))
                .target(CoachTargetResolver.byId(R.id.current_device_card))
                .placement(CoachPlacement.BELOW)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("home_select_device")
                .title(a.getString(R.string.coach_home_select_device_title))
                .text(a.getString(R.string.coach_home_select_device_text))
                .target(CoachTargetResolver.byId(R.id.select_device_frame))
                .placement(CoachPlacement.BELOW)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("home_effects_btn")
                .title(a.getString(R.string.coach_home_effects_title))
                .text(a.getString(R.string.coach_home_effects_text))
                .target(CoachTargetResolver.byId(R.id.effects_btn))
                .placement(CoachPlacement.BELOW)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("home_solid_color_btn")
                .title(a.getString(R.string.coach_home_solid_color_title))
                .text(a.getString(R.string.coach_home_solid_color_text))
                .target(CoachTargetResolver.byId(R.id.solid_color_btn))
                .placement(CoachPlacement.BELOW)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("home_scene_preview")
                .title(a.getString(R.string.coach_home_scene_preview_title))
                .text(a.getString(R.string.coach_home_scene_preview_text))
                .target(CoachTargetResolver.byId(R.id.home_scene_preview_card))
                .placement(CoachPlacement.ABOVE)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("home_suggested")
                .title(a.getString(R.string.coach_home_suggested_title))
                .text(a.getString(R.string.coach_home_suggested_text))
                .target(CoachTargetResolver.byId(R.id.home_suggested_effect_row))
                .placement(CoachPlacement.ABOVE)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("home_brightness")
                .title(a.getString(R.string.coach_home_brightness_title))
                .text(a.getString(R.string.coach_home_brightness_text))
                .target(CoachTargetResolver.byId(R.id.brightness_container))
                .placement(CoachPlacement.ABOVE)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("home_menu_overview")
                .title(a.getString(R.string.coach_home_menu_overview_title))
                .text(a.getString(R.string.coach_home_menu_overview_text))
                .target(t -> bottomNav)
                .bubbleTarget(t -> bottomNav)
                .placement(CoachPlacement.ABOVE)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("home_menu_item_home")
                .title(a.getString(R.string.coach_home_menu_item_home_title))
                .text(a.getString(R.string.coach_home_menu_item_home_text))
                .target(t -> bottomNav.findViewById(R.id.bottom_navigation_home))
                .bubbleTarget(r -> bottomNav)
                .placement(CoachPlacement.ABOVE)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("home_menu_item_effects")
                .title(a.getString(R.string.coach_home_menu_item_effects_title))
                .text(a.getString(R.string.coach_home_menu_item_effects_text))
                .target(r -> bottomNav.findViewById(R.id.bottom_navigation_effects))
                .bubbleTarget(r -> bottomNav)
                .placement(CoachPlacement.ABOVE)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("home_menu_item_solid_color")
                .title(a.getString(R.string.coach_home_menu_item_solid_color_title))
                .text(a.getString(R.string.coach_home_menu_item_solid_color_text))
                .target(t -> bottomNav.findViewById(R.id.bottom_navigation_solid_color))
                .bubbleTarget(r -> bottomNav)
                .placement(CoachPlacement.ABOVE)
                .nudgeX(-12)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("home_menu_item_settings")
                .title(a.getString(R.string.coach_home_menu_item_settings_title))
                .text(a.getString(R.string.coach_home_menu_item_settings_text))
                .target(t -> bottomNav.findViewById(R.id.bottom_navigation_settings))
                .bubbleTarget(r -> bottomNav)
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
