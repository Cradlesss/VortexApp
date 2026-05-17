package com.project_void.vortexapp.core.suggestion;

import com.project_void.vortexapp.ble.model.LedState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class SuggestionEngine {
    private static final int[] ALL_EFFECTS = {1,2,3,4,5,6,7,8,9,10,11};
    private static final float DOMINANCE_THRESHOLD = 0.50f;
    private static final float STALENESS_MULTIPLIER = 0.3f;
    private static final float NOVELTY_BONUS = 0.5f;
    private static final float TIE_THRESHOLD = 0.1f;
    private final Random rng = new Random();

    public static final class Suggestion{
        public final SelectionEntry.Type type;
        public final int effectMode;
        public final int r, g, b;

        private Suggestion(int effectMode){
            this.type = SelectionEntry.Type.EFFECT;
            this.effectMode = effectMode;
            this.r = this.g = this.b = 0;
        }

        private Suggestion(int r, int g, int b){
            this.type = SelectionEntry.Type.COLOR;
            this.effectMode = 0;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public static Suggestion effect(int mode) {
            return new Suggestion(mode);
        }

        public static Suggestion color(int r, int g, int b){
            return new Suggestion(r, g, b);
        }
    }

    public Suggestion compute(
            List<SelectionEntry> history,
            Map<Integer, Integer> savedColors,
            LedState current
    ) {
        List<Candidate> pool = buildPool(savedColors, current);
        if (pool.isEmpty()) return Suggestion.effect(5);
        scoreAll(pool, history);

        return pickBest(pool);
    }

    private List<Candidate> buildPool(Map<Integer, Integer> savedColors, LedState current){
        List<Candidate> pool = new ArrayList<>();

        for (int e : ALL_EFFECTS){
            if (isCurrentEffect(current, e)) continue;
            pool.add(new Candidate(e));
        }

        for (int argb : savedColors.values()){
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b =  argb & 0xFF;

            if (!isCurrentColor(current, r,g,b))
                pool.add(new Candidate(r, g, b));

            for (int hueShift : new int[]{-30, -15, 15,  30}){
                int[] neighbor = shiftHue(r, g, b, hueShift);
                int nr = neighbor[0], ng = neighbor[1], nb = neighbor[2];
                if (isCurrentColor(current, nr, ng, nb)) continue;
                if (alreadyInPool(pool, nr, ng, nb)) continue;
                Candidate c = new Candidate(nr, ng, nb);
                c.score = -0.2f;
                pool.add(c);
            }
        }
        return pool;
    }

    private int[] shiftHue(int r, int g, int b, int degrees) {
        float[] hvs = ColorNamer.toHvs(r, g, b);
        hvs[0] = (hvs[0] + degrees + 360f) % 360f;
        return hvsToRgb(hvs[0], hvs[1], hvs[2]);
    }

    private static int[] hvsToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1 - Math.abs((h / 60f) % 2f - 1f));
        float m = v -c;
        float r, g, b;

        if (h < 60f) { r = c; g = x; b = 0; }
        else if (h < 120f) { r = x; g = c; b = 0; }
        else if (h < 180f) { r = 0; g = c; b = x; }
        else if (h < 240f) { r = 0; g = x; b = c; }
        else if (h < 300f) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }

        return new int[]{
                Math.round((r + m) * 255f),
                Math.round((g + m) * 255f),
                Math.round((b + m) * 255f)
        };
    }

    private static boolean alreadyInPool(List<Candidate> pool, int r, int g, int b){
        for (Candidate c : pool)
            if (c.type == SelectionEntry.Type.COLOR && c.r == r && c.g == g && c.b == b)
                return true;
        return false;
    }
    private boolean isCurrentEffect(LedState s, int mode) {
        return s != null && s.mode() == mode && s.mode() != 13;
    }

    private boolean isCurrentColor(LedState s, int r, int g, int b) {
        return s != null && s.mode() == 13 && s.r() == r && s.g() == g && s.b() == b;
    }

    private void scoreAll(List<Candidate> pool, List<SelectionEntry> history){
        if (history.isEmpty()) return;

        for (SelectionEntry e : history)
            for (Candidate c : pool)
                if (c.matches(e)) c.historyCount++;

        for (int i = 0; i < history.size(); i++){
            SelectionEntry e = history.get(i);
            float weight = 1.0f / (i + 1);
            for (Candidate c : pool)
                if (c.matches(e)) c.score += weight;
        }

        int total = history.size();
        for (Candidate c : pool){
            if (total > 0 && (c.historyCount / (float) total) > DOMINANCE_THRESHOLD)
                c.score *= STALENESS_MULTIPLIER;
            if (c.historyCount == 0)
                c.score += NOVELTY_BONUS;
        }
    }

    private Suggestion pickBest(List<Candidate> pool){
        float best = -1f;
        for (Candidate c : pool)
            if (c.score > best) best = c.score;

        List<Candidate> tied = new ArrayList<>();
        for (Candidate c : pool)
            if (Math.abs(c.score - best) < TIE_THRESHOLD) tied.add(c);

        Candidate chosen = tied.get(rng.nextInt(tied.size()));
        return chosen.toSuggestion();
    }

    private static final class Candidate {
        final SelectionEntry.Type type;
        final int effectMode;
        final int r,g,b;
        float score = 0f;
        int historyCount = 0;

        Candidate(int effectMode){
            this.type = SelectionEntry.Type.EFFECT;
            this.effectMode = effectMode;
            this.r = this.g = this.b = 0;
        }

        Candidate(int r, int g, int b){
            this.type = SelectionEntry.Type.COLOR;
            this.effectMode = 0;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        boolean matches(SelectionEntry e){
            if (e.type != type) return false;
            if (type == SelectionEntry.Type.EFFECT) return e.effectMode == effectMode;
            return r == e.r && g == e.g && b == e.b;
        }

        Suggestion toSuggestion(){
            return type == SelectionEntry.Type.EFFECT ? Suggestion.effect(effectMode) : Suggestion.color(r, g, b);
        }
    }
}
