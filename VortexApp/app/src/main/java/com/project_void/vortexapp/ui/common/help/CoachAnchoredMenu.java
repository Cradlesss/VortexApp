package com.project_void.vortexapp.ui.common.help;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.PopupWindow;
import androidx.annotation.LayoutRes;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.project_void.vortexapp.R;

public class CoachAnchoredMenu {
    private CoachAnchoredMenu() {
    }

    public static View show(ViewGroup root, View anchor, @LayoutRes int layoutId, View existing) {
        if (root.getWidth() == 0 || root.getHeight() == 0) {
            root.post(() -> show(root, anchor, layoutId, existing));
            return existing != null ? existing : new View(root.getContext());
        }

        final Context ctx = root.getContext();
        final LayoutInflater inflater = LayoutInflater.from(ctx);

        final MenuSize size = (view) -> {
            final int unspec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int maxChild = 0;
            if (view instanceof ViewGroup vg) {
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View c = vg.getChildAt(i);
                    c.measure(unspec, unspec);
                    maxChild = Math.max(maxChild, c.getMeasuredWidth());
                }
            } else {
                view.measure(unspec, unspec);
                maxChild = view.getMeasuredWidth();
            }

            int pad = view.getPaddingLeft() + view.getPaddingRight();
            android.graphics.Rect bg = new android.graphics.Rect();
            android.graphics.drawable.Drawable bgd = view.getBackground();
            if (bgd != null && bgd.getPadding(bg)) pad += bg.left + bg.right;

            int contentW = maxChild + pad + dp(root, 30);
            int screenCap = root.getResources().getDisplayMetrics().widthPixels - dp(root, 32);
            int designCap = (int) ctx.getResources().getDimension(R.dimen.popup_max_width);
            int popupW = Math.min(Math.min(contentW, screenCap), designCap);
            popupW = Math.max(popupW, dp(root, 40));

            int wExact = View.MeasureSpec.makeMeasureSpec(popupW, View.MeasureSpec.EXACTLY);
            int hUnspec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            view.measure(wExact, hUnspec);
            int popupH = Math.max(view.getMeasuredHeight(), dp(root, 40));

            return new int[]{popupW, popupH};
        };

        Insets allInsets = Insets.NONE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsets wi = root.getRootWindowInsets();
            if (wi != null) {
                android.graphics.Insets s = wi.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                allInsets = Insets.of(s.left, s.top, s.right, s.bottom);
            }
        } else {
            WindowInsetsCompat wi = ViewCompat.getRootWindowInsets(root);
            if (wi != null)
                allInsets = wi.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
        }

        if (existing != null) {
            Object tag = existing.getTag();
            int[] anchorScr = new int[2];
            int[] rootScr = new int[2];
            anchor.getLocationOnScreen(anchorScr);
            root.getLocationOnScreen(rootScr);

            Rect anchRect = new Rect(
                    anchorScr[0] - rootScr[0],
                    anchorScr[1] - rootScr[1],
                    anchorScr[0] - rootScr[0] + anchor.getWidth(),
                    anchorScr[1] - rootScr[1] + anchor.getHeight()
            );

            int[] sz = size.measure(existing);
            int popupW = sz[0];
            int popupH = sz[1];

            int x = anchRect.right - popupW;
            int y = anchRect.bottom + (int) ctx.getResources().getDimension(R.dimen.coach_popup_y_offset);

            x = Math.max(allInsets.left, Math.min(x, root.getWidth() - popupW - allInsets.right));
            y = Math.max(allInsets.top, Math.min(y, root.getHeight() - popupH - allInsets.bottom));

            int xScreen = rootScr[0] + x;
            int yScreen = rootScr[1] + y;

            if (tag instanceof PopupWindow) {
                ((PopupWindow) tag).update(xScreen, yScreen, popupW, popupH);
            } else {
                existing.setX(x);
                existing.setY(y);
                if (existing.getParent() != root) root.addView(existing);
            }
            return existing;
        }

        View menu = inflater.inflate(layoutId, null, false);
        int[] sz = size.measure(menu);
        int popupW = sz[0];
        int popupH = sz[1];

        if (menu instanceof ViewGroup vg) {
            for (int i = 0; i < vg.getChildCount(); i++) {
                View c = vg.getChildAt(i);
                if (c.getId() == R.id.coach_menu_refresh || c.getId() == R.id.coach_menu_sandbox || c.getId() == R.id.coach_menu_bt || c.getId() == R.id.coach_menu_help) {
                    ViewGroup.LayoutParams lp = c.getLayoutParams();
                    if (lp == null)
                        lp = new ViewGroup.LayoutParams(popupW, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.width = popupW;
                    c.setLayoutParams(lp);
                }
            }
        }

        int[] anchorScr = new int[2];
        int[] rootScr = new int[2];
        anchor.getLocationOnScreen(anchorScr);
        root.getLocationOnScreen(rootScr);

        Rect anchRect = new Rect(
                anchorScr[0] - rootScr[0],
                anchorScr[1] - rootScr[1],
                anchorScr[0] - rootScr[0] + anchor.getWidth(),
                anchorScr[1] - rootScr[1] + anchor.getHeight()
        );

        int x = anchRect.right - popupW;
        int y = anchRect.bottom + (int) ctx.getResources().getDimension(R.dimen.coach_popup_y_offset);

        x = Math.max(allInsets.left, Math.min(x, root.getWidth() - popupW - allInsets.right));
        y = Math.max(allInsets.top, Math.min(y, root.getHeight() - popupH - allInsets.bottom));

        int xScreen = rootScr[0] + x;
        int yScreen = rootScr[1] + y;

        boolean isCoachMenu = layoutId == R.layout.view_select_device_options_popup_coach;

        if (!isCoachMenu) {
            PopupWindow pw = new PopupWindow(
                    menu,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    false
            );
            pw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            pw.setElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10,
                    ctx.getResources().getDisplayMetrics()));
            pw.setClippingEnabled(false);
            pw.setWidth(popupW);
            pw.setHeight(popupH);
            pw.showAtLocation(root, Gravity.START | Gravity.TOP, xScreen, yScreen);
            menu.setTag(pw);
        } else {
            ViewGroup.LayoutParams lp = menu.getLayoutParams();
            if (lp == null)
                lp = new ViewGroup.LayoutParams(popupW, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.width = popupW;
            menu.setLayoutParams(lp);
            menu.setX(x);
            menu.setY(y);
            root.addView(menu);
        }

        return menu;
    }

    public static void dismiss(ViewGroup root, View menu) {
        if (menu == null) return;
        Object tag = menu.getTag();
        if (tag instanceof PopupWindow) ((PopupWindow) tag).dismiss();
        if (root != null) root.removeView(menu);
    }

    private static int dp(View v, int dp) {
        return Math.round(dp * v.getResources().getDisplayMetrics().density);
    }

    private interface MenuSize {
        int[] measure(View v);
    }
}
