package dev.skinoverride.map;

import dev.skinoverride.ModConfig;
import dev.skinoverride.PlayTime;
import dev.skinoverride.chat.ChatColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Waypoint sharing with friends through ordinary chat. A shared waypoint is a normal chat line with a small
 * tag at the end. Players who have this mod turn the tag into a waypoint they can accept (or that is saved
 * automatically when it comes from someone on your friends list). Everyone else just reads the coordinates.
 * Works on any server, no server mod needed.
 */
public final class SharedWaypoints {

    /** A waypoint somebody sent you that you have not accepted yet. */
    public record Incoming(String from, String name, int x, int y, int z, String dim) {
    }

    private static final Pattern TAG = Pattern.compile(
            "\\[iris-wp\\|([^|\\]\\[]{1,32})\\|(-?\\d{1,8})\\|(-?\\d{1,5})\\|(-?\\d{1,8})\\|([a-z0-9_:./-]{1,60})\\]");

    private static final List<Incoming> INBOX = new ArrayList<>();

    /** Set by the client entrypoint so chat can tell you which key accepts a waypoint. */
    public static KeyBinding acceptKey;

    private SharedWaypoints() {
    }

    public static List<Incoming> inbox() {
        return INBOX;
    }

    // ---------- friends ----------

    public static boolean isFriend(String name) {
        for (String f : ModConfig.get().friends) {
            if (f.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean senderIsFriend(String sender) {
        for (String token : sender.split("[^A-Za-z0-9_]+")) {
            if (!token.isEmpty() && isFriend(token)) {
                return true;
            }
        }
        return false;
    }

    public static boolean addFriend(String name) {
        String n = name.trim();
        if (!n.matches("[A-Za-z0-9_]{1,16}") || isFriend(n)) {
            return false;
        }
        ModConfig cfg = ModConfig.get();
        cfg.friends.add(n);
        cfg.save();
        return true;
    }

    public static void removeFriend(String name) {
        ModConfig cfg = ModConfig.get();
        cfg.friends.removeIf(f -> f.equalsIgnoreCase(name));
        cfg.save();
    }

    // ---------- sending ----------

    private static String clean(String name) {
        String n = name.replaceAll("[|\\[\\]]", "_").trim();
        if (n.isEmpty()) {
            n = "Waypoint";
        }
        return n.length() > 32 ? n.substring(0, 32) : n;
    }

    public static String tag(String name, int x, int y, int z, String dim) {
        return "[iris-wp|" + clean(name) + "|" + x + "|" + y + "|" + z + "|" + dim + "]";
    }

    /** Sends a waypoint to public chat (toPlayer == null) or as a private message to one player. */
    public static boolean send(MinecraftClient mc, Waypoints.Waypoint wp, String toPlayer) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
            return false;
        }
        String dim = mc.world.getRegistryKey().getValue().toString().toLowerCase(Locale.ROOT);
        String message = "Waypoint \"" + clean(wp.name) + "\" at " + wp.x + " " + wp.y + " " + wp.z + " "
                + tag(wp.name, wp.x, wp.y, wp.z, dim);
        if (toPlayer == null) {
            mc.getNetworkHandler().sendChatMessage(message);
        } else {
            mc.getNetworkHandler().sendChatCommand("msg " + toPlayer + " " + message);
        }
        return true;
    }

    // ---------- receiving ----------

    /**
     * Looks for waypoint tags in a chat message. Returns the text to show instead (tag removed, with a short
     * note), or null if the message has no tag.
     */
    public static String scan(String sender, String message) {
        Matcher m = TAG.matcher(message);
        if (!m.find()) {
            return null;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        String self = mc.player == null ? "" : mc.player.getName().getString();
        boolean mine = false;
        for (String token : sender.split("[^A-Za-z0-9_]+")) {
            if (token.equalsIgnoreCase(self)) {
                mine = true;
            }
        }
        String cleaned = TAG.matcher(message).replaceAll("").trim();
        if (mine) {
            return cleaned;
        }
        ModConfig cfg = ModConfig.get();
        boolean friend = senderIsFriend(sender);
        int saved = 0;
        int pending = 0;
        String lastName = "";
        m.reset();
        while (m.find()) {
            try {
                Incoming in = new Incoming(sender, m.group(1), Integer.parseInt(m.group(2)),
                        Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)), m.group(5));
                lastName = in.name();
                if (friend && cfg.wpAutoAccept) {
                    accept(mc, in);
                    saved++;
                } else if (!alreadyQueued(in)) {
                    INBOX.add(in);
                    while (INBOX.size() > 20) {
                        INBOX.remove(0);
                    }
                    pending++;
                }
            } catch (NumberFormatException e) {
                // ignore a broken tag
            }
        }
        if (mc.player != null && saved + pending > 0) {
            String text = saved > 0
                    ? "Saved waypoint \"" + lastName + "\" from " + sender
                    : "Shared waypoint \"" + lastName + "\" from " + sender + " - press " + acceptKeyName() + " to save";
            mc.inGameHud.setOverlayMessage(Text.literal(text), false);
        }
        String note = saved > 0 ? " [waypoint saved]" : pending > 0 ? " [waypoint: press " + acceptKeyName() + " to save]" : "";
        return cleaned + note;
    }

    private static String acceptKeyName() {
        return acceptKey == null ? "Y" : acceptKey.getBoundKeyLocalizedText().getString();
    }

    private static boolean alreadyQueued(Incoming in) {
        for (Incoming other : INBOX) {
            if (other.name().equals(in.name()) && other.x() == in.x() && other.y() == in.y()
                    && other.z() == in.z() && other.dim().equals(in.dim())) {
                return true;
            }
        }
        return false;
    }

    // ---------- accepting ----------

    /** Saves a shared waypoint into the right world / dimension list (skips exact duplicates). */
    public static void accept(MinecraftClient mc, Incoming in) {
        String key = PlayTime.worldKey(mc) + "|" + in.dim();
        boolean current = key.equals(Waypoints.currentKey());
        boolean duplicate = false;
        if (current) {
            for (Waypoints.Waypoint w : Waypoints.list()) {
                if (w.x == in.x() && w.y == in.y() && w.z == in.z() && w.name.equals(in.name())) {
                    duplicate = true;
                    break;
                }
            }
        }
        if (!duplicate) {
            int color = ChatColors.PALETTE[(in.name().hashCode() & 0x7FFFFFFF) % ChatColors.PALETTE.length].rgb();
            Waypoints.addTo(key, in.name(), in.x(), in.y(), in.z(), color);
        }
        INBOX.remove(in);
    }

    /** Accepts the newest waypoint in the inbox. Returns false if the inbox is empty. */
    public static boolean acceptLatest(MinecraftClient mc) {
        if (INBOX.isEmpty()) {
            return false;
        }
        Incoming in = INBOX.get(INBOX.size() - 1);
        accept(mc, in);
        if (mc.player != null) {
            mc.inGameHud.setOverlayMessage(Text.literal("Waypoint \"" + in.name() + "\" saved"), false);
        }
        return true;
    }

    public static void dismiss(Incoming in) {
        INBOX.remove(in);
    }
}
