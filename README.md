# Iris's Overrides, Tweaks, Emote wheel and armor (Fabric, Minecraft 1.21.11)

Version 2.2.0. One jar, one mod entry, everything inside:

- **Iris's Overrides & Tweaks** (this project's own code): skin overrides, movable HUD, map, chat, freecam, chest sorter and more (below).
- **Emote Wheel + Hinata / maid armor** (nested inside the same jar as `emote_wheel`, see `emote-wheel/README.md`): 310+ emotes on a radial wheel, cat ears / tail / pet, particles, jutsu props and the armor outfits.

Drop the single jar into `mods` with Fabric API. To let other players see your emotes, install the same jar on the server too.

### How it is put together
The two parts are written against different Minecraft name sets (this part uses Yarn names, the Emote Wheel uses Mojang names), so they live in two Gradle modules (`/` and `/emote-wheel`) and the build nests the Emote Wheel jar inside the main jar. Settings -> Tools has buttons that open the Emote Wheel menu and settings. The emote screens (menu, wheel, settings, clone panel) use the same Iris theme as everything else and follow Settings -> Theme live.

Key conflict note: the Emote Wheel uses **B** (wheel) and **N** (menu), so "add waypoint" is **V**.

## New in 2.2.0
- **Armor texture switches:** Emote settings (Settings -> Cosmetics -> Armor options) has *Maid armor texture* and *Hinata armor texture* on/off. Off = the armor looks like normal diamond / netherite armor.
- **Armor visibility:** the same screen: Show all, Hide mine, Hide other players', Hide everyone's (an elytra stays visible).
- **Baby mobs:** the Hinata sleeve code no longer rewrites the arm size of other models, so baby drowned and zombies keep their own small arms.
- **Mobs no longer get cat ears or tails:** piglins, endermen and other mobs built from the player mesh now hide the ears and tail, and maid ears only show on an actual maid helmet.
- **Clean screenshot:** player name tags now stay visible (the game hides them with the HUD, so they are let through while the shot is taken), and Settings -> Visuals has a switch for each thing that hides: game HUD, IRIS panels, waypoint labels, chat bubbles, damage numbers, light overlay.
- **Hide HUD elements:** Settings -> HUD -> *Shown HUD elements* has a switch for every element (minimap, radar, info, clock ... ), plus a key ("Hide / show all IRIS HUD panels", unbound by default).
- **Villager trades panel:** look at a villager to see its enchanted book trades. The game only sends trades when you open them, so open each villager once; the panel remembers them until you leave the world.
- **Reroll with Trade Cycling:** the villager reroller can use the Trade Cycling mod's cycle key (C by default) instead of breaking the job block: *Method* = Auto (uses Trade Cycling if installed, and falls back to breaking the job block if the key does nothing), Trade Cycling mod, or Break job block. Pick the cycle key in the reroll setup if you rebound it. Trade Cycling has to be on the server too.

## New in 2.1.0
**Map**
- **Waypoint sharing:** Map -> *Share waypoints* (or the Share button on the full map). Keep a friends list, send a waypoint to public chat or as a private message to one friend, and save the ones people send you from an inbox (key **Y** saves the newest). Optionally save waypoints from friends automatically. It works through plain chat on any server: players without the mod just read the coordinates.
- **Cave map:** Off / Auto / Always. It shows a slice of the cave around your height (rock dark, floors lighter, water and lava in their colors) instead of the surface. Auto switches on when you are underground.
- **Entity radar:** a movable scope with switchable categories (players, hostile mobs, animals, villagers, dropped items, other mobs), range, height limit, optional names and arrows for things above or below you.

**Chat**
- **Timestamps** in front of every line, 12 or 24 hour, optional seconds.
- **Name mentions:** a sound and a highlighted line when someone else says your name (extra words can be added in `settings.json` as `chatMentionWords`).
- **Search** everything said this session (Search button in the chat screen, or Settings -> Chat). Click a result to copy it.

**Profiles and cosmetics**
- **Profiles:** save every setting under a name and load it again. Link a profile to a server or world and it switches on when you join; your normal settings return when you leave.
- **Capes and wings:** six built-in capes and five wings (wings re-texture the elytra), or drop your own 64x32 PNGs into `config/skinoverride/capes` and `/wings`. Built-in ones are shown to other players who have the mod when the server has it too; your own PNGs are only visible to you.
- **Weapon Master is now visible to others** (same condition: the mod on both sides). You can switch sharing and seeing others' weapons off.

