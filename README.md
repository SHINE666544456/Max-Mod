# Emote Wheel (+ Maid & Hinata Armor)

One mod for **Minecraft 1.21.11 (Fabric)**. It replaces BOTH of your old mods: delete the old armor-mod jar and
the old emote-wheel jar, and install just this one (plus Fabric API).

## What's in it
- **310+ emotes**: greetings, poses, dances, actions, moods, silly, **anime** (Naruto, Boruto, Jujutsu Kaisen, DBZ, JoJo, One Piece, Demon Slayer, MHA, Bleach, and more) and **cat & maid**.
- **Particle FX**: party poppers, confetti, hearts, notes, flames, lightning, auras and more, tied to the emote you're playing. Nearby players with the mod see them too.
- A **Fortnite-style radial wheel**, 3 pages x 8 slots, with a gold rim, glowing hover, page tabs, slot numbers, favorites and a live centre preview. Fully customizable, with search. The last wheel page **and** the last library page/filter in the emote menu are saved.
- A **Fortnite-style radial wheel**, 3 pages x 8 slots, fully customizable, with favorites and search.
- A **live preview** of your own character in the menu: hover any emote to watch it.
- **Cat ears + tail** that appear during cat/maid emotes (or always, if you want - see Options).
- **Walk-while-emoting**: upper-body emotes (waves, dances, salutes...) and run-style emotes (Ninja Run, Gojo's float...) keep playing while you move.
- **Armor outfits** (the old armor mod): rename armor in an anvil.
- **Shadow Clone Jutsu** (only for the account named `SSK1P`) and **Sexy Jutsu** (cat-girl skin swap).

## Controls (rebindable in *Options > Controls > Key Binds > Emote Wheel*)
| Default | Action |
|---|---|
| **B** (hold) | Open the wheel. Point at a slice and **let go to play it**. Tap instead, then click a slice. |
| **N** | Emote menu: library, wheel editor, preview, options |
| *unbound* | Stop emote, and Quick Emote: Slot 1-8 |

On the wheel: **1/2/3** or mouse wheel = page, **right-click** a slice = edit it, **E** = full menu.
In the menu: pick a wheel slot, then click emotes to fill it. **Play**, **Favorite**, **Clear Slot**, and the options along the bottom.

## Armor outfits (rename in an anvil, exactly)
| Armor | Name | Outfit |
|---|---|---|
| Diamond | `Hinata` | Hinata outfit (thin sleeves) |
| Diamond | `made` or `maid` | Black maid outfit + cat ears |
| Netherite | `made` or `maid` | Pink maid outfit + cat ears |

(Client side, per piece. Armor trims work on top.)

## Shadow Clone Jutsu (SSK1P only)
Find it under **Anime** in the menu (it simply doesn't exist for anyone else). Do the hand seal and **four clones** poof into
existence around you. They copy everything you do: walking, jumping, sneaking, sprinting, swinging, your armor, held items, and your emotes.
Do the jutsu again and they poof away. Clones are client-side: **everyone who has this mod (and sees you) sees them**; nobody else does.
The check uses your account name (`SSK1P`) both in the client and on the server, so other players can't trigger it.
To change the name, edit `OWNER` in `States.java`.

## Sexy Jutsu (skin swap)
Under **Anime**. Hand seal, poof, and your skin becomes `Cat_Girl` (included in the mod at
`assets/emote_wheel/textures/skin/cat_girl.png`). Do it again to turn back. Everyone with the mod sees the swap.
It's available to everybody by default; set `CAT_GIRL_OWNER_ONLY = true` in `States.java` to lock it to `SSK1P` too.

## Do other people see my stuff?
Yes, if **the server and the other players also have this mod**. The server only relays "X started emote Y" / "X toggled clones".
Without the mod on the server, everything still works but only **you** see it.

## Options (bottom of the menu, saved in `config/emote_wheel.json`)
Stop on move, 3rd person while emoting, See others' emotes, and **Cat parts: Off / Emotes / Always** (Always is only visible to you).

## Emotes
- **Greetings** (18): Wave, Big Wave, Hi There, Salute, Bow, Gentleman's Bow, Blow Kiss, Thumbs Up, High Five, Handshake, Come Here, Hello Everyone, Hey, You!, Tip the Hat, Hug, Cheers!, Fist Bump, Bye Bye!
- **Poses** (35): T-Pose, A-Pose, Hands on Hips, Arms Crossed, Superhero, Thinker, Facepalm, Shrug, Pray, Zombie, Point Forward, To the Sky, Hands Up!, Flex, Victory, Peace Sign, Lunge, Hero Landing, Sit, Sit Cross-Legged, Meditate, Lounge, Cool Lean, Model Pose, At Attention, At Ease, Tiptoes, Crane Stance, Captain Pose, Dramatic Gaze, Flamingo, Fighting Stance, Lie Down, Hug Yourself, Hero Salute
- **Dances** (34): Disco, Night Fever, Floss, Dab, The Twist, Robot, Chicken Dance, Macarena, Letters (Y-M-C-A), Moonwalk, Running Man, Sprinkler, Shuffle, Headbang, Raise the Roof, Mix It Up, Windmill Arms, Helicopter, Slow Sway, Salsa, Hype Jump, Pogo, Bounce, Can-Can, Conductor, Air Guitar, Air Drums, Hula, Ballet Twirl, Cheerleader, Arm Ripple, Snap Along, Charleston, Side Step
- **Actions** (51): Jumping Jacks, Jump, Hop, Squats, Run in Place, Sprint, March, Kick, Punch, Shadow Boxing, Karate Chop, Sword Slash, Archer, Cast a Line, Chop Wood, Mining, Digging, Munch, Drink, Clap, Slow Clap, Standing Ovation, Fist Pump, Stretch, Side Stretch, Touch Your Toes, Push-Ups, Lie Face Down, Backflip, Front Flip, Cartwheel, Pirouette, Spin, Superman, Dribble, Jump Shot, Soccer Kick, Golf Swing, Baseball Swing, Tennis Serve, Freestyle Swim, Backstroke, Rowing, Tree Pose, Warrior Pose, Tai Chi, Bowling, Violin, Trumpet, Piano, DJ Scratch
- **Moods** (34): Laugh, Evil Laugh, Cry, Angry Stomp, Scared, Shiver, Nod (Yes), Shake Head (No), Confused, Sleepy, Yawn, Sigh, Exhausted, Dizzy, Proud, Shy, Sneaky, Look Around, Impatient, Surprised, Shocked, Worried, Bored, Disappointed, Love-Struck, Heartbroken, Sleeping, Standing Nap, Tie Your Shoes, Wipe Sweat, Shhh, Mic Drop, Evil Plan, Thumbs Down
- **Silly** (29): Party Popper, Confetti Burst, Sparkler, Fireworks Show, Penguin Waddle, Duck Walk, Bunny Hop, Frog Hop, Gorilla, T-Rex, Spooky Ghost, Jelly Wobble, Rubber Arms, Glitch, Flail, Earthquake, Selfie, Phone Call, Karaoke, Buffering..., Noodle Arms, Starfish, Scarecrow, Sloth Hang, Crab Walk, Runway Walk, Peekaboo, Knock Knock, Rock Paper Scissors
- **Anime** (100+): previous set plus Re:Zero (Aura Monster, Aura Farm, Return by Death, I Love Emilia, Witch's Scent, Barusu!, Betty's Contractor, Rem's Flail, Sword Saint, From Zero) and more JJK (Infinity, Six Eyes, Simple Domain, 7:3 Ratio, Cursed Speech, Piercing Blood, Divergent Fist, Sukuna's Laugh, Open... Fuga, Mahoraga Wheel, Hairpin)
- **Maid & Cat** (33): Maid Curtsy, Welcome Home, Master, Serve Tea, Sweep the Floor, Dust the Shelves, Wipe the Window, Nya~, Cat Pounce, Cat Stretch, Cat Loaf, Paw Wave, Heart Hands, Ear Wiggle, Please?, Cat Nap, Neko Dance, This Way, Master, Sparkle Pose, Yes, Master!, Paw Bow, Scratch Scratch, Cat Stalk, Paw Wash, Chase Your Tail, Bunny Kick, Pour Tea, Polish, Apron Twirl, Moe Moe Kyun!, Formal Bow, See You, Master!, Cat Pose, Neko Run

## Building
`./gradlew build` (Java 21+). The jar ends up in `build/libs/`. The included GitHub Action does this on every push.
Needs Minecraft 1.21.11, Fabric Loader 0.19.5+ and Fabric API.

## Adding your own emotes
Emotes are plain Java in `src/main/java/com/example/emotewheel/emote/`:

```java
Emotes.add("my_wave", "My Wave", GREET, "poppy", 0, (p, t) -> {
    p.rax = -2.75f;                       // right arm up
    p.raz = -0.3f + sin(t * 9) * 0.4f;    // sway it
});
```
`duration` 0 = plays until cancelled; seconds = one-shot. Add the id to `Flags.java` to make it walkable (`WALK`/`MOVE`).
See `Pose.java` for every field (arms, legs, head, body, `lean`, and whole-body `rootX/Y/Z`, `spin`, `flip`, `roll`).

## Extras
- `optional_icon_pack/` : an optional resource pack that gives Hinata armor its own inventory icons.
- `tools/` : the Python scripts that generate the armor textures.
