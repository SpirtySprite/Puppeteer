# Puppeteer

PNJ pour Paper et Folia 1.21, entièrement décrits dans `npcs.yml`. Ils n'existent qu'en paquets :
aucune entité réelle, aucun tick serveur. Joueurs avec skin, villageois, chats, zombies ou toute
entité vivante, avec lignes de texte, équipement, poses, animations et actions au clic.

`/npc reload` compare chaque PNJ à sa version en place et ne recrée que ceux qui ont changé. Un PNJ
dont le monde n'est pas encore chargé attend et apparaît tout seul au chargement du monde. Les PNJ
réservés à une permission ne sont visibles que par les joueurs qui l'ont, sans délai.

## Installation

1. Placez `Puppeteer.jar` dans `plugins/`.
2. Démarrez le serveur : `npcs.yml` (avec quelques exemples désactivés) et `messages.yml` sont créés
   dans `plugins/Puppeteer/`.

Optionnel : PlaceholderAPI (placeholders dans les textes), Vault (actions `GIVE_MONEY` et `TAKE_MONEY`).

## Reglages globaux

| Cle | Defaut | Effet |
|---|---|---|
| `settings.view-distance` | `48` | distance d'affichage en blocs, entre 8 et 256 |
| `settings.default-cooldown-ms` | `500` | delai entre deux clics quand un PNJ n'en precise pas |
| `settings.skin-cache-minutes` | `30` | duree de conservation des skins telecharges |
| `settings.debug` | `false` | journal detaille de FoliaNPC |
| `settings.nametag-style` | vanilla | style par defaut des lignes au-dessus des PNJ, memes cles que `nametag-style` |

## Options d'un PNJ

Chaque PNJ vit sous `npcs.<id>`, avec un identifiant en minuscules, chiffres, `_` et `-`.
Seul `location.world` est obligatoire.

| Cle | Defaut | Effet |
|---|---|---|
| `enabled` | `true` | `false` garde la definition sans afficher le PNJ |
| `name` | l'identifiant | nom interne, repris par `%npc%` |
| `type` | `PLAYER` | `PLAYER` ou toute entite vivante (`VILLAGER`, `CAT`, `ZOMBIE`...) |
| `location` | | `world`, `x`, `y`, `z`, `yaw`, `pitch` |
| `skin` | aucun | pseudo, lien `https://`, `mirror`, ou section `value`/`signature`, `url`, `player`, `mirror` |
| `nametag` | aucun | lignes au-dessus du PNJ, MiniMessage ou codes `&`, placeholders par joueur |
| `nametag-refresh-ticks` | `0` | rafraichit les lignes pour les placeholders qui bougent |
| `nametag-style.background` | `#000000` | couleur du fond des lignes, `#RRGGBB` ou un nom (`black`, `dark_purple`...) |
| `nametag-style.background-opacity` | `25` | opacite du fond en pourcent, `0` retire le fond |
| `nametag-style.text-opacity` | `100` | opacite du texte en pourcent, en dessous de `10` le texte disparait |
| `nametag-style.shadow` | `false` | ombre sous le texte |
| `nametag-style.see-through` | `false` | lignes visibles a travers les blocs |
| `look-at-players` | `true` | le PNJ suit du regard le joueur le plus proche |
| `view-distance` | global | distance propre a ce PNJ, `0` reprend la valeur globale |
| `cooldown-ms` | global | delai entre deux clics d'un meme joueur |
| `permission` | aucune | seuls les joueurs ayant la permission voient le PNJ |
| `pose` | `STANDING` | `STANDING`, `CROUCHING`, `SLEEPING`, `SWIMMING`, `SITTING`, `FALL_FLYING`, `DYING` |
| `baby` | `false` | version bebe des mobs |
| `variant` | | nombre (lapin, perroquet, axolotl, cheval) ou nom (`black` pour un chat, un loup, une grenouille) |
| `villager.profession`, `villager.type`, `villager.level` | | apparence des villageois, niveau 1 a 5 |
| `appearance.glowing`, `appearance.glow-color` | `false` | contour lumineux et sa couleur (`aqua`, `gold`...) |
| `appearance.invisible` | `false` | corps invisible, equipement et nom visibles |
| `appearance.scale` | `1.0` | taille entre 0.1 et 16 |
| `appearance.skin-layers` | `true` | couches exterieures du skin |
| `appearance.collidable` | `true` | les joueurs peuvent pousser contre le PNJ |
| `appearance.show-in-tab` | `false` | apparait dans la liste des joueurs |
| `appearance.nametag-visible` | auto | force l'affichage du nom vanilla |
| `equipment.<emplacement>` | | matiere ou objet complet (`material`, `name`, `lore`, `glow`, `item-model`...) pour `hand`, `off-hand`, `head`, `chest`, `legs`, `feet`, `body` |
| `actions` | | liste d'actions executees au clic |

