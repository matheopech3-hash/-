#!/usr/bin/env python3
"""Génère le modèle (.geo.json), les animations (.animation.json) et une texture
placeholder ORIGINALE pour l'entité Yuji. Relance ce script après avoir modifié
les valeurs ci-dessous. Tu peux aussi tout remplacer par ton propre modèle fait
dans Blockbench (format GeckoLib) en gardant les mêmes noms d'os / d'animations.

Convention GeckoLib/Bedrock utilisée :
  - rotation X NEGATIVE sur un bras/une jambe = vers l'avant
  - rotation X POSITIVE sur le torse/la tête = penché vers l'avant
Si un mouvement part dans le mauvais sens en jeu, inverse FWD ci-dessous.
"""
import json, os, random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "yujimod")
NAME = "yuji_itadori"
FWD = 1  # mets -1 pour inverser le sens avant/arrière de TOUTES les rotations X

# ----------------------------------------------------------------- MODELE
def cube(origin, size, uv, pivot=None, rotation=None):
    c = {"origin": origin, "size": size, "uv": uv}
    if rotation:
        c["pivot"] = pivot
        c["rotation"] = rotation
    return c

HAIR_UV = [0, 32]  # zone 64x16 de la texture entièrement peinte en rose
spikes = [
    ([-4.0, 32.5, -3.5], [2.5, 2.5, 2.5], [0, 0, 18]),
    ([-1.2, 32.8, -4.0], [2.5, 3.0, 2.5], [-10, 0, 0]),
    ([1.5, 32.5, -3.5], [2.5, 2.5, 2.5], [0, 0, -18]),
    ([-3.5, 32.5, 0.0], [2.5, 2.5, 2.5], [0, 0, 20]),
    ([1.0, 32.5, 0.0], [2.5, 2.5, 2.5], [0, 0, -20]),
    ([-1.2, 32.5, 2.0], [2.5, 3.0, 2.5], [15, 0, 0]),
    ([-1.2, 32.5, -0.8], [2.5, 3.2, 2.5], [0, 0, 0]),
    ([-3.5, 30.0, -5.2], [2.0, 2.5, 1.0], [0, 0, 8]),
    ([0.0, 30.2, -5.2], [2.0, 2.5, 1.0], [0, 0, -8]),
]
hair_cubes = [cube([-4.5, 28.5, -4.5], [9, 4, 9], HAIR_UV)]
for o, s, r in spikes:
    piv = [o[0] + s[0] / 2, o[1], o[2] + s[2] / 2]
    hair_cubes.append(cube(o, s, HAIR_UV, piv, r) if any(r) else cube(o, s, HAIR_UV))

bones = [
    {"name": "root", "pivot": [0, 0, 0]},
    {"name": "waist", "parent": "root", "pivot": [0, 12, 0]},
    {"name": "body", "parent": "waist", "pivot": [0, 24, 0], "cubes": [cube([-4, 12, -2], [8, 12, 4], [16, 16])]},
    {"name": "head", "parent": "body", "pivot": [0, 24, 0], "cubes": [cube([-4, 24, -4], [8, 8, 8], [0, 0])]},
    {"name": "hair", "parent": "head", "pivot": [0, 24, 0], "cubes": hair_cubes},
    {"name": "right_arm", "parent": "body", "pivot": [-5, 22, 0], "cubes": [cube([-8, 12, -2], [4, 12, 4], [40, 16])]},
    {"name": "left_arm", "parent": "body", "pivot": [5, 22, 0], "cubes": [cube([4, 12, -2], [4, 12, 4], [32, 48])]},
    {"name": "right_leg", "parent": "root", "pivot": [-1.9, 12, 0], "cubes": [cube([-3.9, 0, -2], [4, 12, 4], [0, 16])]},
    {"name": "left_leg", "parent": "root", "pivot": [1.9, 12, 0], "cubes": [cube([-0.1, 0, -2], [4, 12, 4], [16, 48])]},
]
geo = {
    "format_version": "1.12.0",
    "minecraft:geometry": [{
        "description": {
            "identifier": f"geometry.{NAME}", "texture_width": 64, "texture_height": 64,
            "visible_bounds_width": 3, "visible_bounds_height": 3.5, "visible_bounds_offset": [0, 1.25, 0],
        },
        "bones": bones,
    }],
}

# -------------------------------------------------------------- ANIMATIONS
def kf(frames):
    """frames: liste de (temps, [x,y,z])"""
    return {f"{t:.2f}": v for t, v in frames}

def rx(x, y=0, z=0):  # applique FWD sur l'axe X
    return [x * FWD, y, z]

def A(length, loop, bones_):
    return {"loop": loop, "animation_length": length, "bones": bones_}

def R(frames):
    return {"rotation": kf(frames)}

def RP(rot, pos):
    return {"rotation": kf(rot), "position": kf(pos)}

GUARD_R = rx(-25, 0, -6)
GUARD_L = rx(-25, 0, 6)

anims = {}

