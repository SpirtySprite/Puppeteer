package com.kirugoldzzzz.puppeteer.importer;

import com.kirugoldzzzz.puppeteer.common.text.Tr;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class CitizensSource implements NpcSource {

    @Override
    public String id() {
        return "citizens";
    }

    @Override
    public String plugin() {
        return "Citizens";
    }

    @Override
    public Result read(File pluginFolder) {
        Result result = new Result(plugin());
        File saves = new File(pluginFolder, "saves.yml");
        ConfigurationSection npcs = YamlConfiguration.loadConfiguration(saves).getConfigurationSection("npc");
        if (npcs == null) {
            result.warn(Tr.t("Aucun PNJ trouvé dans ") + saves.getPath());
            return result;
        }
        for (String key : npcs.getKeys(false)) {
            ConfigurationSection npc = npcs.getConfigurationSection(key);
            if (npc != null) {
                Npc converted = npc(key, npc, result);
                if (converted != null) {
                    result.add(converted);
                }
            }
        }
        return result;
    }

    static Npc npc(String key, ConfigurationSection npc, Result result) {
        String name = npc.getString("name", "NPC");
        ConfigurationSection traits = npc.getConfigurationSection("traits");
        ConfigurationSection location = traits == null ? null : traits.getConfigurationSection("location");
        if (location == null || location.getString("world") == null) {
            result.warn("citizens-" + key + Tr.t(" : pas de position, PNJ ignoré"));
            return null;
        }
        String id = NpcSource.slug(name) + "_" + key;
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("enabled", true);
        section.put("name", NpcSource.profileName(name));
        section.put("type", traits.getString("type", "PLAYER").toUpperCase(Locale.ROOT));
        section.put("location", NpcSource.location(location.getString("world"), location.getDouble("x"),
                location.getDouble("y"), location.getDouble("z"), location.getDouble("yaw"),
                location.getDouble("pitch")));
        Object skin = skin(traits.getConfigurationSection("skintrait"));
        if (skin != null) {
            section.put("skin", skin);
        }
        section.put("nametag", new ArrayList<>(List.of(NpcSource.text(name))));
        section.put("look-at-players", traits.getBoolean("lookclose.enabled", false));
        List<Map<String, Object>> actions = actions(id, traits.getConfigurationSection("commandtrait.commands"),
                result);
        if (!actions.isEmpty()) {
            section.put("actions", actions);
        }
        return new Npc(id, section);
    }

    static Object skin(ConfigurationSection trait) {
        if (trait == null) {
            return null;
        }
        String texture = trait.getString("textureRaw");
        String signature = trait.getString("signature");
        if (texture != null && !texture.isBlank() && signature != null && !signature.isBlank()) {
            Map<String, Object> skin = new LinkedHashMap<>();
            skin.put("value", texture);
            skin.put("signature", signature);
            return skin;
        }
        String name = trait.getString("skinName");
        return name == null || name.isBlank() ? null : name;
    }

    static List<Map<String, Object>> actions(String id, ConfigurationSection commands, Result result) {
        List<Map<String, Object>> actions = new ArrayList<>();
        if (commands == null) {
            return actions;
        }
        Set<String> unsupported = new TreeSet<>();
        for (String index : commands.getKeys(false)) {
            ConfigurationSection command = commands.getConfigurationSection(index);
            if (command == null || command.getString("command") == null) {
                continue;
            }
            String hand = command.getString("hand", "BOTH").toUpperCase(Locale.ROOT);
            String click = hand.endsWith("LEFT") ? "LEFT" : hand.endsWith("RIGHT") ? "RIGHT" : "BOTH";
            Boolean sneak = hand.startsWith("SHIFT_") ? Boolean.TRUE : null;
            String line = command.getString("command").replace("<p>", "%player%").replace("<player>", "%player%");
            String type = command.getBoolean("player", false) ? "PLAYER_COMMAND" : "CONSOLE_COMMAND";
            if (command.getBoolean("op", false)) {
                unsupported.add("op");
            }
            if (command.getDouble("cost", -1.0D) > 0.0D || command.getInt("experienceCost", -1) > 0) {
                unsupported.add("cost");
            }
            actions.add(NpcSource.action(click, sneak, type, line, Math.max(0, command.getInt("delay", 0))));
        }
        if (!unsupported.isEmpty()) {
            result.warn(id + Tr.t(" : options de commande ignorées ") + String.join(", ", unsupported));
        }
        return actions;
    }
}
