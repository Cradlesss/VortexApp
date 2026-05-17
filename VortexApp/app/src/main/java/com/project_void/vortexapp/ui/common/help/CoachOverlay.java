package com.project_void.vortexapp.ui.common.help;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import com.project_void.vortexapp.BuildConfig;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.android.material.button.MaterialButton;
import com.project_void.vortexapp.R;
import java.util.List;

public final class CoachOverlay extends FrameLayout {
    private final static String TAG = "ui/common/help/CoachOverlay";
    private CoachScript script;
    private List<CoachStep> steps;
    private int index = -1;
    private int safeTop = 0;
    private int safeBottom = 0;
    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path dimPath = new Path();
    private final RectF hole = new RectF();
    private final Path holePath = new Path();
    private View callout;
    private View rootForTargets;

    public CoachOverlay(Context ctx, AttributeSet a) {
        super(ctx, a);
        init();
    }

    public CoachOverlay(Context ctx) {
        super(ctx);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        dimPaint.setColor(0xB3000000);
        setClickable(true);

        setLayerType(LAYER_TYPE_HARDWARE, null);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    public void start(Activity act, ViewGroup attachTo, CoachScript s) {
        this.rootForTargets = attachTo;
        this.script = s;
        this.steps = s.steps();

        attachTo.addView(this, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        next();
    }

    public void next() {
        if (steps == null) return;

        if (index >= 0 && index < steps.size()) {
            CoachStep c = steps.get(index);
            if (c.onNext != null) c.onNext.run();
        }

        index++;
        if (index >= steps.size()) {
            dismiss();
            return;
        }

        setStep(steps.get(index));
    }

    public void back() {
        if (steps == null) return;

        if (index >= 0 && index < steps.size()) {
            CoachStep c = steps.get(index);
            if (c.onPrev != null) c.onPrev.run();
        }

        index = Math.max(0, index - 1);
        setStep(steps.get(index));
    }

    public void dismiss() {
        if (index >= 0 && index < steps.size()) {
            CoachStep c = steps.get(index);
            if (c.onCancel != null) c.onCancel.run();
        }

        ViewGroup p = (ViewGroup) getParent();
        if (p != null) p.removeView(this);
    }

    private void setStep(CoachStep s) {
        if (s.onBeforeStep != null) s.onBeforeStep.run();

        bringToFront();
        post(this::invalidate);

        View target = (s.target != null) ? s.target.resolve(rootForTargets) : null;

        if (target != null && (target.getWidth() == 0 || target.getHeight() == 0)) {
            target.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    target.getViewTreeObserver().removeOnPreDrawListener(this);
                    setStep(s);
                    return true;
                }
            });
            return;
        }

        final View bubbleAnchorView = (s.bubbleTarget != null)
                ? s.bubbleTarget.resolve(rootForTargets)
                : target;

        if (target == null) {
            if (script.allowSkipWhenTargetMissing()) {
                next();
                return;
            }
            hole.setEmpty();
        } else
            hole.set(getRectInParent(target, this));