anims["idle"] = A(3.0, True, {
    "waist": R([(0, [0, 8, 0])]),
    "body": R([(0, rx(0)), (1.5, rx(2)), (3.0, rx(0))]),
    "head": R([(0, rx(0)), (1.5, rx(-2)), (3.0, rx(0))]),
    "right_arm": R([(0, GUARD_R), (1.5, rx(-29, 0, -8)), (3.0, GUARD_R)]),
    "left_arm": R([(0, GUARD_L), (1.5, rx(-29, 0, 8)), (3.0, GUARD_L)]),
    "right_leg": R([(0, rx(6))]),
    "left_leg": R([(0, rx(-6))]),
})

anims["walk"] = A(1.0, True, {
    "waist": R([(0, [0, 4, 0]), (0.5, [0, -4, 0]), (1.0, [0, 4, 0])]),
    "body": RP([(0, rx(3))], [(0, [0, 0, 0]), (0.25, [0, 0.4, 0]), (0.5, [0, 0, 0]), (0.75, [0, 0.4, 0]), (1.0, [0, 0, 0])]),
    "right_leg": R([(0, rx(35)), (0.5, rx(-35)), (1.0, rx(35))]),
    "left_leg": R([(0, rx(-35)), (0.5, rx(35)), (1.0, rx(-35))]),
    "right_arm": R([(0, rx(-30, 0, -4)), (0.5, rx(30, 0, -4)), (1.0, rx(-30, 0, -4))]),
    "left_arm": R([(0, rx(30, 0, 4)), (0.5, rx(-30, 0, 4)), (1.0, rx(30, 0, 4))]),
})

anims["run"] = A(0.6, True, {
    "waist": R([(0, [0, 8, 0]), (0.3, [0, -8, 0]), (0.6, [0, 8, 0])]),
    "body": RP([(0, rx(14))], [(0, [0, 0, 0]), (0.15, [0, 0.8, 0]), (0.3, [0, 0, 0]), (0.45, [0, 0.8, 0]), (0.6, [0, 0, 0])]),
    "head": R([(0, rx(-8))]),
    "right_leg": R([(0, rx(60)), (0.3, rx(-60)), (0.6, rx(60))]),
    "left_leg": R([(0, rx(-60)), (0.3, rx(60)), (0.6, rx(-60))]),
    "right_arm": R([(0, rx(-55, 0, -6)), (0.3, rx(55, 0, -6)), (0.6, rx(-55, 0, -6))]),
    "left_arm": R([(0, rx(55, 0, 6)), (0.3, rx(-55, 0, 6)), (0.6, rx(55, 0, 6))]),
})

# impact au tick 6 (0.30 s) -> windup=6 dans YujiEntity.Technique.PUNCH
anims["punch"] = A(0.7, False, {
    "waist": R([(0, [0, 8, 0]), (0.1, [0, 28, 0]), (0.3, [0, -25, 0]), (0.45, [0, -25, 0]), (0.7, [0, 8, 0])]),
    "body": R([(0, rx(0)), (0.3, rx(8)), (0.7, rx(0))]),
    "right_arm": R([(0, GUARD_R), (0.1, rx(35, 0, -6)), (0.3, rx(-95, 0, -2)), (0.45, rx(-95, 0, -2)), (0.7, GUARD_R)]),
    "left_arm": R([(0, GUARD_L), (0.15, rx(-60, 0, 10)), (0.7, GUARD_L)]),
})

# impact au tick 14 (0.70 s) -> windup=14
anims["black_flash"] = A(1.1, False, {
    "root": {"position": kf([(0, [0, 0, 0]), (0.3, [0, -1.5, 0]), (0.6, [0, -1.5, 0]), (0.7, [0, 0, 0])])},
    "waist": R([(0, [0, 8, 0]), (0.3, [0, 40, 0]), (0.6, [0, 40, 0]), (0.7, [0, -35, 0]), (0.85, [0, -35, 0]), (1.1, [0, 8, 0])]),
    "body": R([(0, rx(0)), (0.3, rx(-6)), (0.7, rx(16)), (1.1, rx(0))]),
    "right_arm": R([(0, GUARD_R), (0.3, rx(70, 0, -10)), (0.6, rx(72, 0, -12)), (0.7, rx(-110, 0, 0)), (0.85, rx(-110, 0, 0)), (1.1, GUARD_R)]),
    "left_arm": R([(0, GUARD_L), (0.3, rx(-70, 0, 25)), (0.7, rx(40, 0, 10)), (1.1, GUARD_L)]),
    "right_leg": R([(0, rx(6)), (0.3, rx(25)), (0.7, rx(-40)), (1.1, rx(6))]),
    "left_leg": R([(0, rx(-6)), (0.3, rx(-20)), (0.7, rx(30)), (1.1, rx(-6))]),
})

