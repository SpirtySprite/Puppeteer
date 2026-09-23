package com.kirugoldzzzz.puppeteer.importer;

import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public interface NpcSource {

    List<NpcSource> ALL = List.of(new CitizensSource(), new FancyNpcsSource(), new ZnpcsPlusSource());

    String id();

    String plugin();

    Result read(File pluginFolder);

    static Optional<NpcSource> byId(String id) {
        String wanted = id == null ? "" : id.toLowerCase(Locale.ROOT);
        return ALL.stream().filter(source -> source.id().equals(wanted)).findFirst();
    }

    record Npc(String id, Map<String, Object> section) {
    }

    final class Result {

        private final String source;
        private final List<Npc> npcs = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public Result(String source) {
            this.source = source;
        }

        public String source() {
            return source;
        }

        public List<Npc> npcs() {
            return npcs;
        }

        public List<String> warnings() {
            return warnings;
        }

        public void add(Npc npc) {
            npcs.add(npc);
        }

        public void warn(String warning) {
            warnings.add(warning);
        }
    }

    Pattern LEGACY = Pattern.compile("[&§]([0-9a-fk-orA-FK-OR])");

    static String slug(String raw) {
        String slug = raw == null ? "" : LEGACY.matcher(raw).replaceAll("").replaceAll("<[^>]*>", "")
                .toLowerCase(Locale.ROOT).trim().replace(' ', '_').replaceAll("[^a-z0-9_-]", "");
        return slug.isEmpty() ? "npc" : slug;
    }

    static String profileName(String raw) {
        String plain = raw == null ? "" : LEGACY.matcher(raw).replaceAll("").replaceAll("<[^>]*>", "").strip();
        String clean = plain.replaceAll("[^A-Za-z0-9_ ]", "");
        if (clean.isBlank()) {
            clean = "NPC";
        }
        return clean.length() > 16 ? clean.substring(0, 16) : clean;
    }

    static Map<String, Object> location(String world, double x, double y, double z, double yaw, double pitch) {
        Map<String, Object> location = new LinkedHashMap<>();
        location.put("world", world);
        location.put("x", x);
        location.put("y", y);
        location.put("z", z);
        location.put("yaw", yaw);
        location.put("pitch", pitch);
        return location;
    }

    static Map<String, Object> action(String click, Boolean sneak, String type, Object value, long delayTicks) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("click", click);
        if (sneak != null) {
            action.put("sneak", sneak);
        }
        action.put("type", type);
        if (value != null) {
            action.put("value", value);
        }
        if (delayTicks > 0L) {
            action.put("delay", delayTicks);
        }
        return action;
    }

    static String text(String raw) {
        return raw == null ? null : ImportText.mini(raw);
    }

    static double number(ConfigurationSection section, String path) {
        return section == null ? 0.0D : section.getDouble(path);
    }
}