        if (target != null)
            try {
                int id = target.getId();
                String resName = (id != View.NO_ID) ? target.getResources().getResourceName(id) : "no_id";
                if (BuildConfig.DEBUG) Log.d(TAG, "Step= " + s.id + " target=" + target.getClass().getSimpleName() + " id= " + resName + " rect=" + getRectOnScreen(target));
            } catch (Throwable i) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Step= " + s.id + " target=" + target.getClass().getSimpleName() + " rect=" + getRectOnScreen(target));
            }

        if (callout == null) {
            Context ctx = new ContextThemeWrapper(getContext(), R.style.Theme_Vortex_Coach_Material);
            callout = LayoutInflater.from(ctx).inflate(R.layout.view_coach_callout, this, false);
            addView(callout);

            callout.findViewById(R.id.coach_prev).setOnClickListener(v -> back());
            callout.findViewById(R.id.coach_next).setOnClickListener(v -> next());
            callout.findViewById(R.id.coach_cancel).setOnClickListener(v -> dismiss());
        }

        TextView titleTv = callout.findViewById(R.id.coach_title);
        titleTv.setText(s.title);

        TextView textTv = callout.findViewById(R.id.coach_text);
        textTv.setText(s.text);

        TextView counterTv = callout.findViewById(R.id.coach_counter);
        if (counterTv != null && steps != null) {
            int total = steps.size();
            int stepIndex = Math.max(0, Math.min(index, total - 1)) + 1;
            counterTv.setText(getContext().getString(R.string.coach_counter_format, stepIndex, total));
        }

        MaterialButton nextBtn = callout != null ? callout.findViewById(R.id.coach_next) : null;
        if (nextBtn != null) {
            if (steps != null && index == (steps.size() - 1))
                nextBtn.setText(R.string.finish);
            else
                nextBtn.setText(R.string.next);
        }

        try {
            if (callout != null)
                callout.announceForAccessibility(s.title + ". " + s.text);
        } catch (Throwable ignored) {}

        post(() -> {
            final int sidePad = dp(20);
            final int widthAvailable = getWidth() - (sidePad * 2);
            final int wSpec = View.MeasureSpec.makeMeasureSpec(widthAvailable, View.MeasureSpec.AT_MOST);
            final int hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            callout.measure(wSpec, hSpec);
            final int bubbleW = callout.getMeasuredWidth();
            final int bubbleH = callout.getMeasuredHeight();

            final View anchor = (bubbleAnchorView != null ? bubbleAnchorView : this);
            final android.graphics.Rect a = (anchor == this)
                    ? new android.graphics.Rect(getWidth() / 2, getHeight() / 2, getWidth() / 2, getHeight() / 2)
                    : getRectInParent(anchor, this);

            final int margin = dp(12);
            float x = a.centerX() - (bubbleW / 2f);
            float y = switch (s.placement) {
                case ABOVE -> a.top - bubbleH - margin;
                case BELOW -> a.bottom + margin;
                case CENTER -> {
                    x = (getWidth() - bubbleW) / 2f;
                    yield (getHeight() - bubbleH) / 2f;
                }
                default -> {
                    boolean fitsAbove = (a.top - margin - bubbleH) >= (safeTop + margin);
                    yield fitsAbove ? (a.top - bubbleH - margin) : (a.bottom + margin);
                }
            };

            final float maxX = getWidth() - sidePad - bubbleW;
            final float minY = safeTop + sidePad;
            final float maxY = getHeight() - safeBottom - sidePad - bubbleH;

            x += dp(s.nudgeXdp);
            y += dp(s.nudgeYdp);

            x = Math.max((float) sidePad, Math.min(x, maxX));
            y = Math.max(minY, Math.min(y, maxY));

            ViewGroup.LayoutParams rawLp = callout.getLayoutParams();
            FrameLayout.LayoutParams lp = (rawLp instanceof FrameLayout.LayoutParams)
                    ? (FrameLayout.LayoutParams) rawLp
                    : new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.leftMargin = Math.round(x);
            lp.topMargin = Math.round(y);
            lp.gravity = Gravity.TOP | Gravity.START;
            callout.setLayoutParams(lp);

            callout.setTranslationX(0f);
            callout.setTranslationY(0f);

            try {
                if (callout.getAlpha() < 1f || callout.getScaleX() < 0.99f) {
                    callout.setAlpha(0f);
                    callout.setScaleX(0.94f);
                    callout.setScaleY(0.94f);
                    callout.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(160);
                }
            } catch (Throwable ignored) {
            }
        });

        if (s.onAfterStep != null) s.onAfterStep.run();
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        safeTop = insets.getSystemWindowInsetTop();
        safeBottom = insets.getSystemWindowInsetBottom();
        return super.onApplyWindowInsets(insets);
    }

    @Override
    protected void onDraw(@NonNull Canvas c) {
        super.onDraw(c);

        dimPath.reset();
        dimPath.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        c.drawPath(dimPath, dimPaint);

        if (!hole.isEmpty()) {
            holePath.rewind();
            holePath.addRoundRect(hole, dp(8), dp(8), Path.Direction.CW);
            c.drawPath(holePath, clearPaint);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP) {
            if (!hole.isEmpty() && hole.contains(e.getX(), e.getY())) {
                if (steps != null && index >= 0 && index < steps.size()) {
                    CoachStep c = steps.get(index);
                    if (c.onHoleTap != null) c.onHoleTap.run();
                }
                return true;
            }
        }
        return true;
    }

    private RectF getRectOnScreen(View v) {
        final int[] loc = new int[2];
        v.getLocationOnScreen(loc);
        int w = v.getWidth();
        int h = v.getHeight();
        if (w == 0) w = v.getMeasuredWidth();
        if (h == 0) h = v.getMeasuredHeight();
        return new RectF(loc[0], loc[1], loc[0] + w, loc[1] + h);
    }

    private Rect getRectInParent(View child, View parent) {
        final int[] locChild = new int[2];
        final int[] locParent = new int[2];
        child.getLocationOnScreen(locChild);
        parent.getLocationOnScreen(locParent);

        return new Rect(
                locChild[0] - locParent[0],
                locChild[1] - locParent[1],
                locChild[0] - locParent[0] + child.getWidth(),
                locChild[1] - locParent[1] + child.getHeight()
        );
    }

    private int dp(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}