# impact au tick 10 (0.50 s) -> windup=10 ; le "tremblement" apres l'impact = coup a retardement
anims["divergent_fist"] = A(1.0, False, {
    "waist": R([(0, [0, 8, 0]), (0.2, [0, 30, 0]), (0.5, [0, -22, 0]), (1.0, [0, 8, 0])]),
    "body": R([(0, rx(0)), (0.5, rx(10)), (1.0, rx(0))]),
    "right_arm": R([(0, GUARD_R), (0.2, rx(50, 0, -6)), (0.5, rx(-95, 0, -2)), (0.6, rx(-85, 0, -2)),
                    (0.65, rx(-100, 0, -2)), (0.7, rx(-88, 0, -2)), (0.75, rx(-98, 0, -2)), (1.0, GUARD_R)]),
    "left_arm": R([(0, GUARD_L), (0.2, rx(-50, 0, 12)), (0.5, rx(30, 0, 6)), (1.0, GUARD_L)]),
})

# impact au tick 12 (0.60 s) -> windup=12
anims["manji_kick"] = A(0.9, False, {
    "waist": R([(0, [0, 8, 0]), (0.6, [0, -30, 0]), (0.9, [0, 8, 0])]),
    "body": R([(0, rx(0)), (0.25, rx(-18)), (0.6, rx(-25)), (0.9, rx(0))]),
    "right_leg": R([(0, rx(6)), (0.25, rx(-40)), (0.6, rx(-100)), (0.75, rx(-100)), (0.9, rx(6))]),
    "left_leg": R([(0, rx(-6)), (0.3, rx(10)), (0.9, rx(-6))]),
    "right_arm": R([(0, GUARD_R), (0.3, rx(20, 0, -35)), (0.9, GUARD_R)]),
    "left_arm": R([(0, GUARD_L), (0.3, rx(20, 0, 35)), (0.9, GUARD_L)]),
})

anim_file = {"format_version": "1.8.0", "animations": {f"animation.{NAME}.{k}": v for k, v in anims.items()}}

# ----------------------------------------------------------------- TEXTURE
random.seed(7)
img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

def jitter(c, a=6):
    d = random.randint(-a, a)
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)

def fill(x, y, w, h, c, a=6):
    for i in range(x, x + w):
        for j in range(y, y + h):
            img.putpixel((i, j), jitter(c, a))

def faces(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }

SKIN = (240, 200, 160)
NAVY = (28, 37, 64)
RED = (150, 30, 45)
SHOE = (20, 20, 24)
HAIR = (230, 140, 140)

# tete
for f in faces(0, 0, 8, 8, 8).values():
    fill(*f, SKIN, 3)
fx, fy, _, _ = faces(0, 0, 8, 8, 8)["front"]
fill(fx + 1, fy + 3, 2, 1, (150, 80, 90), 0)      # sourcil gauche
fill(fx + 5, fy + 3, 2, 1, (150, 80, 90), 0)      # sourcil droit
fill(fx + 1, fy + 4, 1, 1, (255, 255, 255), 0)    # oeil gauche : blanc
fill(fx + 2, fy + 4, 1, 1, (120, 70, 40), 0)      # oeil gauche : iris
fill(fx + 5, fy + 4, 1, 1, (120, 70, 40), 0)
fill(fx + 6, fy + 4, 1, 1, (255, 255, 255), 0)
fill(fx + 3, fy + 6, 2, 1, (170, 90, 80), 0)      # bouche

# cheveux (zone 0,32 -> 64x16)
fill(0, 32, 64, 16, HAIR, 10)

# corps : veste bleu nuit + col rouge + boutons
for name, f in faces(16, 16, 8, 12, 4).items():
    fill(*f, NAVY, 4)
    if name in ("front", "back", "left", "right"):
        fill(f[0], f[1], f[2], 2, RED, 4)
bx, by, _, _ = faces(16, 16, 8, 12, 4)["front"]
for r in (4, 7, 10):
    fill(bx + 3, by + r, 1, 1, (212, 175, 55), 0)

# bras : manches + mains
for u, v in ((40, 16), (32, 48)):
    for name, f in faces(u, v, 4, 12, 4).items():
        fill(*f, NAVY, 4)
        if name in ("front", "back", "left", "right"):
            fill(f[0], f[1] + f[3] - 2, f[2], 2, SKIN, 3)
        if name == "bottom":
            fill(*f, SKIN, 3)

# jambes : pantalon + chaussures
for u, v in ((0, 16), (16, 48)):
    for name, f in faces(u, v, 4, 12, 4).items():
        fill(*f, NAVY, 4)
        if name in ("front", "back", "left", "right"):
            fill(f[0], f[1] + f[3] - 2, f[2], 2, SHOE, 3)
        if name == "bottom":
            fill(*f, SHOE, 3)

# ------------------------------------------------------------------ ECRITURE
os.makedirs(os.path.join(ROOT, "geo", "entity"), exist_ok=True)
os.makedirs(os.path.join(ROOT, "animations", "entity"), exist_ok=True)
os.makedirs(os.path.join(ROOT, "textures", "entity"), exist_ok=True)
with open(os.path.join(ROOT, "geo", "entity", f"{NAME}.geo.json"), "w") as f:
    json.dump(geo, f, indent=2)
with open(os.path.join(ROOT, "animations", "entity", f"{NAME}.animation.json"), "w") as f:
    json.dump(anim_file, f, indent=2)
img.save(os.path.join(ROOT, "textures", "entity", f"{NAME}.png"))
print("OK : modèle, animations et texture générés.")
