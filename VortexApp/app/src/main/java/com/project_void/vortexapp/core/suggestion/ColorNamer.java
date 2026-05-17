package com.project_void.vortexapp.core.suggestion;

public class ColorNamer {
    private ColorNamer() {}

    private static final Object[][] ENTRIES = {
            {240, 248, 255, "Alice Blue"},
            {250, 235, 215, "Antique White"},
            {127, 255, 212, "Aquamarine"},
            {240, 255, 255, "Azure"},
            {245, 245, 220, "Beige"},
            {255, 228, 196, "Bisque"},
            {0, 0, 0, "Black"},
            {255, 235, 205, "Blanched Almond"},
            {0, 0, 255, "Blue"},
            {138, 43, 226, "Blue Violet"},
            {165, 42, 42, "Brown"},
            {222, 184, 135, "Burlywood"},
            {95, 158, 160, "Cadet Blue"},
            {127, 255, 0, "Chartreuse"},
            {210, 105, 30, "Chocolate"},
            {255, 127, 80, "Coral"},
            {100, 149, 237, "Cornflower Blue"},
            {255, 248, 220, "Cornsilk"},
            {220, 20, 60, "Crimson"},
            {0, 255, 255, "Cyan"},
            {0, 0, 139, "Dark Blue"},
            {0, 139, 139, "Dark Cyan"},
            {184, 134, 11, "Dark Goldenrod"},
            {0, 100, 0, "Dark Green"},
            {189, 183, 107, "Dark Khaki"},
            {139, 0, 139, "Dark Magenta"},
            {85, 107, 47, "Dark Olive Green"},
            {255, 140, 0, "Dark Orange"},
            {153, 50, 204, "Dark Orchid"},
            {139, 0, 0, "Dark Red"},
            {233, 150, 122, "Dark Salmon"},
            {143, 188, 143, "Dark Sea Green"},
            {72, 61, 139, "Dark Slate Blue"},
            {47, 79, 79, "Dark Slate Gray"},
            {0, 206, 209, "Dark Turquoise"},
            {148, 0, 211, "Dark Violet"},
            {255, 20, 147, "Deep Pink"},
            {0, 191, 255, "Deep Sky Blue"},
            {30, 144, 255, "Dodger Blue"},
            {178, 34, 34, "Firebrick"},
            {255, 250, 240, "Floral White"},
            {34, 139, 34, "Forest Green"},
            {248, 248, 255, "Ghost White"},
            {255, 215, 0, "Gold"},
            {218, 165, 32, "Goldenrod"},
            {128, 128, 128, "Gray"},
            {0, 128, 0, "Web Green"},
            {173, 255, 47, "Green Yellow"},
            {240, 255, 240, "Honeydew"},
            {255, 105, 180, "Hot Pink"},
            {205, 92, 92, "Indian Red"},
            {75, 0, 130, "Indigo"},
            {255, 255, 240, "Ivory"},
            {240, 230, 140, "Khaki"},
            {230, 230, 250, "Lavender"},
            {255, 240, 245, "Lavender Blush"},
            {124, 252, 0, "Lawn Green"},
            {255, 250, 205, "Lemon Chiffon"},
            {173, 216, 230, "Light Blue"},
            {240, 128, 128, "Light Coral"},
            {224, 255, 255, "Light Cyan"},
            {238, 221, 130, "Light Goldenrod"},
            {250, 250, 210, "Light Goldenrod Yellow"},
            {144, 238, 144, "Light Green"},
            {255, 182, 193, "Light Pink"},
            {255, 160, 122, "Light Salmon"},
            {32, 178, 170, "Light Sea Green"},
            {135, 206, 250, "Light Sky Blue"},
            {119, 136, 153, "Light Slate Gray"},
            {176, 196, 222, "Light Steel Blue"},
            {255, 255, 224, "Light Yellow"},
            {50, 205, 50, "Lime Green"},
            {250, 240, 230, "Linen"},
            {255, 0, 255, "Magenta"},
            {176, 48, 96, "Maroon"},
            {102, 205, 170, "Medium Aquamarine"},
            {0, 0, 205, "Medium Blue"},
            {186, 85, 211, "Medium Orchid"},
            {147, 112, 219, "Medium Purple"},
            {60, 179, 113, "Medium Sea Green"},
            {123, 104, 238, "Medium Slate Blue"},
            {0, 250, 154, "Medium Spring Green"},
            {72, 209, 204, "Medium Turquoise"},
            {199, 21, 133, "Medium Violet Red"},
            {25, 25, 112, "Midnight Blue"},
            {245, 255, 250, "Mint Cream"},
            {255, 228, 225, "Misty Rose"},
            {255, 228, 181, "Moccasin"},
            {255, 222, 173, "Navajo White"},
            {0, 0, 128, "Navy"},
            {253, 245, 230, "Old Lace"},
            {128, 128, 0, "Olive"},
            {107, 142, 35, "Olive Drab"},
            {255, 165, 0, "Orange"},
            {255, 69, 0, "Orange Red"},
            {218, 112, 214, "Orchid"},
            {238, 232, 170, "Pale Goldenrod"},
            {152, 251, 152, "Pale Green"},
            {175, 238, 238, "Pale Turquoise"},
            {219, 112, 147, "Pale Violet Red"},
            {255, 239, 213, "Papaya Whip"},
            {255, 218, 185, "Peach Puff"},
            {205, 133, 63, "Peru"},
            {255, 192, 203, "Pink"},
            {221, 160, 221, "Plum"},
            {176, 224, 230, "Powder Blue"},
            {128, 0, 128, "Purple"},
            {255, 0, 0, "Red"},
            {188, 143, 143, "Rosy Brown"},
            {65, 105, 225, "Royal Blue"},
            {139, 69, 19, "Saddle Brown"},
            {250, 128, 114, "Salmon"},
            {244, 164, 96, "Sandy Brown"},
            {46, 139, 87, "Sea Green"},
            {255, 245, 238, "Seashell"},
            {160, 82, 45, "Sienna"},
            {192, 192, 192, "Silver"},
            {135, 206, 235, "Sky Blue"},
            {106, 90, 205, "Slate Blue"},
            {112, 128, 144, "Slate Gray"},
            {255, 250, 250, "Snow"},
            {0, 255, 127, "Spring Green"},
            {70, 130, 180, "Steel Blue"},
            {210, 180, 140, "Tan"},
            {0, 128, 128, "Teal"},
            {216, 191, 216, "Thistle"},
            {255, 99, 71, "Tomato"},
            {64, 224, 208, "Turquoise"},
            {238, 130, 238, "Violet"},
            {245, 222, 179, "Wheat"},
            {255, 255, 255, "White"},
            {245, 245, 245, "White Smoke"},
            {255, 255, 0, "Yellow"},
            {154, 205, 50, "Yellow Green"},
            {255, 140, 105, "Sunset Orange"},
            {255, 83, 112, "Neon Red"},
            {0, 255, 128, "Neon Green"},
            {0, 128, 255, "Neon Blue"},
            {255, 0, 128, "Neon Pink"},
            {128, 0, 255, "Neon Purple"},
            {255, 191, 0, "Neon Yellow"},
            {0, 255, 210, "Neon Cyan"},
            {255, 77, 0, "Neon Orange"},
            {255, 255, 200, "Warm White"},
            {200, 220, 255, "Cool White"},
    };

