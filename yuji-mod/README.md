# Yuji Itadori Mod — Minecraft 1.20.1 (Forge 47.3.0 + GeckoLib 4.4.9)

Mod fan-made : ajoute **Yuji Itadori** comme allié animé (GeckoLib).

## Obtenir le .jar SANS rien installer (GitHub)
1. Crée un compte sur github.com puis un nouveau dépôt (New repository).
2. Envoie TOUT le contenu du dossier `yuji-mod` dans ce dépôt (bouton *Add file > Upload files*,
   pense à inclure le dossier caché `.github`).
3. Onglet **Actions** > le workflow *Build mod* se lance tout seul (2-5 min).
4. Clique sur le run terminé > section **Artifacts** > télécharge `yuji-mod-jar`.
5. Dézippe : prends le fichier `yujimod-1.0.0.jar` (pas celui en `-sources`).

## Jouer
1. Installe **Minecraft Forge 1.20.1 (47.3.0 ou plus)** avec le lanceur officiel.
2. Mets dans le dossier `mods` (`%appdata%/.minecraft/mods`) :
   - `yujimod-1.0.0.jar`
   - **GeckoLib 4.4.9** pour 1.20.1 Forge (CurseForge / Modrinth)
3. Lance le profil Forge, crée un monde Créatif, onglet Spawn Eggs > Yuji Itadori.

## Installation du projet (méthode locale)
1. Installe **JDK 17**.
2. Télécharge le **Forge MDK 1.20.1-47.3.0** (files.minecraftforge.net) et copie dans ce dossier :
   `gradlew`, `gradlew.bat` et le dossier `gradle/` (le wrapper Gradle).
3. Ouvre le dossier dans IntelliJ IDEA (ou Eclipse) → "Import Gradle project".
4. Lance `./gradlew genIntellijRuns` puis la config **runClient**, ou `./gradlew build`
   (le .jar sort dans `build/libs/`). Il faut aussi GeckoLib 4.4.9 dans le dossier `mods/` du jeu.

## En jeu
- Œuf d'apparition : onglet créatif **Spawn Eggs** → *Yuji Itadori*.
- **Apprivoiser** : clic droit avec une **pomme dorée**.
- Clic droit main vide = s'asseoir / se lever. Clic droit avec de la nourriture = le soigner.
- Il attaque les monstres, défend son propriétaire et te suit.

## Techniques
| Technique | Effet |
|---|---|
| Coup de poing | attaque de base (4 dégâts) |
| **Black Flash** | 8 % de chance par attaque. Dégâts × 2.5^2.5 (≈ ×9.9), éclair noir/rouge. Entre dans **la Zone** : 10 s où la chance monte à 35 %+ et augmente à chaque Black Flash |
| **Divergent Fist** | 1er impact normal, **2e impact retardé** (~0.7 s) qui ignore l'armure |
| **Manji Kick** | bond + coup de pied, forte projection (portée 3.5 – 8 blocs) |
| **Renforcement d'énergie occulte** | sous 50 % de vie : Force II + Vitesse II + Résistance, 10 s (cooldown 60 s) |

## Modifier le modèle / les animations
- `tools/generate_assets.py` régénère modèle, animations et texture (`python3 tools/generate_assets.py`).
- Si un mouvement part dans le mauvais sens (bras en arrière…), mets `FWD = -1` dans ce script et relance-le.
- Tu peux remplacer par ton propre modèle fait dans **Blockbench** (format GeckoLib) : garde les noms d'os
  (`root, waist, body, head, hair, right_arm, left_arm, right_leg, left_leg`) et d'animations
  (`idle, walk, run, punch, black_flash, divergent_fist, manji_kick`, préfixe `animation.yuji_itadori.`).
- La texture fournie est un **placeholder original** : peins ta propre skin dans
  `src/main/resources/assets/yujimod/textures/entity/yuji_itadori.png`.

## Réglages (YujiEntity.java)
- `Technique` : `windup` = tick de l'impact (doit coller à l'animation), `recovery` = repos.
- `BLACK_FLASH_MULT`, `blackFlashChance()`, PV/dégâts dans `createAttributes()`.

## Idées pour la suite
Domaines/Sukuna (boss), objet doigt de Sukuna, sons personnalisés, particules custom,
spawn naturel, config, version Fabric.

Note : Yuji Itadori appartient à *Jujutsu Kaisen* (G. Akutami / Shueisha). Mod fan non officiel ;
fais attention si tu le publies publiquement (risque de retrait).
