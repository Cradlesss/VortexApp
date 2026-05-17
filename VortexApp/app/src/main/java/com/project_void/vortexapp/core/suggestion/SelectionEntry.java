package com.project_void.vortexapp.core.suggestion;

public final class SelectionEntry {
    public enum Type {
        EFFECT, COLOR
    }

    public final Type type;
    public final int effectMode;
    public final int r, g, b;
    public final long timestamp;

    private SelectionEntry(Type type, int effectMode, int r, int g, int b, long timestamp) {
        this.type = type;
        this.effectMode = effectMode;
        this.r = r;
        this.g = g;
        this.b = b;
        this.timestamp = timestamp;
    }

    public static SelectionEntry effect(int mode){
        return new SelectionEntry(Type.EFFECT, mode, 0, 0, 0, System.currentTimeMillis());
    }

    public static SelectionEntry color(int r, int g, int b){
        return new SelectionEntry(Type.COLOR, 0, r, g, b, System.currentTimeMillis());
    }

    public String serialize(){
        if (type == Type.EFFECT)
            return "e:" + effectMode + ":" + timestamp;
        return String.format("c:%02X%02X%02X:%d", r, g, b, timestamp);
    }

    public static SelectionEntry deserialize(String s){
        try{
            String[] p = s.split(":");
            long ts = Long.parseLong(p[2]);
            if (p[0].equals("e"))
                return new SelectionEntry(Type.EFFECT, Integer.parseInt(p[1]), 0, 0, 0, ts);
            int rgb = Integer.parseInt(p[1], 16);
            return new SelectionEntry(Type.COLOR, 0, (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, ts);
        } catch (Exception e){
            return null;
        }
    }
}
