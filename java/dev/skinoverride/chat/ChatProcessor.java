package dev.skinoverride.chat;

import dev.skinoverride.ModConfig;
import dev.skinoverride.SkinOverrideClient;
import dev.skinoverride.WorldLabels;
import dev.skinoverride.map.SharedWaypoints;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.TextCodecs;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites every chat line the client receives: per-player name/message colors, join/leave/
 * advancement/death colors, emotes. Also feeds speech bubbles. Never throws: on any problem
 * the original message is shown untouched.
 */
public final class ChatProcessor {

    private static final Pattern PLAIN_CHAT = Pattern.compile("^<([A-Za-z0-9_]{1,16})> (.*)$", Pattern.DOTALL);

    private ChatProcessor() {
    }

    public static Text process(Text original) {
        Text result;
        try {
            result = doProcess(original, ModConfig.get());
        } catch (RuntimeException e) {
            SkinOverrideClient.LOGGER.warn("Chat processing failed, showing original message", e);
            result = original;
        }
        try {
            return ChatExtras.finish(result);
        } catch (RuntimeException e) {
            SkinOverrideClient.LOGGER.warn("Chat timestamp failed", e);
            return result;
        }
    }

    private static Text doProcess(Text original, ModConfig cfg) {
        if (!(original.getContent() instanceof TranslatableTextContent content)) {
            return processPlainChat(original, cfg);
        }
        String key = content.getKey();
        Object[] args = content.getArgs();

        int nameIdx = -1;
        int msgIdx = -1;
        boolean bubble = false;
        switch (key) {
            case "chat.type.text", "chat.type.emote", "chat.type.announcement" -> {
                nameIdx = 0;
                msgIdx = 1;
                bubble = true;
            }
            case "commands.message.display.incoming" -> {
                nameIdx = 0;
                msgIdx = 1;
            }
            case "chat.type.team.text" -> {
                nameIdx = 1;
                msgIdx = 2;
                bubble = true;
            }
            default -> {
            }
        }
        if (nameIdx >= 0 && args.length > msgIdx) {
            return playerChat(original, key, args, nameIdx, msgIdx, bubble, cfg);
        }

        if (!cfg.chatCustom) {
            return original;
        }
        if (key.startsWith("multiplayer.player.joined")) {
            return ChatColors.apply(original, cfg.chatJoinColor);
        }
        if (key.equals("multiplayer.player.left")) {
            return ChatColors.apply(original, cfg.chatLeaveColor);
        }
        if (key.startsWith("chat.type.advancement.")) {
            return ChatColors.apply(original, cfg.chatAdvancementColor);
        }
        if (key.startsWith("death.")) {
            return ChatColors.apply(original, cfg.chatDeathColor);
        }
        return original;
    }

    private static Text playerChat(Text original, String key, Object[] args, int nameIdx, int msgIdx,
                                   boolean bubble, ModConfig cfg) {
        Text nameText = asText(args[nameIdx]);
        Text msgText = asText(args[msgIdx]);
        String sender = nameText.getString();

        String sharedWp = SharedWaypoints.scan(sender, msgText.getString());
        if (sharedWp != null) {
            msgText = Text.literal(sharedWp);
        }
        boolean mention = ChatExtras.isMention(sender, msgText.getString());

        int nameMode = ChatColors.DEFAULT;
        int msgMode = ChatColors.DEFAULT;
        if (cfg.chatCustom) {
            ModConfig.PlayerChat pc = lookup(sender, cfg);
            nameMode = pc != null && pc.nameColor != ChatColors.DEFAULT ? pc.nameColor : cfg.chatDefaultNameColor;
            msgMode = pc != null && pc.msgColor != ChatColors.DEFAULT ? pc.msgColor : cfg.chatDefaultMsgColor;
        }
        if (mention) {
            ChatExtras.noteMention();
            if (cfg.chatMentionHighlight) {
                msgMode = cfg.chatMentionColor;
            }
        }
        if (cfg.chatEmotes) {
            msgText = Emotes.apply(msgText, cfg.chatEmoteFaces);
        }
        if (bubble && cfg.chatBubbles) {
            WorldLabels.push(sender, msgText.getString());
        }
        nameText = ChatColors.apply(nameText, nameMode);
        msgText = ChatColors.apply(msgText, msgMode);

        Object[] newArgs = args.clone();
        newArgs[nameIdx] = nameText;
        newArgs[msgIdx] = msgText;
        MutableText result = Text.translatable(key, newArgs).setStyle(original.getStyle());
        for (Text sibling : original.getSiblings()) {
            result.append(sibling);
        }
        if (cfg.chatHeads && bubble) {
            Text head = headFor(sender);
            if (head != null) {
                return Text.empty().append(head).append(Text.literal(" ")).append(result);
            }
        }
        return result;
    }