**Tools**
- **Chest and inventory sorter:** Sort / Stash / Same / My inv buttons above chests and the inventory, plus optional automatic sorting when a chest or your inventory opens. *Stash* moves your items into the chest, *Same* only the kinds it already holds.
- **Villager trade reroller:** pick an enchantment, a minimum level and a maximum price. The mod breaks and replaces the villager's job block (keep it in your hotbar) and re-reads the trades until the one you want appears, then stops with the trade screen open. Works only on villagers you have not traded with. Stops if you open a menu or get hurt. Many servers forbid automation, so check the rules first.

### Server note
Sharing capes, wings and Weapon Master needs this mod on the server as well (it only relays a short text). The server never needs to know what a cape looks like. Everything else is client-side.

## Features
Press **K** for skins and **J** for settings. Other keys (all rebindable under Controls, search "IRIS"): **C** zoom (hold), **G** fullbright, **F9** clean screenshot, **F8** freecam, **F7** light level overlay, **M** full map, **V** add waypoint, **[ ]** minimap zoom, **R** sort chest / inventory, **B** (hold) emote wheel, **N** emote menu, **Y** save the newest shared waypoint. Unbound by default (set them under Controls): send my position, chat search, cycle cave map, toggle radar, villager reroller.

**Skins**
- 3D preview of the selected player; hover a skin to preview it before applying.
- Add skins by dragging a `.png` (e.g. from The Skindex) onto the window, dropping it in `config/skinoverride/skins/`, or typing a Minecraft username and pressing **Download skin**.
- Assignments are saved by player UUID, the GUI reopens on your last player, and **Restore last** brings back a player's previous skin.

**HUD** (Settings -> Edit HUD layout: drag to move, **scroll over an element to resize it**, right-click to reset its size)
- **Armor & held items:** row or column (to sit beside the hotbar), durability numbers or percent, flashing red when low.
- **Effect timers:** icon, name, level and countdown. The bar on the left drains and shifts green -> yellow -> orange -> red as the effect runs out.
- **Coordinates & FPS:** XYZ, facing direction, FPS and ping.
- **Day & time:** permanent day counter with a ticking in-game clock (24h or AM/PM, optional seconds, optional playtime line).
- **Subtitles:** the vanilla sound captions rebuilt as a movable, resizable element with direction arrows.
- **Keystrokes & CPS:** WASD, space and the mouse buttons light up when held; clicks per second under LMB / RMB.
- **Join / leave pop-ups:** a small notice when a player comes online or goes offline (optionally only for players on your per-player list), with a soft sound.
- **Crosshair inspect** and an **AFK list** (both optional).

**Pause menu panel:** the day number, exact ticking time of day, time until sunset / next day, and playtime (this world or server, this session, all worlds) plus the real clock.

**AFK detection:** tags above heads, in the skin screen, in the inspect overlay and the optional HUD list.

**Chat** (Settings -> Chat)
- Name and message colors for everyone, or per player with a color grid or a custom hex code. Solid, rainbow or color-cycle. Separate colors for join, leave, advancement and death messages.
- **Emotes:** `:smile: :heart: :fire: :poggies:` and more (16 faces/symbols). **Armor icons:** `:Dchest:` (diamond chestplate), `:LLeg:` (leather leggings), `:Ihelm:`, `:Gboot:` ... Letters: L leather, C chainmail, I iron, G gold, D diamond, N netherite; pieces: helm, chest, leg, boot. Long names work too (`:diamond_chestplate:`).
- **Speech bubbles** above nearby players, and **text to speech** through Minecraft's narrator.

**Freecam (F8):** the camera detaches from your player and flies freely through blocks. WASD moves, jump/sneak go up/down, sprint is a x3 boost, scroll changes the speed. Your character stays where it is (and is drawn there), actions are blocked while flying, and it exits if you get hurt. Everything is a setting.

**Light level:** a line in the info panel (block / sky light where you stand) and an overlay (F7) of colored squares on the floor around you: red = 0 (hostile mobs can spawn), yellow = 1-7, green = 8+.

