package com.kirugoldzzzz.puppeteer.importer;

import com.kirugoldzzzz.puppeteer.common.text.Tr;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class ZnpcsPlusSource implements NpcSource {

    @Override
    public String id() {
        return "znpcsplus";
    }

    @Override
    public String plugin() {
        return "ZNPCsPlus";
    }

    @Override
    public Result read(File pluginFolder) {
        Result result = new Result(plugin());
        File[] files = new File(pluginFolder, "data").listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            if (new File(pluginFolder, "znpcsplus.sqlite").isFile()) {
                result.warn(Tr.t("ZNPCsPlus stocke ses PNJ en base de données, passez-le en YAML pour importer"));
            } else {
                result.warn(Tr.t("Aucun PNJ trouvé dans ") + new File(pluginFolder, "data").getPath());
            }
            return result;
        }
        Arrays.sort(files);
        for (File file : files) {
            Npc npc = npc(YamlConfiguration.loadConfiguration(file), result);
            if (npc != null) {
                result.add(npc);
            }
        }
        return result;
    }

    static Npc npc(ConfigurationSection yaml, Result result) {
        String id = NpcSource.slug(yaml.getString("id", "npc"));
        String world = yaml.getString("world");
        ConfigurationSection location = yaml.getConfigurationSection("location");
        if (world == null || location == null) {
            result.warn(id + Tr.t(" : pas de position, PNJ ignoré"));
            return null;
        }
        List<String> lines = yaml.getStringList("hologram.lines");
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("enabled", yaml.getBoolean("enabled", true));
        section.put("name", NpcSource.profileName(lines.isEmpty() ? id : lines.getFirst()));
        section.put("type", yaml.getString("type", "player").toUpperCase(Locale.ROOT));
        section.put("location", NpcSource.location(world, location.getDouble("x"), location.getDouble("y"),
                location.getDouble("z"), location.getDouble("yaw"), location.getDouble("pitch")));
        ConfigurationSection properties = yaml.getConfigurationSection("properties");
        Object skin = properties == null ? null : skin(properties.getString("skin"));
        if (skin != null) {
            section.put("skin", skin);
        }
        List<String> nametag = new ArrayList<>();
        for (String line : lines) {
            nametag.add(NpcSource.text(line));
        }
        section.put("nametag", nametag);
        String look = properties == null ? null : properties.getString("look");
        section.put("look-at-players", look != null && !look.equalsIgnoreCase("false")
                && !look.equalsIgnoreCase("fixed"));
        if (properties != null && properties.getString("glow") != null
                && !properties.getString("glow").equalsIgnoreCase("false")) {
            section.put("appearance", new LinkedHashMap<>(Map.of("glowing", true)));
        }
        List<Map<String, Object>> actions = actions(id, yaml.getStringList("actions"), result);
        if (!actions.isEmpty()) {
            section.put("actions", actions);
        }
        return new Npc(id, section);
    }

    static Object skin(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.split(";");
        return switch (parts[0].toLowerCase(Locale.ROOT)) {
            case "mirror" -> "mirror";
            case "fetching" -> parts.length > 1 ? String.join(";", Arrays.copyOfRange(parts, 1, parts.length)) : null;
            case "prefetched" -> {
                for (int index = 1; index + 2 < parts.length; index += 3) {
                    if (parts[index].equals("textures")) {
                        Map<String, Object> skin = new LinkedHashMap<>();
                        skin.put("value", parts[index + 1]);
                        skin.put("signature", parts[index + 2]);
                        yield skin;
                    }
                }
                yield null;
            }
            default -> null;
        };
    }

    static List<Map<String, Object>> actions(String id, List<String> raw, Result result) {
        List<Map<String, Object>> actions = new ArrayList<>();
        Set<String> unsupported = new TreeSet<>();
        for (String line : raw) {
            String[] parts = line.split(";");
            if (parts.length < 2) {
                continue;
            }
            String kind = parts[0].substring(parts[0].lastIndexOf('.') + 1);
            String type = switch (kind) {
                case "ConsoleCommandAction" -> "CONSOLE_COMMAND";
                case "PlayerCommandAction" -> "PLAYER_COMMAND";
                case "MessageAction" -> "MESSAGE";
                case "SwitchServerAction" -> "SERVER";
                default -> null;
            };
            if (type == null) {
                unsupported.add(kind);
                continue;
            }
            String value;
            try {
                value = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException invalid) {
                value = parts[1];
            }
            String interaction = parts.length > 3 ? parts[3].toUpperCase(Locale.ROOT) : "ANY_CLICK";
            String click = interaction.startsWith("LEFT") ? "LEFT" : interaction.startsWith("RIGHT") ? "RIGHT" : "BOTH";
            long delay = parts.length > 4 ? parse(parts[4]) : 0L;
            value = value.replace("{player}", "%player%");
            actions.add(NpcSource.action(click, null, type, type.equals("MESSAGE") ? NpcSource.text(value) : value,
                    delay));
        }
        if (!unsupported.isEmpty()) {
            result.warn(id + Tr.t(" : actions ignorées ") + String.join(", ", unsupported));
        }
        return actions;
    }

    private static long parse(String raw) {
        try {
            return Math.max(0L, Long.parseLong(raw.trim()));
        } catch (NumberFormatException invalid) {
            return 0L;
        }
    }
}
