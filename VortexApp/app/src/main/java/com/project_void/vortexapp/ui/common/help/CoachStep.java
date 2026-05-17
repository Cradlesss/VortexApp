package com.project_void.vortexapp.ui.common.help;

import android.view.View;
import androidx.annotation.Nullable;

public final class CoachStep {
    public interface TargetResolver {
        @Nullable
        View resolve(View root);
    }

    public final String id;
    public final CharSequence title;
    public final CharSequence text;
    public final TargetResolver target;
    public final CoachPlacement placement;
    public final CoachTouchMode touchMode;
    public final Runnable onBeforeStep;
    public final Runnable onAfterStep;
    public final TargetResolver bubbleTarget;
    public final Runnable onNext;
    public final Runnable onPrev;
    public final Runnable onHoleTap;
    public final Runnable onCancel;
    public int nudgeXdp;
    public int nudgeYdp;

    private CoachStep(Builder b) {
        id = b.id;
        title = b.title;
        text = b.text;
        target = b.target;
        placement = b.placement;
        touchMode = b.touchMode;
        onBeforeStep = b.onBeforeStep;
        onAfterStep = b.onAfterStep;
        bubbleTarget = b.bubbleTarget;
        onNext = b.onNext;
        onPrev = b.onPrev;
        onHoleTap = b.onHoleTap;
        onCancel = b.onCancel;
    }

    public static final class Builder {
        String id;
        CharSequence title;
        CharSequence text;
        TargetResolver target;
        CoachPlacement placement = CoachPlacement.AUTO;
        CoachTouchMode touchMode = CoachTouchMode.BLOCK_ALL;
        Runnable onBeforeStep;
        Runnable onAfterStep;
        TargetResolver bubbleTarget;
        Runnable onNext;
        Runnable onPrev;
        Runnable onHoleTap;
        Runnable onCancel;
        int nudgeXdp;
        int nudgeYdp;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder title(CharSequence title) {
            this.title = title;
            return this;
        }

        public Builder text(CharSequence text) {
            this.text = text;
            return this;
        }

        public Builder target(TargetResolver target) {
            this.target = target;
            return this;
        }

        public Builder placement(CoachPlacement placement) {
            this.placement = placement;
            return this;
        }

        public Builder touch(CoachTouchMode touchMode) {
            this.touchMode = touchMode;
            return this;
        }

        public Builder before(Runnable onBeforeStep) {
            this.onBeforeStep = onBeforeStep;
            return this;
        }

        public Builder after(Runnable onAfterStep) {
            this.onAfterStep = onAfterStep;
            return this;
        }

        public Builder bubbleTarget(TargetResolver bubbleTarget) {
            this.bubbleTarget = bubbleTarget;
            return this;
        }

        public Builder onNext(Runnable onNext) {
            this.onNext = onNext;
            return this;
        }

        public Builder onPrev(Runnable onPrev) {
            this.onPrev = onPrev;
            return this;
        }

        public Builder onHoleTap(Runnable onHoleTap) {
            this.onHoleTap = onHoleTap;
            return this;
        }

        public Builder onCancel(Runnable onCancel) {
            this.onCancel = onCancel;
            return this;
        }

        public Builder nudgeX(int nudgeXdp) {
            this.nudgeXdp = nudgeXdp;
            return this;
        }

        public Builder nudgeY(int nudgeYdp) {
            this.nudgeYdp = nudgeYdp;
            return this;
        }

        public CoachStep build() {
            return new CoachStep(this);
        }
    }
}