    /** Servers that format chat themselves as plain "<Name> message" text. */
    private static Text processPlainChat(Text original, ModConfig cfg) {
        Matcher m = PLAIN_CHAT.matcher(original.getString());
        if (!m.matches()) {
            return original;
        }
        String sender = m.group(1);
        String body = m.group(2);
        String sharedWp = SharedWaypoints.scan(sender, body);
        if (sharedWp != null) {
            body = sharedWp;
        }
        Text msg = Text.literal(body);
        Text name = Text.literal(sender);
        boolean mention = ChatExtras.isMention(sender, body);
        if (cfg.chatEmotes) {
            msg = Emotes.apply(msg, cfg.chatEmoteFaces);
        }
        int nameMode = ChatColors.DEFAULT;
        int msgMode = ChatColors.DEFAULT;
        if (cfg.chatCustom) {
            ModConfig.PlayerChat pc = lookup(sender, cfg);
            nameMode = pc != null && pc.nameColor != ChatColors.DEFAULT ? pc.nameColor : cfg.chatDefaultNameColor;
            msgMode = pc != null && pc.msgColor != ChatColors.DEFAULT ? pc.msgColor : cfg.chatDefaultMsgColor;
        }
        if (mention) {
            ChatExtras.noteMention();
            if (cfg.chatMentionHighlight) {
                msgMode = cfg.chatMentionColor;
            }
        }
        if (cfg.chatBubbles) {
            WorldLabels.push(sender, msg.getString());
        }
        return Text.literal("<").append(ChatColors.apply(name, nameMode)).append(Text.literal("> "))
                .append(ChatColors.apply(msg, msgMode));
    }

    /**
     * The game can draw a player's face inside any text (an "object" text component), so Chat Heads
     * is just a small head in front of each line. Returns null if the sender is not an online player.
     */
    private static Text headFor(String sender) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.getNetworkHandler() == null) {
                return null;
            }
            String low = sender.toLowerCase(Locale.ROOT);
            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                String name = entry.getProfile().name();
                if (low.equals(name.toLowerCase(Locale.ROOT)) || java.util.Arrays.asList(low.split("[^a-z0-9_]+"))
                        .contains(name.toLowerCase(Locale.ROOT))) {
                    java.util.UUID id = entry.getProfile().id();
                    long msb = id.getMostSignificantBits();
                    long lsb = id.getLeastSignificantBits();
                    String json = "{\"object\":\"player\",\"player\":{\"id\":["
                            + (int) (msb >> 32) + "," + (int) msb + "," + (int) (lsb >> 32) + "," + (int) lsb
                            + "]},\"hat\":true}";
                    JsonElement element = JsonParser.parseString(json);
                    return TextCodecs.CODEC.parse(JsonOps.INSTANCE, element).result().orElse(null);
                }
            }
        } catch (RuntimeException e) {
            // never break chat over a head
        }
        return null;
    }

    private static Text asText(Object o) {
        return o instanceof Text t ? t : Text.literal(String.valueOf(o));
    }

    public static ModConfig.PlayerChat lookup(String sender, ModConfig cfg) {
        String low = sender.toLowerCase(Locale.ROOT);
        ModConfig.PlayerChat pc = cfg.chatPlayers.get(low);
        if (pc != null) {
            return pc;
        }
        for (String token : low.split("[^a-z0-9_]+")) {
            if (!token.isEmpty()) {
                pc = cfg.chatPlayers.get(token);
                if (pc != null) {
                    return pc;
                }
            }
        }
        return null;
    }

    /** A sample chat line for previews in the settings screens. */
    public static Text sample(String name, int nameMode, int msgMode) {
        Text msg = Emotes.apply(Text.literal("hey everyone :smile: nice build :fire:"), false);
        return Text.literal("<").append(ChatColors.apply(Text.literal(name), nameMode))
                .append(Text.literal("> ")).append(ChatColors.apply(msg, msgMode));
    }
}
