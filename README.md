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
| `once` | `false` | the action runs only the first time each player triggers it, remembered across restarts |
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
| `DIALOGUE` | list of lines | each click shows the next line to that player; the dialogue starts over after a minute without clicking |

Texts accept `%player%`, `%npc%` and, when PlaceholderAPI is installed, all of its placeholders.

## Patrols

An NPC can walk a loop, starting from its location, through a list of points, and back:

```yaml
patrol:
  speed: 0.8
  pause-ticks: 60
  points:
    - "4.5 64 4.5"
    - "4.5 64 0.5"
```

`speed` is in blocks per second, `pause-ticks` is the wait at each point. The NPC finds its way
around blocks; when no path exists it walks straight to the point. `/npc path add <id>` adds the
point where you stand, `/npc path clear <id>` removes the patrol.

## Command

`/npc` (aliases `/npcs`, `/pnj`, permission `puppeteer.admin.npc`): `reload`, `list`, `info <id>`,
`create <id> [type]`, `delete <id>`, `movehere <id>`, `tp <id>`, `rename <id> <name>`,
`skin <id> <player|url|mirror|none>`, `copy <id> <new>`, `enable <id>`, `disable <id>`, `stats`,
`path add|clear <id>`, `forget <id> [player]` (resets the `once` actions of an NPC, for one player or
everyone), `import <source>`.
In-game edits rewrite `npcs.yml` and reload only the NPC concerned. `/npc reload` also rereads
`config.yml` and the language files. Changing `language` takes full effect after a restart.

## Importing from other NPC plugins

Keep the other plugin's folder in `plugins/`, then run `/npc import <source>`:

| Source | Reads |
|---|---|
| `citizens` | `plugins/Citizens/saves.yml`: name, type, location, skin, look close and command trait |
| `fancynpcs` | `plugins/FancyNpcs/npcs.yml`: display name, type, location, skin, glow, scale, equipment and actions |
| `znpcsplus` | `plugins/ZNPCsPlus/data/*.yml`: hologram lines, type, location, skin, look and actions |

Commands become `CONSOLE_COMMAND` or `PLAYER_COMMAND` actions on the same click, messages become
`MESSAGE` actions, FancyNpcs waits become action delays, and player placeholders become `%player%`.
Imported NPCs are enabled right away, so disable the old plugin to avoid seeing each NPC twice.

An NPC whose id already exists in Puppeteer is skipped, so running the import twice does not
duplicate anything. Anything that could not be carried over (costs, op commands, random actions,
ZNPCsPlus database storage) is listed in the console after the import.

## Developer API

Add Puppeteer as a `depend` or `softdepend`, then get the service:

```java
PuppeteerApi.get().ifPresent(puppeteer -> {
    puppeteer.location("guide").ifPresent(player::teleport);
    puppeteer.setEnabled("banker", false).thenRun(() -> getLogger().info("banker hidden"));
});
```

`PuppeteerApi` lists NPC ids, their state (`ACTIVE`, `PENDING` while the world is not loaded,
`DISABLED`, `UNKNOWN`), their location and click count. `move`, `setEnabled` and `reload` rewrite
`npcs.yml` off the main thread and return a `CompletableFuture`. Unknown ids throw
`IllegalArgumentException`.

`NpcClickEvent` fires when a player clicks an NPC, before its actions run. It carries the NPC id,
the side (`rightClick()`) and `sneaking()`. Cancelling it skips every action of that click.

## Updates and metrics

On start Puppeteer checks the latest GitHub release and tells the console and players with
`puppeteer.admin.npc` when a newer version exists. Set `update-checker: false` in `config.yml` to
turn it off. Anonymous usage statistics go through bStats and follow the global bStats opt-out in
`plugins/bStats/config.yml`.

## Building

```bash
mvn package
```

The plugin is in `target/Puppeteer.jar`. Java 21 is required.
