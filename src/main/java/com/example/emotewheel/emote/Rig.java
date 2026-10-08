package com.example.emotewheel.emote;

/**
 * Where the head, body and whole-model root end up for a given emote pose, in model-space pixels and radians.
 * This repeats the maths the model mixin applies to the real model parts, so extras that are drawn separately
 * (ears, tail, pet) can follow the player exactly, including flips, spins and leaning.
 *
 * Result layout (18 floats): root[x y z xRot yRot zRot], head[...], body[...]
 */
public final class Rig {
    private Rig() {}

    public static final int ROOT = 0, HEAD = 6, BODY = 12;

    /** @param p pose (all zero if no emote)  @param w blend 0..1  @param upperOnly walking emotes leave the legs/root alone */
    public static float[] compute(Pose p, float w, boolean upperOnly, float headYawRad, float headPitchRad, boolean crouch) {
        float[] r = new float[18];
        float lean = p.lean * w;
        float c = (float) Math.cos(lean), s = (float) Math.sin(lean);

        float headY0 = crouch ? 4.2f : 0f, bodyY0 = crouch ? 3.2f : 0f, bodyX0 = crouch ? 0.5f : 0f;

        // head
        r[HEAD]     = 0f;
        r[HEAD + 1] = 12f + (headY0 - 12f) * c;
        r[HEAD + 2] = (headY0 - 12f) * s;
        r[HEAD + 3] = headPitchRad + p.headX * w + lean;
        r[HEAD + 4] = headYawRad + p.headY * w;
        r[HEAD + 5] = p.headZ * w;

        // body
        r[BODY]     = 0f;
        r[BODY + 1] = 12f + (bodyY0 - 12f) * c;
        r[BODY + 2] = (bodyY0 - 12f) * s;
        r[BODY + 3] = bodyX0 + ((p.bodyX + p.lean) - bodyX0) * w;
        r[BODY + 4] = p.bodyY * w;
        r[BODY + 5] = p.bodyZ * w;

        // whole-model transform
        if (!upperOnly) {
            float flip = p.flip * w, roll = p.roll * w;
            r[ROOT + 3] = flip;
            r[ROOT + 4] = p.spin * w;
            r[ROOT + 5] = roll;
            r[ROOT]     = p.rootX * w + 12f * (float) Math.sin(roll);
            r[ROOT + 1] = p.rootY * w + (12f - 12f * (float) Math.cos(flip)) + (12f - 12f * (float) Math.cos(roll));
            r[ROOT + 2] = p.rootZ * w - 12f * (float) Math.sin(flip);
        }
        return r;
    }

    /** The blend weight the model mixin uses (ease-in, and ease-out for one-shot emotes). */
    public static float weight(Emote e, Pose p, float t) {
        float w = Anim.smooth(t / 0.18f) * p.fade;
        if (e.isOneShot()) w *= 1f - Anim.smooth((t - (e.duration() - 0.2f)) / 0.2f);
        return w;
    }
}
