package com.example.emotewheel.emote;

/**
 * One emote.
 * icon: item id path in the minecraft namespace, used as the wheel icon (e.g. "poppy").
 * duration: seconds; 0 = plays until cancelled (poses and loops).
 */
public final class Emote {
    /** Upper-body emote: you keep walking normally (legs and body position are left to the game). */
    public static final int WALK = 1;
    /** Full-body emote that keeps playing while you move (naruto run, floating...). */
    public static final int MOVE = 2;
    /** Cat ears + tail appear while this plays. */
    public static final int CAT = 4;
    /** Nothing cancels it except playing it again / the stop key. */
    public static final int NOCANCEL = 8;

    @FunctionalInterface
    public interface PoseFn {
        void apply(Pose p, float t);
    }

    private final String id, name, icon;
    private final Category category;
    private final float duration;
    private final PoseFn fn;
    private int flags;
    private String state;       // persistent toggle this emote flips (e.g. "cat_girl"), or null
    private int stateDelay;     // ticks after starting before the toggle happens
    private String only;        // account name this emote is restricted to, or null for everyone
    private Fx fx = Fx.NONE;

    Emote(String id, String name, Category category, String icon, float duration, PoseFn fn) {
        this.id = id; this.name = name; this.category = category;
        this.icon = icon; this.duration = duration; this.fn = fn;
    }

    public String id() { return id; }
    public String name() { return name; }
    public Category category() { return category; }
    public String icon() { return icon; }
    public float duration() { return duration; }
    public PoseFn fn() { return fn; }

    public boolean isOneShot() { return duration > 0f; }
    public boolean isWalk() { return (flags & WALK) != 0; }
    public boolean isCat() { return (flags & CAT) != 0; }
    public boolean allowsMovement() { return (flags & (WALK | MOVE | NOCANCEL)) != 0; }
    public String state() { return state; }
    public int stateDelay() { return stateDelay; }
    public String only() { return only; }
    public Fx fx() { return fx; }

    // builder-style setters, used while registering
    public Emote flag(int f) { flags |= f; return this; }
    public Emote state(String s, int delayTicks) { this.state = s; this.stateDelay = delayTicks; this.flags |= NOCANCEL; return this; }
    public Emote only(String accountName) { this.only = accountName; return this; }
    public Emote fx(Fx f) { this.fx = f; return this; }
}
