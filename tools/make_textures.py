from PIL import Image
import os

R = "src/main/resources/"
EQ = R + "assets/emote_wheel/textures/entity/equipment/"
os.makedirs(EQ + "humanoid", exist_ok=True)
os.makedirs(EQ + "humanoid_leggings", exist_ok=True)
ICON = "optional_icon_pack/assets/minecraft/textures/item/"
os.makedirs(ICON, exist_ok=True)

def c(h, a=255):
    h = h.lstrip("#"); return (int(h[0:2],16), int(h[2:4],16), int(h[4:6],16), a)

CREAM=c("E6D3BC"); SHADE=c("C9B196"); FUR=c("F8F6F8"); FUR2=c("C9BDD8")
ZIP=c("9C9C9C"); BLUE=c("27447F"); PLATE=c("B9BFC9"); SWIRL=c("6B7280")
PANT=c("4B5278"); PANTD=c("252B47"); BAND=c("B8B8BD"); BANDD=c("8E8E95")
BOOT=c("2E4F94"); BOOTD=c("1F376B")

def rect(im, x, y, w, h, col):
    for i in range(x, x+w):
        for j in range(y, y+h):
            im.putpixel((i, j), col)

# ---------- Layer 1 (helmet / chest / boots), 64x32 ----------
L1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
# Head region (0..32, 0..16) stays fully transparent -> invisible helmet.

def jumper_face(x, y, w, zip_col=None, blue_collar=True):
    rect(L1, x, y, w, 12, CREAM)
    for i in range(w):                       # soft shading near the bottom of body
        L1.putpixel((x+i, y+8), SHADE)
    for i in range(w):                       # fur hem (rows 10-11)
        L1.putpixel((x+i, y+10), FUR if i % 2 == 0 else FUR2)
        L1.putpixel((x+i, y+11), FUR2 if i % 2 == 0 else FUR)
    if blue_collar:                          # forehead protector at the neck
        rect(L1, x, y, w, 1, BLUE)
    if zip_col:
        for j in range(1, 10):
            L1.putpixel((x+4, y+j), zip_col)

jumper_face(20, 20, 8, ZIP)                  # front
rect(L1, 22, 20, 4, 1, PLATE); L1.putpixel((23,20), SWIRL); L1.putpixel((24,20), SWIRL)
jumper_face(32, 20, 8)                       # back
jumper_face(16, 20, 4)                       # sides
jumper_face(28, 20, 4)
rect(L1, 20, 16, 8, 4, CREAM)                # top
rect(L1, 28, 16, 8, 4, FUR)                  # bottom (fur)

# Arms (right arm at 40,16; mirrored for left in 64x32 layout)
rect(L1, 44, 16, 4, 4, CREAM)                # top
for (x) in (40, 44, 48, 52):                 # outer, front, inner, back
    rect(L1, x, 20, 4, 8, CREAM)             # sleeve, rows 0-7; rows 8-11 left bare (hands)
    for i in range(4):
        L1.putpixel((x+i, 26), SHADE)
        L1.putpixel((x+i, 27), SHADE)        # cuff shade

# Boots (legs region at 0,16): sandals-style blue wraps, rows 8-11
for x in (0, 4, 8, 12):
    rect(L1, x, 20+7, 4, 1, BOOTD)
    rect(L1, x, 20+8, 4, 4, BOOT)
    for i in range(4):
        L1.putpixel((x+i, 20+11), BOOTD)
L1.save(EQ + "humanoid/hinata.png")

# ---------- Layer 2 (leggings), 64x32 ----------
L2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
rect(L2, 4, 16, 4, 4, PANT)                  # leg top
for x in (0, 4, 8, 12):
    rect(L2, x, 20, 4, 5, PANT)              # upper capri
    rect(L2, x, 25, 4, 3, PANTD)             # lower capri (darker), ends mid-calf
    rect(L2, x, 21, 4, 3, BAND)              # thigh bandage
    rect(L2, x, 22, 4, 1, BANDD)
L2.save(EQ + "humanoid_leggings/hinata.png")

# ---------- Optional 16x16 inventory icons ----------
def icon(name, pixels):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (x, y, w, h, col) in pixels: rect(im, x, y, w, h, col)
    im.save(ICON + name + ".png")

icon("hinata_chestplate", [
    (2,3,12,10,CREAM), (0,3,3,7,CREAM), (13,3,3,7,CREAM),
    (6,2,4,2,BLUE), (7,2,2,1,PLATE),
    (7,4,1,8,ZIP),
    (2,11,12,1,FUR), (2,12,12,2,FUR2), (3,13,2,1,FUR),(7,13,2,1,FUR),(11,13,2,1,FUR),
    (3,8,3,1,SHADE),(10,7,3,1,SHADE)])
icon("hinata_leggings", [
    (3,2,10,3,PANT), (3,5,4,7,PANT), (9,5,4,7,PANT),
    (3,7,4,2,BAND),(3,8,4,1,BANDD),
    (3,10,4,3,PANTD),(9,10,4,3,PANTD)])
icon("hinata_boots", [
    (2,6,4,6,BOOT),(10,6,4,6,BOOT),(2,6,4,1,BOOTD),(10,6,4,1,BOOTD),
    (1,12,6,2,BOOTD),(9,12,6,2,BOOTD)])
icon("hinata_helmet", [
    (1,6,14,4,BLUE),(5,5,6,6,PLATE),(7,7,2,2,SWIRL)])
print("done")
