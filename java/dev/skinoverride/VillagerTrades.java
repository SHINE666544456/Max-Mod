package dev.skinoverride;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Remembers the enchanted book trades of every villager whose trade screen you opened, so the hover panel can show
 * them later when you just look at the villager. The game only sends a villager's trades when you open them, so a
 * villager you never opened shows no trades yet.
 */
public final class VillagerTrades {

    /** The enchanted book trades of one villager and how many other trades it has. */
    public record Info(List<String> books, int others) {
    }

    private static final Map<UUID, Info> CACHE = new HashMap<>();
    private static int timer;

    private VillagerTrades() {
    }

    public static void clear() {
        CACHE.clear();
    }

    public static Info get(UUID id) {
        return CACHE.get(id);
    }

    public static void tick(MinecraftClient mc) {
        if (++timer < 5) {
            return;
        }
        timer = 0;
        if (!(mc.currentScreen instanceof MerchantScreen screen)) {
            return;
        }
        VillagerEntity villager = TradeReroller.lastVillager();
        if (villager == null) {
            return;
        }
        TradeOfferList offers = screen.getScreenHandler().getRecipes();
        if (offers == null || offers.isEmpty()) {
            return;
        }
        List<String> books = new ArrayList<>();
        int others = 0;
        for (TradeOffer offer : offers) {
            String line = TradeReroller.bookLine(offer);
            if (line != null) {
                books.add(line);
            } else {
                others++;
            }
        }
        if (CACHE.size() > 300) {
            CACHE.clear();
        }
        CACHE.put(villager.getUuid(), new Info(books, others));
    }
}