**Map** (Settings -> Map): a live minimap built from the chunks around you (rotating or north-up, zoom 1-4, players, mobs, waypoints, coordinates) and a full map (**M**) you can pan and zoom. The map remembers everywhere you have been (saved per world / server). Waypoints: right-click the full map, press **V** to drop one at your feet, rename / recolor / delete them from the full map; they also float in the world with their distance. A waypoint is added automatically where you die.

**Chat heads:** a small face in front of each player's chat message (uses the game's built-in player heads).

**Chest sorter:** press **R** in a chest, barrel, ender chest, shulker box, hopper, dispenser or your own inventory to merge stacks and sort by item ID. It sorts whatever your mouse is over.

**Low fire:** when you burn, the flames only cover the bottom of the screen (adjustable height). Fire on the ground is untouched.

**Weapon Master** (Settings -> Tools): the swords, tools, tridents, bows, shield and banner from your hotbar are drawn on your own character (swords on the hips, the rest on the back), each category switchable. Client-only: only you see it, in third person or freecam.

**Combat** (Settings -> Combat; everything has an on/off switch)
- **Damage numbers:** floating numbers when a creature's health drops. "Only my hits" shows just creatures you were attacking; turn it off for all nearby damage (and optionally healing). Size, duration, decimals and color (auto by damage, a fixed color, rainbow or cycle) are adjustable.
- **Hit flash color:** recolors the red tint on hurt creatures (any color, rainbow or cycle) with adjustable strength.
- **Crosshair:** Vanilla, Dot, Plus, Plus + dot, Circle, Square or X with size, thickness, gap, color, outline, red-on-target and a hit marker. (Custom styles replace the vanilla attack indicator.)
- **Hotbar cooldowns:** a colored wipe over items on cooldown, the seconds left, and a green flash when ready.

**Nametags:** set a nametag color for everyone or per player (solid, rainbow or cycling; the rainbow animates).

**Visuals**
- Reveal invisible players, fullbright, instant zoom (hold C, scroll to change the level, no animation), clean screenshot (HUD hidden, name tags stay).

**Theme** (Settings -> Theme): presets (including *Cherry Blossom* and *Pink Night*), accent color, background color and panel opacity for every IRIS menu and HUD box.

## Limits
- Skins only change on your own screen. Other players keep seeing the real skin.
- Inspect and the 3D slots only work for players loaded in your world, and only show held/worn items: servers never send other players' inventories to your client, so no client mod can show them.
- Chat colors and emotes need servers that send normal chat messages; some chat plugins format messages in ways that can't be recolored. Colors are fixed when a message arrives, so rainbow does not animate.
- Speech bubbles and AFK tags are drawn from the camera's point of view and can be slightly off while your field of view is changing (sprinting, effects).
- AFK detection only watches players near you. A player starts counting from the moment you first see them, and being pushed (water, pistons) counts as moving.
- "Only my hits" is judged from your attacks (you pressed attack while aiming at the creature); the game does not tell the client who dealt each hit, so arrows and other indirect hits only show with "Only my hits" off.
- Weapon Master only shows on your own character (the game never sends other players' hotbars to your client), and its item positions are approximate.
- The map is drawn from chunks your game has loaded: it fills in as you explore. It does not do cave mode.
- Light overlay and freecam use the client's own data and rendering hooks; the overlay shows squares through walls within its radius.
- Mojang rate-limits username lookups; wait a minute if one fails.
- Mixins use Yarn names for 1.21.11. Other Minecraft versions need updates.

## Install
1. Install Fabric Loader 0.19.5+ and [Fabric API](https://modrinth.com/mod/fabric-api) for Minecraft 1.21.11.
2. Drop the jar from the **Releases** page (or `build/libs/`) into `.minecraft/mods`.

## Build
```
./gradlew build
```
The jar lands in `build/libs/` (use the one without `-sources`). Requires JDK 21.

## Put it on GitHub
```
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/<you>/iris-overrides-and-tweaks.git
git push -u origin main
```
Every push builds the mod with GitHub Actions (download the jar from the run's **Artifacts**). To publish a release jar:
```
git tag v2.0.0
git push origin v2.0.0
```

## License
GNU General Public License v3.0 (GPL-3.0-only), see `LICENSE`. You may use, change and share this mod, but anyone who distributes a modified version must release their complete source code under the same license, so it cannot be turned into a closed-source product. (The GPL still allows selling copies; it just requires the source to stay open.) Fabric API is Apache-2.0, which is compatible. Minecraft itself is not covered by this license.
