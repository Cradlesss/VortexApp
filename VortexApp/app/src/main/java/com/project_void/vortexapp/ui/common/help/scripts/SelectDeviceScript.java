package com.project_void.vortexapp.ui.common.help.scripts;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import com.project_void.vortexapp.R;
import com.project_void.vortexapp.ui.common.help.CoachPlacement;
import com.project_void.vortexapp.ui.common.help.CoachScript;
import com.project_void.vortexapp.ui.common.help.CoachStep;
import com.project_void.vortexapp.ui.common.help.CoachTargetResolver;
import com.project_void.vortexapp.ui.common.help.CoachAnchoredMenu;
import com.project_void.vortexapp.ui.selectdevice.SelectDeviceActivity;
import java.util.ArrayList;
import java.util.List;

public final class SelectDeviceScript implements CoachScript {
    private final Activity a;
    private View fakeMenu;

    private SelectDeviceScript(Activity a) {
        this.a = a;
    }

    public static CoachScript build(Activity a) {
        return new SelectDeviceScript(a);
    }

    @Override
    public List<CoachStep> steps() {
        final List<CoachStep> s = new ArrayList<>();
        View anchor = a.findViewById(R.id.select_device_burger_menu_button);
        ViewGroup root = a.findViewById(R.id.root_layout_select_device);

        s.add(new CoachStep.Builder()
                .id("select_intro")
                .title(a.getString(R.string.coach_select_device_intro_title))
                .text(a.getString(R.string.coach_select_device_intro_text))
                .placement(CoachPlacement.CENTER)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("row")
                .title(a.getString(R.string.coach_select_device_row_title))
                .text(a.getString(R.string.coach_select_device_row_text))
                .before(() -> {
                    if (a instanceof SelectDeviceActivity act) {
                        act.enterDevicesTutorial();
                    }
                })
                .target(firstRow(R.id.select_device_device_container))
                .placement(CoachPlacement.BELOW)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("name")
                .title(a.getString(R.string.coach_select_device_name_title))
                .text(a.getString(R.string.coach_select_device_name_text))
                .target(firstChildViewInRecycler(
                        R.id.select_device_device_container,
                        R.id.device_name))
                .placement(CoachPlacement.BELOW)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("addr")
                .title(a.getString(R.string.coach_select_device_addr_title))
                .text(a.getString(R.string.coach_select_device_addr_text))
                .target(firstChildViewInRecycler(
                        R.id.select_device_device_container,
                        R.id.device_addr))
                .placement(CoachPlacement.BELOW)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("status")
                .title(a.getString(R.string.coach_select_device_status_title))
                .text(a.getString(R.string.coach_select_device_status_text))
                .target(firstChildViewInRecycler(
                        R.id.select_device_device_container,
                        R.id.device_status))
                .placement(CoachPlacement.BELOW)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("actions")
                .title(a.getString(R.string.coach_select_device_actions_title))
                .text(a.getString(R.string.coach_select_device_actions_text))
                .target(firstChildViewInRecycler(
                        R.id.select_device_device_container,
                        R.id.device_settings))
                .placement(CoachPlacement.AUTO)
                .onNext(() -> {
                    if (a instanceof SelectDeviceActivity act) {
                        act.exitDevicesTutorial();
                    }
                })
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("menu")
                .title(a.getString(R.string.coach_select_device_menu_title))
                .text(a.getString(R.string.coach_select_device_menu_text))
                .before(() -> {
                    if (anchor != null && root != null) {
                        fakeMenu = CoachAnchoredMenu.show(
                                root,
                                anchor,
                                R.layout.view_select_device_options_popup_coach,
                                fakeMenu
                        );
                    }
                })
                .onPrev(() -> {
                    if (root != null && fakeMenu != null) {
                        CoachAnchoredMenu.dismiss(root, fakeMenu);
                        fakeMenu = null;
                    }
                    if (anchor != null) {
                        Object o = anchor.getTag(R.id.coach_saved_click);
                        if (o instanceof View.OnClickListener)
                            anchor.setOnClickListener((View.OnClickListener) o);
                        anchor.setTag(R.id.coach_saved_click, null);
                    }
                })
                .onCancel(() -> {
                    if (root != null && fakeMenu != null) {
                        CoachAnchoredMenu.dismiss(root, fakeMenu);
                    }
                    if (anchor != null) {
                        Object o = anchor.getTag(R.id.coach_saved_click);
                        if (o instanceof View.OnClickListener)
                            anchor.setOnClickListener((View.OnClickListener) o);
                        anchor.setTag(R.id.coach_saved_click, null);
                    }
                })
                .target(r -> fakeMenu)
                .bubbleTarget(r -> isAttached(fakeMenu) ? fakeMenu : null)
                .placement(CoachPlacement.BELOW)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("refresh")
                .title(a.getString(R.string.coach_select_device_refresh_title))
                .text(a.getString(R.string.coach_select_device_refresh_text))
                .before(() -> {
                    if (anchor != null && root != null) {
                        fakeMenu = CoachAnchoredMenu.show(
                                root, anchor, R.layout.view_select_device_options_popup_coach, fakeMenu);
                    }
                })
                .onCancel(() -> {
                    if (root != null && fakeMenu != null) {
                        CoachAnchoredMenu.dismiss(root, fakeMenu);
                    }
                    if (anchor != null) {
                        Object o = anchor.getTag(R.id.coach_saved_click);
                        if (o instanceof View.OnClickListener)
                            anchor.setOnClickListener((View.OnClickListener) o);
                        anchor.setTag(R.id.coach_saved_click, null);
                    }
                })
                .target(r -> isAttached(fakeMenu) ? fakeMenu.findViewById(R.id.coach_menu_refresh) : null)
                .bubbleTarget(r -> isAttached(fakeMenu) ? fakeMenu : null)
                .placement(CoachPlacement.BELOW)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("sandbox")
                .title(a.getString(R.string.coach_select_device_sandbox_title))
                .text(a.getString(R.string.coach_select_device_sandbox_text))
                .before(() -> {
                    if (anchor != null && root != null) {
                        fakeMenu = CoachAnchoredMenu.show(
                                root, anchor, R.layout.view_select_device_options_popup_coach, fakeMenu);
                    }
                })
                .onCancel(() -> {
                    if (root != null && fakeMenu != null) {
                        CoachAnchoredMenu.dismiss(root, fakeMenu);
                    }
                    if (anchor != null) {
                        Object o = anchor.getTag(R.id.coach_saved_click);
                        if (o instanceof View.OnClickListener)
                            anchor.setOnClickListener((View.OnClickListener) o);
                        anchor.setTag(R.id.coach_saved_click, null);
                    }
                })
                .target(r -> isAttached(fakeMenu) ? fakeMenu.findViewById(R.id.coach_menu_sandbox) : null)
                .bubbleTarget(r -> isAttached(fakeMenu) ? fakeMenu : null)
                .placement(CoachPlacement.BELOW)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("bt_settings")
                .title(a.getString(R.string.coach_select_device_bt_title))
                .text(a.getString(R.string.coach_select_device_bt_text))
                .before(() -> {
                    if (anchor != null && root != null) {
                        fakeMenu = CoachAnchoredMenu.show(
                                root, anchor, R.layout.view_select_device_options_popup_coach, fakeMenu);
                    }
                })
                .onCancel(() -> {
                    if (root != null && fakeMenu != null) {
                        CoachAnchoredMenu.dismiss(root, fakeMenu);
                    }
                    if (anchor != null) {
                        Object o = anchor.getTag(R.id.coach_saved_click);
                        if (o instanceof View.OnClickListener)
                            anchor.setOnClickListener((View.OnClickListener) o);
                        anchor.setTag(R.id.coach_saved_click, null);
                    }
                })
                .target(r -> isAttached(fakeMenu) ? fakeMenu.findViewById(R.id.coach_menu_bt) : null)
                .bubbleTarget(r -> isAttached(fakeMenu) ? fakeMenu : null)
                .placement(CoachPlacement.BELOW)
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("help")
                .title(a.getString(R.string.coach_select_device_help_title))
                .text(a.getString(R.string.coach_select_device_help_text))
                .before(() -> {
                    if (anchor != null && root != null) {
                        fakeMenu = CoachAnchoredMenu.show(
                                root, anchor, R.layout.view_select_device_options_popup_coach, fakeMenu);
                    }
                })
                .target(r -> isAttached(fakeMenu) ? fakeMenu.findViewById(R.id.coach_menu_help) : null)
                .bubbleTarget(r -> isAttached(fakeMenu) ? fakeMenu : null)
                .placement(CoachPlacement.BELOW)
                .onNext(() -> {
                    if (anchor != null) {
                        Object o = anchor.getTag(R.id.coach_saved_click);
                        if (o instanceof View.OnClickListener)
                            anchor.setOnClickListener((View.OnClickListener) o);
                        anchor.setTag(R.id.coach_saved_click, null);
                    }
                })
                .onCancel(() -> {
                    if (root != null && fakeMenu != null) {
                        CoachAnchoredMenu.dismiss(root, fakeMenu);
                    }
                    if (anchor != null) {
                        Object o = anchor.getTag(R.id.coach_saved_click);
                        if (o instanceof View.OnClickListener)
                            anchor.setOnClickListener((View.OnClickListener) o);
                        anchor.setTag(R.id.coach_saved_click, null);
                    }
                })
                .build()
        );

        s.add(new CoachStep.Builder()
                .id("scan")
                .before(() -> {
                    if (anchor != null && root != null) {
                        CoachAnchoredMenu.dismiss(root, fakeMenu);
                        fakeMenu = null;
                    }
                })
                .title(a.getString(R.string.coach_select_device_scan_title))
                .text(a.getString(R.string.coach_select_device_scan_text))
                .target(CoachTargetResolver.byId(R.id.select_device_scan_button))
                .placement(CoachPlacement.AUTO)
                .build()
        );

        return s;
    }

    @Override
    public boolean allowSkipWhenTargetMissing() {
        return false;
    }

    private static CoachStep.TargetResolver firstRow(int recyclerId) {
        return CoachTargetResolver.recyclerChild(recyclerId, rv -> {
            if (rv.getChildCount() == 0) return null;
            return rv.getChildAt(0);
        });
    }

    private static CoachStep.TargetResolver firstChildViewInRecycler(int recyclerId, int childId) {
        return CoachTargetResolver.recyclerChild(recyclerId, rv -> {
            if (rv.getChildCount() == 0) return null;
            View row = rv.getChildAt(0);
            return (row != null) ? row.findViewById(childId) : null;
        });
    }

    private static boolean isAttached(View v) {
        return v != null && v.getWindowToken() != null && v.isAttachedToWindow();
    }
}