## Actions

Chaque action choisit son clic et son type. Les actions d'un meme clic s'executent dans l'ordre ;
`delay` compte en ticks depuis le clic. Une action `TAKE_MONEY` ou `REQUIRE_PERMISSION` qui
echoue annule toutes les suivantes.

| Cle | Defaut | Effet |
|---|---|---|
| `click` | `RIGHT` | `LEFT`, `RIGHT` ou `BOTH` (alias `GAUCHE`, `DROIT`, `LES_DEUX`) |
| `sneak` | tous | `true` accroupi seulement, `false` debout seulement |
| `type` | | voir ci-dessous |
| `value` | | texte, commande, cle ou destination selon le type ; une liste donne plusieurs lignes |
| `delay` | `0` | attente en ticks avant l'action |
| `permission` | aucune | action ignoree sans la permission, les suivantes continuent |
| `deny-message` | | message si la permission ou le paiement manque |
| `amount` | | montant, niveau d'effet ou nombre de particules |
| `duration` | `60` ou `200` | ticks d'affichage d'un titre ou duree d'un effet |
| `subtitle`, `fade-in`, `fade-out` | `10`, `20` | reglages des titres |
| `volume`, `pitch` | `1.0` | reglages des sons |

| Type | Valeur | Effet |
|---|---|---|
| `MESSAGE` | texte | message au joueur |
| `BROADCAST` | texte | message a tout le serveur |
| `ACTIONBAR` | texte | barre d'action du joueur |
| `TITLE` | texte | titre, avec `subtitle` |
| `PLAYER_COMMAND` | commande | executee par le joueur |
| `CONSOLE_COMMAND` | commande | executee par la console |
| `SOUND` | `entity.player.levelup` ou `ENTITY_PLAYER_LEVELUP` | son pour le joueur, sons de pack acceptes |
| `TELEPORT` | `monde x y z [yaw pitch]` | teleportation |
| `SERVER` | nom du serveur | envoi vers un autre serveur du proxy |
| `EFFECT` | `speed`, `haste`... | effet de potion, `amount` pour le niveau |
| `PARTICLE` | `HAPPY_VILLAGER`, `HEART`... | particules sans donnees, visibles par le joueur |
| `GIVE_MONEY` | message facultatif | credite `amount` |
| `TAKE_MONEY` | message facultatif | debite `amount`, sinon bloque la suite |
| `REQUIRE_PERMISSION` | permission | bloque la suite sans la permission |
| `SWING` | | le PNJ balance le bras |

Les textes acceptent `%player%`, `%npc%` et, si PlaceholderAPI est present, tous ses placeholders.

## Commande

`/npc` (alias `/pnj`, permission `puppeteer.admin.npc`) : `reload`, `list`, `info <id>`,
`create <id> [type]`, `delete <id>`, `movehere <id>`, `tp <id>`, `rename <id> <nom>`,
`skin <id> <joueur|url|mirror|none>`, `copy <id> <nouveau>`, `enable <id>`, `disable <id>`, `stats`.
Les modifications en jeu reecrivent `npcs.yml` puis rechargent seulement le PNJ concerne.

## Compilation

```bash
mvn package
```

Le plugin se trouve dans `target/Puppeteer.jar`. Java 21 est requis.
