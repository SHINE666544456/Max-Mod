#!/usr/bin/env python3
"""Generates the built-in cape and wings textures (64x32 PNGs). Run from the repo root: python3 tools/generate_cosmetics.py"""
import math, random
from PIL import Image

OUT = "src/main/resources/assets/skinoverride/textures/cosmetic/"

def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

def clamp(v):
    return max(0, min(255, int(v)))

def shade(c, f):
    return tuple(clamp(x * f) for x in c)

def make(name, top, bottom, accent, kind, motif, seed, wing=False):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (64, 32))
    px = img.load()
    for y in range(32):
        for x in range(64):
            t = (y % 22) / 21.0 if wing else min(y, 17) / 17.0
            c = lerp(top, bottom, t)
            if wing:
                # feather stripes
                f = 0.88 + 0.12 * math.sin(x * 1.6 + y * 0.35)
                c = shade(c, f)
            else:
                # soft vertical folds
                c = shade(c, 0.94 + 0.06 * math.sin(x * 1.2))
            px[x, y] = c + (255,)
    # motif
    for _ in range(90):
        x, y = rnd.randrange(64), rnd.randrange(32)
        if motif == "petal":
            px[x, y] = accent + (255,)
            if x + 1 < 64:
                px[x + 1, y] = shade(accent, 0.9) + (255,)
        elif motif == "star":
            px[x, y] = accent + (255,)
        elif motif == "leaf":
            for dx, dy in ((0, 0), (1, 0), (0, 1), (-1, 1)):
                if 0 <= x + dx < 64 and 0 <= y + dy < 32:
                    px[x + dx, y + dy] = accent + (255,)
        elif motif == "flame":
            for dy in range(3):
                if y + dy < 32:
                    px[x, y + dy] = lerp(accent, (255, 230, 120), dy / 2.0) + (255,)
        elif motif == "wave":
            yy = (y + int(2 * math.sin(x / 2.0))) % 32
            px[x, yy] = accent + (255,)
        elif motif == "frost":
            px[x, y] = (255, 255, 255, 255)
    # darker edges on the cape's visible faces so it reads as a cloth
    if not wing:
        for x in range(0, 23):
            for y in (0, 16):
                r, g, b, a = px[x, y]
                px[x, y] = shade((r, g, b), 0.8) + (255,)
    img.save(OUT + name + ".png")

capes = {
    "sakura":   ((255, 200, 220), (230, 120, 170), (255, 245, 250), "petal"),
    "midnight": ((20, 24, 60),    (60, 40, 110),   (255, 240, 170), "star"),
    "leaf":     ((70, 170, 80),   (30, 100, 50),   (190, 240, 120), "leaf"),
    "ember":    ((255, 150, 40),  (170, 30, 20),   (255, 200, 60),  "flame"),
    "ocean":    ((60, 160, 220),  (20, 70, 150),   (200, 240, 255), "wave"),
    "mono":     ((245, 247, 250), (190, 198, 210), (255, 255, 255), "frost"),
}
wings = {
    "sakura":  ((255, 215, 230), (240, 140, 185), (255, 250, 252), "petal"),
    "angel":   ((255, 255, 255), (210, 220, 240), (255, 250, 200), "star"),
    "ember":   ((255, 190, 70),  (190, 40, 20),   (255, 220, 100), "flame"),
    "void":    ((60, 20, 100),   (15, 5, 35),     (200, 120, 255), "star"),
    "crystal": ((170, 240, 255), (60, 150, 220),  (255, 255, 255), "frost"),
}
for i, (n, (t, b, a, m)) in enumerate(capes.items()):
    make("cape_" + n, t, b, a, "cape", m, 100 + i)
for i, (n, (t, b, a, m)) in enumerate(wings.items()):
    make("wings_" + n, t, b, a, "wings", m, 200 + i, wing=True)
print("ok")
