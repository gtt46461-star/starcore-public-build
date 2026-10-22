package com.ethk3.novaonepro;

/** Generated from Models.Draw by tools/apply_v51_source_patches.py. */
public final class V51DrawAdapter {
    private V51DrawAdapter() {}

    public static V51Models.DrawFrame convert(Models.Draw draw) {
        if (draw == null) return null;
        int[] permutation = V51Math.copy(draw.numbers);
        if (!V51Math.validPermutation(permutation)) return null;
        return new V51Models.DrawFrame(draw.issue, permutation, draw.openTime);
    }
}
