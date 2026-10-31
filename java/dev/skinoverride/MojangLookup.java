package dev.skinoverride;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Downloads the PUBLIC skin of any Minecraft username from Mojang and saves it as a
 * PNG in the skins folder. No login token is used or sent.
 */
public final class MojangLookup {

    public record Result(String file, boolean slim, String error) {
    }

    private static volatile boolean busy;

    private MojangLookup() {
    }

    public static boolean isBusy() {
        return busy;
    }

    /** Runs on a background thread; the callback is delivered on the client thread. */
    public static void fetch(String username, Consumer<Result> callback) {
        if (busy) {
            return;
        }
        busy = true;
        Thread thread = new Thread(() -> {
            Result result;
            try {
                result = doFetch(username);
            } catch (Exception e) {
                String reason = e.getMessage() == null ? e.toString() : e.getMessage();
                SkinOverrideClient.LOGGER.warn("Lookup of {} failed: {}", username, reason);
                result = new Result(null, false, "Lookup failed: " + reason);
            }
            Result finalResult = result;
            MinecraftClient.getInstance().execute(() -> {
                busy = false;
                callback.accept(finalResult);
            });
        }, "skinoverride-lookup");
        thread.setDaemon(true);
        thread.start();
    }

    private static Result doFetch(String username) throws Exception {
        if (!username.matches("[A-Za-z0-9_]{1,16}")) {
            return new Result(null, false, "Not a valid Minecraft username.");
        }
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        HttpResponse<String> idResponse = http.send(
                get("https://api.mojang.com/users/profiles/minecraft/" + username),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (idResponse.statusCode() == 404 || idResponse.statusCode() == 204) {
            return new Result(null, false, "No such player: " + username);
        }
        if (idResponse.statusCode() != 200) {
            return new Result(null, false, "Mojang returned " + idResponse.statusCode() + " (try again in a minute).");
        }
        JsonObject idJson = JsonParser.parseString(idResponse.body()).getAsJsonObject();
        String id = idJson.get("id").getAsString();
        String realName = idJson.get("name").getAsString();

        HttpResponse<String> profileResponse = http.send(
                get("https://sessionserver.mojang.com/session/minecraft/profile/" + id),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (profileResponse.statusCode() == 429) {
            return new Result(null, false, "Mojang is rate-limiting this lookup. Wait a minute and retry.");
        }
        if (profileResponse.statusCode() != 200) {
            return new Result(null, false, "Profile lookup returned " + profileResponse.statusCode() + ".");
        }
        JsonObject profile = JsonParser.parseString(profileResponse.body()).getAsJsonObject();
        JsonArray properties = profile.getAsJsonArray("properties");
        String texturesValue = null;
        for (JsonElement element : properties) {
            JsonObject property = element.getAsJsonObject();
            if ("textures".equals(property.get("name").getAsString())) {
                texturesValue = property.get("value").getAsString();
                break;
            }
        }
        if (texturesValue == null) {
            return new Result(null, false, realName + " has no skin data.");
        }
        String decoded = new String(Base64.getDecoder().decode(texturesValue), StandardCharsets.UTF_8);
        JsonObject textures = JsonParser.parseString(decoded).getAsJsonObject().getAsJsonObject("textures");
        if (textures == null || !textures.has("SKIN")) {
            return new Result(null, false, realName + " uses a default skin (nothing to download).");
        }
        JsonObject skin = textures.getAsJsonObject("SKIN");
        String url = skin.get("url").getAsString();
        boolean slim = skin.has("metadata")
                && skin.getAsJsonObject("metadata").has("model")
                && "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString());

        // Only accept Mojang's texture host.
        URI uri = URI.create(url);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (!host.equals("textures.minecraft.net")) {
            return new Result(null, false, "Unexpected texture host: " + host);
        }

        HttpResponse<byte[]> png = http.send(get(url), HttpResponse.BodyHandlers.ofByteArray());
        if (png.statusCode() != 200) {
            return new Result(null, false, "Texture download returned " + png.statusCode() + ".");
        }
        String fileName = realName + ".png";
        Path target = SkinStore.skinsDir().resolve(fileName);
        Files.write(target, png.body());
        return new Result(fileName, slim, null);
    }

    private static HttpRequest get(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "SkinOverride-Fabric-Mod")
                .GET()
                .build();
    }
}
