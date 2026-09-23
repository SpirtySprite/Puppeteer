package com.kirugoldzzzz.puppeteer.importer;

import com.kirugoldzzzz.puppeteer.common.text.Tr;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class FancyNpcsSource implements NpcSource {

    private static final Map<String, String> SLOTS = Map.of(
            "MAINHAND", "hand", "OFFHAND", "off-hand", "HEAD", "head", "CHEST", "chest", "LEGS", "legs",
            "FEET", "feet", "BODY", "body");

    @Override
    public String id() {
        return "fancynpcs";
    }

    @Override
    public String plugin() {
        return "FancyNpcs";
    }

    @Override
    public Result read(File pluginFolder) {
        Result result = new Result(plugin());
        File file = new File(pluginFolder, "npcs.yml");
        ConfigurationSection npcs = YamlConfiguration.loadConfiguration(file).getConfigurationSection("npcs");
        if (npcs == null) {
            result.warn(Tr.t("Aucun PNJ trouvé dans ") + file.getPath());
            return result;
        }
        for (String key : npcs.getKeys(false)) {
            ConfigurationSection npc = npcs.getConfigurationSection(key);
            if (npc != null) {
                Npc converted = npc(npc, result);
                if (converted != null) {
                    result.add(converted);
                }
            }
        }
        return result;
    }

    static Npc npc(ConfigurationSection npc, Result result) {
        String name = npc.getString("name", npc.getName());
        String id = NpcSource.slug(name);
        ConfigurationSection location = npc.getConfigurationSection("location");
        if (location == null || location.getString("world") == null) {
            result.warn(id + Tr.t(" : pas de position, PNJ ignoré"));
            return null;
        }
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("enabled", true);
        section.put("name", NpcSource.profileName(name));
        section.put("type", npc.getString("type", "PLAYER").toUpperCase(Locale.ROOT));
        section.put("location", NpcSource.location(location.getString("world"), location.getDouble("x"),
                location.getDouble("y"), location.getDouble("z"), location.getDouble("yaw"),
                location.getDouble("pitch")));
        Object skin = skin(npc);
        if (skin != null) {
            section.put("skin", skin);
        }
        String display = npc.getString("displayName", "<empty>");
        section.put("nametag", display.equalsIgnoreCase("<empty>") ? new ArrayList<>()
                : new ArrayList<>(List.of(NpcSource.text(display))));
        section.put("look-at-players", npc.getBoolean("turnToPlayer", false));
        if (npc.contains("interactionCooldown")) {
            section.put("cooldown-ms", Math.round(npc.getDouble("interactionCooldown") * 1000.0D));
        }
        Map<String, Object> appearance = new LinkedHashMap<>();
        appearance.put("glowing", npc.getBoolean("glowing", false));
        if (npc.getBoolean("glowing", false)) {
            appearance.put("glow-color", npc.getString("glowingColor", "white").toLowerCase(Locale.ROOT));
        }
        appearance.put("scale", npc.getDouble("scale", 1.0D));
        appearance.put("collidable", npc.getBoolean("collidable", true));
        appearance.put("show-in-tab", npc.getBoolean("showInTab", false));
        section.put("appearance", appearance);
        Map<String, Object> equipment = equipment(npc.getConfigurationSection("equipment"));
        if (!equipment.isEmpty()) {
            section.put("equipment", equipment);
        }
        List<Map<String, Object>> actions = actions(id, npc, result);
        if (!actions.isEmpty()) {
            section.put("actions", actions);
        }
        return new Npc(id, section);
    }

    static Object skin(ConfigurationSection npc) {
        if (npc.getBoolean("skin.mirrorSkin", false)) {
            return "mirror";
        }
        String value = npc.getString("skin.value");
        String signature = npc.getString("skin.signature");
        if (value != null && signature != null) {
            Map<String, Object> skin = new LinkedHashMap<>();
            skin.put("value", value);
            skin.put("signature", signature);
            return skin;
        }
        String identifier = npc.getString("skin.identifier", npc.getString("skin.uuid"));
        return identifier == null || identifier.isBlank() ? null : identifier;
    }

    private static Map<String, Object> equipment(ConfigurationSection section) {
        Map<String, Object> equipment = new LinkedHashMap<>();
        if (section == null) {
            return equipment;
        }
        for (String slot : section.getKeys(false)) {
            String target = SLOTS.get(slot.toUpperCase(Locale.ROOT));
            ItemStack item = target == null ? null : section.getItemStack(slot);
            if (item != null && !item.getType().isAir()) {
                equipment.put(target, Map.of("item", item));
            }
        }
        return equipment;
    }

    static List<Map<String, Object>> actions(String id, ConfigurationSection npc, Result result) {
        List<Map<String, Object>> actions = new ArrayList<>();
        Set<String> unsupported = new TreeSet<>();
        ConfigurationSection triggers = npc.getConfigurationSection("actions");
        if (triggers != null) {
            for (String trigger : triggers.getKeys(false)) {
                String click = switch (trigger.toUpperCase(Locale.ROOT)) {
                    case "LEFT_CLICK" -> "LEFT";
                    case "RIGHT_CLICK" -> "RIGHT";
                    case "ANY_CLICK" -> "BOTH";
                    default -> null;
                };
                ConfigurationSection steps = triggers.getConfigurationSection(trigger);
                if (click == null || steps == null) {
                    unsupported.add(trigger.toLowerCase(Locale.ROOT));
                    continue;
                }
                long delay = 0L;
                List<String> order = new ArrayList<>(steps.getKeys(false));
                order.sort((a, b) -> Integer.compare(number(a), number(b)));
                for (String step : order) {
                    String action = steps.getString(step + ".action", "").toLowerCase(Locale.ROOT);
                    String value = steps.getString(step + ".value", "");
                    String type = switch (action) {
                        case "message" -> "MESSAGE";
                        case "player_command" -> "PLAYER_COMMAND";
                        case "console_command" -> "CONSOLE_COMMAND";
                        case "send_to_server" -> "SERVER";
                        case "play_sound" -> "SOUND";
                        case "need_permission" -> "REQUIRE_PERMISSION";
                        default -> null;
                    };
                    if (action.equals("wait")) {
                        delay += Math.round(parse(value) * 20.0D);
                    } else if (type == null) {
                        unsupported.add(action);
                    } else {
                        String converted = type.equals("MESSAGE") ? NpcSource.text(value) : value;
                        actions.add(NpcSource.action(click, null, type,
                                converted.replace("{player}", "%player%"), delay));
                    }
                }
            }
        }
        legacy(npc, actions);
        if (!unsupported.isEmpty()) {
            result.warn(id + Tr.t(" : actions ignorées ") + String.join(", ", unsupported));
        }
        return actions;
    }

    private static void legacy(ConfigurationSection npc, List<Map<String, Object>> actions) {
        for (String message : npc.getStringList("messages")) {
            actions.add(NpcSource.action("BOTH", null, "MESSAGE", NpcSource.text(message), 0L));
        }
        for (String command : npc.getStringList("playerCommands")) {
            actions.add(NpcSource.action("BOTH", null, "PLAYER_COMMAND", command.replace("{player}", "%player%"), 0L));
        }
        for (String command : npc.getStringList("serverCommands")) {
            actions.add(NpcSource.action("BOTH", null, "CONSOLE_COMMAND", command.replace("{player}", "%player%"), 0L));
        }
    }

    private static int number(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException invalid) {
            return Integer.MAX_VALUE;
        }
    }

    private static double parse(String raw) {
        try {
            return Math.max(0.0D, Double.parseDouble(raw.trim()));
        } catch (NumberFormatException invalid) {
            return 0.0D;
        }
    }
}
