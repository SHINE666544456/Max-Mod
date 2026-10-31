from PIL import Image
import os

EQ = "src/main/resources/assets/emote_wheel/textures/entity/equipment/"
os.makedirs(EQ + "humanoid", exist_ok=True)
os.makedirs(EQ + "humanoid_leggings", exist_ok=True)

def c(h): h = h.lstrip("#"); return (int(h[0:2],16), int(h[2:4],16), int(h[4:6],16), 255)

PALETTES = {
    "pink": dict(DRESS=c("F49AC1"), DRESS_D=c("D86FA0"), TRIM=c("FFFFFF"), TRIM_D=c("E4DDEA"),
                 ACCENT=c("FF4F95"), SHOE=c("E0709F"), SHOE_D=c("B84C7C"),
                 STOCK=c("FFFFFF"), STOCK_D=c("E8E0EE"), EAR=c("F49AC1"), EAR_IN=c("FFD6E8")),
    "black": dict(DRESS=c("1D1D24"), DRESS_D=c("0F0F14"), TRIM=c("FFFFFF"), TRIM_D=c("E4DDEA"),
                  ACCENT=c("E8467F"), SHOE=c("15151A"), SHOE_D=c("08080B"),
                  STOCK=c("2A2A33"), STOCK_D=c("1A1A21"), EAR=c("1D1D24"), EAR_IN=c("F08CB4")),
}

def rect(im, x, y, w, h, col):
    for i in range(x, x+w):
        for j in range(y, y+h):
            im.putpixel((i, j), col)

def frill(im, x, y, w, a, b):
    for i in range(w):
        im.putpixel((x+i, y), a if i % 2 == 0 else b)

def make(name, P):
    D, DD, T, TD, A = P["DRESS"], P["DRESS_D"], P["TRIM"], P["TRIM_D"], P["ACCENT"]
    L1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    # ---- Helmet: frilled maid headband (head region 0..32 x 0..16) ----
    rect(L1, 8, 5, 8, 3, T); frill(L1, 8, 4, 8, T, TD)           # top face, front half
    for x in (0, 8, 16, 24):                                      # right/front/left/back
        rect(L1, x, 8, 8, 1, T); frill(L1, x, 9, 8, T, TD)
    rect(L1, 3, 9, 2, 2, A); rect(L1, 19, 9, 2, 2, A)             # side ribbons

    # ---- Cat ears (strip at 56..64 x 16..28) ----
    for v in (16, 22):
        rect(L1, 56, v, 8, 3, P["EAR"]);   rect(L1, 58, v+1, 1, 2, P["EAR_IN"])   # base + inner
        rect(L1, 56, v+3, 4, 3, P["EAR"]); rect(L1, 57, v+4, 1, 2, P["EAR_IN"])   # tip + inner

    # ---- Chestplate: bodice + apron ----
    rect(L1, 20, 20, 8, 12, D)                                    # front
    rect(L1, 20, 20, 8, 1, T)                                     # collar
    rect(L1, 22, 21, 4, 1, A); rect(L1, 23, 22, 2, 1, A)          # neck bow
    rect(L1, 22, 23, 4, 4, T)                                     # apron bib
    rect(L1, 21, 27, 6, 5, T); rect(L1, 21, 27, 6, 1, TD)         # apron skirt + tie
    frill(L1, 20, 31, 8, T, TD)                                   # hem frill
    for x in (16, 28):                                            # sides
        rect(L1, x, 20, 4, 12, D); rect(L1, x, 20, 4, 1, T); frill(L1, x, 31, 4, T, TD)
    rect(L1, 32, 20, 8, 12, D); rect(L1, 32, 20, 8, 1, T)        # back
    rect(L1, 33, 26, 2, 3, A); rect(L1, 37, 26, 2, 3, A)         # big back bow wings
    rect(L1, 35, 27, 2, 1, A); rect(L1, 35, 28, 2, 3, A)         # knot + tails
    frill(L1, 32, 31, 8, T, TD)
    rect(L1, 20, 16, 8, 4, D); rect(L1, 28, 16, 8, 4, DD)         # top / bottom

    # ---- Arms: puffy short sleeves + white cuffs ----
    rect(L1, 44, 16, 4, 4, D); rect(L1, 48, 16, 4, 4, D)
    for x in (40, 44, 48, 52):
        rect(L1, x, 20, 4, 4, D); rect(L1, x, 24, 4, 1, DD)
        frill(L1, x, 25, 4, T, TD)
        rect(L1, x, 29, 4, 2, T); frill(L1, x, 31, 4, T, TD)

    # ---- Boots (legs region) ----
    for x in (0, 4, 8, 12):
        frill(L1, x, 28, 4, T, TD)
        rect(L1, x, 29, 4, 2, P["SHOE"]); rect(L1, x, 31, 4, 1, P["SHOE_D"])
    for x in (4,):                                                # strap on the front face
        rect(L1, x, 29, 4, 1, A)
    L1.save(EQ + f"humanoid/maid_{name}.png")

    # ---- Leggings: skirt + stockings ----
    L2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    for (x, w) in ((16, 4), (20, 8), (28, 4), (32, 8)):           # hips on body faces
        rect(L2, x, 28, w, 4, D); frill(L2, x, 31, w, T, TD)
    rect(L2, 22, 28, 4, 3, T)                                     # apron on front hips
    rect(L2, 4, 16, 4, 4, D)
    for x in (0, 4, 8, 12):
        rect(L2, x, 20, 4, 5, D)                                  # skirt
        frill(L2, x, 25, 4, T, TD)                                # hem lace
        rect(L2, x, 26, 4, 6, P["STOCK"])                         # stockings
        frill(L2, x, 26, 4, T, TD)                                # stocking lace top
        rect(L2, x, 31, 4, 1, P["STOCK_D"])
    rect(L2, 6, 20, 2, 5, T)                                      # apron over front legs
    L2.save(EQ + f"humanoid_leggings/maid_{name}.png")

for n, p in PALETTES.items():
    make(n, p)
print("done")
