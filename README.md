# Puppeteer

Packet-based NPCs for Paper and Folia 1.21 and 26.x, fully described in `npcs.yml`. They exist only as
packets: no real entity, no server tick, Folia-native. Players with skins, villagers, cats, zombies or
any living entity, with nametag lines, equipment, poses, animations and click actions.

`/npc reload` compares every NPC with the live one and only recreates those that changed. An NPC whose
world is not loaded yet waits and appears on its own when the world loads. NPCs restricted to a
permission are only visible to players who have it, with no delay.

## Installation

1. Drop `Puppeteer.jar` into `plugins/`.
2. Start the server: `config.yml`, `npcs.yml` (with a few disabled examples) and the language files are
   created in `plugins/Puppeteer/`.

Optional: PlaceholderAPI (placeholders in texts), Vault (`GIVE_MONEY` and `TAKE_MONEY` actions).

## Languages

Puppeteer ships in English and French. Set the language in `config.yml`:

```yaml
language: en
```

Use `fr` for French. On first start, the example `npcs.yml` is written in the chosen language.

- `lang/messages_<language>.yml` holds every chat message. Edit it freely.
- `lang/<language>.yml` translates the menu and item texts. Add or override any entry to customise a
  label. Missing entries fall back to the original text.

To add a language, copy `lang/messages_en.yml` and `lang/en.yml` to `messages_<code>.yml` and
`<code>.yml`, translate them, and set `language: <code>`. Changing the language needs a restart.

## Global settings

| Key | Default | Effect |
|---|---|---|
| `settings.view-distance` | `48` | display distance in blocks, between 8 and 256 |
| `settings.default-cooldown-ms` | `500` | delay between two clicks when an NPC does not set one |
| `settings.skin-cache-minutes` | `30` | how long downloaded skins are kept |
| `settings.debug` | `false` | verbose FoliaNPC logging |
| `settings.nametag-style` | vanilla | default style of the lines above NPCs, same keys as `nametag-style` |

## NPC options

Every NPC lives under `npcs.<id>`, with an id made of lowercase letters, digits, `_` and `-`. Only
`location.world` is required.

| Key | Default | Effect |
|---|---|---|
| `enabled` | `true` | `false` keeps the definition without showing the NPC |
| `name` | the id | internal name, used by `%npc%` |
| `type` | `PLAYER` | `PLAYER` or any living entity (`VILLAGER`, `CAT`, `ZOMBIE`...) |
| `location` | | `world`, `x`, `y`, `z`, `yaw`, `pitch` |
| `skin` | none | player name, `https://` link, `mirror`, or a `value`/`signature`, `url`, `player`, `mirror` section |
| `nametag` | none | lines above the NPC, MiniMessage or `&` codes, per player placeholders |
| `nametag-refresh-ticks` | `0` | refreshes the lines for placeholders that change |
| `nametag-style.background` | `#000000` | line background colour, `#RRGGBB` or a name (`black`, `dark_purple`...) |
| `nametag-style.background-opacity` | `25` | background opacity in percent, `0` removes it |
| `nametag-style.text-opacity` | `100` | text opacity in percent, below `10` the text disappears |
| `nametag-style.shadow` | `false` | text shadow |
| `nametag-style.see-through` | `false` | lines visible through blocks |
| `look-at-players` | `true` | the NPC follows the nearest player with its eyes |
| `view-distance` | global | distance for this NPC, `0` uses the global value |
| `cooldown-ms` | global | delay between two clicks from the same player |
| `permission` | none | only players with the permission see the NPC |
| `pose` | `STANDING` | `STANDING`, `CROUCHING`, `SLEEPING`, `SWIMMING`, `SITTING`, `FALL_FLYING`, `DYING` |
| `baby` | `false` | baby version of mobs |
| `variant` | | number (rabbit, parrot, axolotl, horse) or name (`black` for a cat, a wolf, a frog) |
| `villager.profession`, `villager.type`, `villager.level` | | villager appearance, level 1 to 5 |
| `appearance.glowing`, `appearance.glow-color` | `false` | glowing outline and its colour (`aqua`, `gold`...) |
| `appearance.invisible` | `false` | invisible body, equipment and name still visible |
| `appearance.scale` | `1.0` | size between 0.1 and 16 |
| `appearance.skin-layers` | `true` | outer skin layers |
| `appearance.collidable` | `true` | players can push against the NPC |
| `appearance.show-in-tab` | `false` | appears in the player list |
| `appearance.nametag-visible` | auto | forces the vanilla name to show |
| `equipment.<slot>` | | material or full item (`material`, `name`, `lore`, `glow`, `item-model`...) for `hand`, `off-hand`, `head`, `chest`, `legs`, `feet`, `body` |
| `actions` | | list of actions run on click |

## Actions

Every action picks its click and its type. Actions on the same click run in order; `delay` counts in
ticks from the click. A `TAKE_MONEY` or `REQUIRE_PERMISSION` action that fails cancels all the
following ones.

| Key | Default | Effect |
|---|---|---|
| `click` | `RIGHT` | `LEFT`, `RIGHT` or `BOTH` |
| `sneak` | any | `true` sneaking only, `false` standing only |
| `type` | | see below |
| `value` | | text, command, key or destination depending on the type; a list gives several lines |
| `delay` | `0` | wait in ticks before the action |
| `permission` | none | action skipped without the permission, the following ones continue |
| `deny-message` | | message when the permission or the payment is missing |
| `amount` | | amount, effect level or number of particles |
| `duration` | `60` or `200` | ticks a title stays or an effect lasts |
| `subtitle`, `fade-in`, `fade-out` | `10`, `20` | title settings |
| `volume`, `pitch` | `1.0` | sound settings |

| Type | Value | Effect |
|---|---|---|
| `MESSAGE` | text | message to the player |
| `BROADCAST` | text | message to the whole server |
| `ACTIONBAR` | text | player's action bar |
| `TITLE` | text | title, with `subtitle` |
| `PLAYER_COMMAND` | command | run by the player |
| `CONSOLE_COMMAND` | command | run by the console |
| `SOUND` | `entity.player.levelup` or `ENTITY_PLAYER_LEVELUP` | sound for the player, resource pack sounds accepted |
| `TELEPORT` | `world x y z [yaw pitch]` | teleport |
| `SERVER` | server name | sends the player to another proxy server |
| `EFFECT` | `speed`, `haste`... | potion effect, `amount` for the level |
| `PARTICLE` | `HAPPY_VILLAGER`, `HEART`... | particles without extra data, visible to the player |
| `GIVE_MONEY` | optional message | credits `amount` |
| `TAKE_MONEY` | optional message | debits `amount`, otherwise blocks the rest |
| `REQUIRE_PERMISSION` | permission | blocks the rest without the permission |
| `SWING` | | the NPC swings its arm |

Texts accept `%player%`, `%npc%` and, when PlaceholderAPI is installed, all of its placeholders.

## Command

`/npc` (aliases `/npcs`, `/pnj`, permission `puppeteer.admin.npc`): `reload`, `list`, `info <id>`,
`create <id> [type]`, `delete <id>`, `movehere <id>`, `tp <id>`, `rename <id> <name>`,
`skin <id> <player|url|mirror|none>`, `copy <id> <new>`, `enable <id>`, `disable <id>`, `stats`.
In-game edits rewrite `npcs.yml` and reload only the NPC concerned.

## Building

```bash
mvn package
```

The plugin is in `target/Puppeteer.jar`. Java 21 is required.