    private static final String[] NAMES;
    private static final float[][] HVS_TABLE;

    static {
        NAMES = new String[ENTRIES.length];
        HVS_TABLE = new float[ENTRIES.length][3];

        for (int i = 0; i < ENTRIES.length; i++) {
            int r = (int) ENTRIES[i][0];
            int g = (int) ENTRIES[i][1];
            int b = (int) ENTRIES[i][2];
            NAMES[i] = (String) ENTRIES[i][3];
            HVS_TABLE[i] = toHvs(r, g, b);
        }
    }

    public static String name(int r, int g, int b) {
        if (r == 0 && g == 0 && b == 0) return "Black";

        float[] hvs = toHvs(r, g, b);
        float bestDist = Float.MAX_VALUE;
        int bestIdx = 0;

        for (int i = 0; i < HVS_TABLE.length; i++) {
            float d = hvsDistance(hvs, HVS_TABLE[i]);
            if (d < bestDist) {
                bestDist = d;
                bestIdx = i;
            }
        }
        return NAMES[bestIdx];
    }

    private static float hvsDistance(float[] a, float[] b) {
        float dh = Math.abs(a[0] - b[0]);
        if (dh > 180f) dh = 360f - dh;
        dh /= 180f;

        float ds = Math.abs(a[1] - b[1]);
        float dv = Math.abs(a[2] - b[2]);

        float minSat = Math.min(a[1], b[1]);
        return (dh * minSat * 4f) + (ds) + (dv * 0.5f);
    }

    static float[] toHvs(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        float h = 0f;
        if (delta > 0f) {
            if (max == rf) h = 60f * (((gf - bf) / delta) % 6f);
            else if (max == gf) h = 60f * (((bf - rf) / delta) + 2f);
            else h = 60f * (((rf - gf) / delta) + 4f);
            if (h < 0f) h += 360f;
        }
        float s = (max == 0f) ? 0f : delta / max;
        return new float[]{h, s, max};
    }
}
