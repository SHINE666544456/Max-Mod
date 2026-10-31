package dev.skinoverride;

import net.minecraft.client.MinecraftClient;

import java.util.Locale;

/** World day counter and exact in-game clock (tick 0 = 6:00, tick 6000 = noon, tick 18000 = midnight). */
public final class DayClock {

    private static long prev;
    private static long cur;
    private static boolean valid;
    private static boolean running;

    private DayClock() {
    }

    public static void tick(MinecraftClient mc) {
        if (mc.world == null) {
            valid = false;
        } else {
            long now = mc.world.getTimeOfDay();
            if (!valid) {
                prev = now;
                cur = now;
                valid = true;
            } else {
                prev = cur;
                cur = now;
            }
            running = cur != prev;
        }
        PlayTime.tick(mc);
    }

    public static boolean valid() {
        return valid;
    }

    /** False when the day/night cycle is stopped (or the game is paused). */
    public static boolean running() {
        return running;
    }

    public static double ticks(float tickProgress) {
        return cur + (running ? tickProgress : 0f);
    }

    public static long day(float tickProgress) {
        return (long) (ticks(tickProgress) / 24000.0) + 1;
    }

    /** Tick within the current day, 0..23999. */
    public static int dayTick(float tickProgress) {
        return (int) (((long) ticks(tickProgress)) % 24000L);
    }

    public static String clock(float tickProgress, boolean h24, boolean seconds) {
        double hours = (ticks(tickProgress) % 24000.0) / 1000.0 + 6.0;
        hours = hours % 24.0;
        int hh = (int) hours;
        double minutes = (hours - hh) * 60.0;
        int mm = (int) minutes;
        int ss = (int) ((minutes - mm) * 60.0);
        String suffix = "";
        int shownHour = hh;
        if (!h24) {
            suffix = hh < 12 ? " AM" : " PM";
            shownHour = hh % 12 == 0 ? 12 : hh % 12;
        }
        if (seconds) {
            return String.format(Locale.ROOT, "%02d:%02d:%02d%s", shownHour, mm, ss, suffix);
        }
        return String.format(Locale.ROOT, "%02d:%02d%s", shownHour, mm, suffix);
    }

    public static String phase(int dayTick) {
        if (dayTick < 6000) {
            return "Morning";
        }
        if (dayTick < 12000) {
            return "Afternoon";
        }
        if (dayTick < 13000) {
            return "Dusk";
        }
        if (dayTick < 23000) {
            return "Night";
        }
        return "Dawn";
    }

    /** Real-time seconds until the in-game tick `target` of the current day (wraps to the next day). */
    public static int secondsUntil(int dayTick, int target) {
        int diff = target - dayTick;
        if (diff < 0) {
            diff += 24000;
        }
        return diff / 20;
    }
}
