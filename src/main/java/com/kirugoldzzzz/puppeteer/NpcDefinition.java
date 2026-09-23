package com.kirugoldzzzz.puppeteer;

import com.kirugoldzzzz.puppeteer.common.text.Tr;

import net.folianpc.api.NpcPose;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public record NpcDefinition(String id, boolean enabled, String name, EntityType type, String world,
                            double x, double y, double z, float yaw, float pitch,
                            NpcSkinSource skin, List<String> nametag, Boolean nametagVisible,
                            long nametagRefreshTicks, NpcNametagStyle nametagStyle,
                            boolean lookAtPlayers, double viewDistance,
                            long cooldownMillis, boolean glowing, NamedTextColor glowColor,
                            boolean invisible, boolean skinLayers, double scale, boolean collidable,
                            boolean showInTab, NpcPose pose, boolean baby, Integer variant,
                            String variantName, String villagerProfession, String villagerType,
                            int villagerLevel, Map<EquipmentSlot, NpcEquipment> equipment,
                            String permission, List<NpcActionSpec> actions) {

    public static final double MIN_SCALE = 0.1D;
    public static final double MAX_SCALE = 16.0D;

    private static final Pattern ID = Pattern.compile("[a-z0-9_-]{1,32}");
    private static final String STYLE_KEY = "nametag-style";

    public static boolean validId(String id) {
        return id != null && ID.matcher(id).matches();
    }

    public static NpcDefinition parse(String id, ConfigurationSection section, NpcDefaults defaults,
                                      List<String> warnings) {
        if (!validId(id)) {
            warnings.add(id + Tr.t(" : identifiant invalide, lettres minuscules, chiffres, _ et - uniquement"));
            return null;
        }
        ConfigurationSection location = section.getConfigurationSection("location");
        String world = location == null ? null : location.getString("world");
        if (world == null || world.isBlank()) {
            warnings.add(id + Tr.t(" : position absente, il faut au moins location.world"));
            return null;
        }
        EntityType type = entityType(section.getString("type", "PLAYER"));
        if (type == null) {
            warnings.add(id + Tr.t(" : type d'entité invalide \"") + section.getString("type") + "\"");
            return null;
        }
        NamedTextColor glowColor = glowColor(section.getString("appearance.glow-color"), id, warnings);
        NpcPose pose = pose(section.getString("pose"), id, warnings);

        Integer variant = section.isInt("variant") ? section.getInt("variant") : null;
        String variantName = variant == null ? registryName(section.getString("variant")) : null;

        return new NpcDefinition(id,
                section.getBoolean("enabled", true),
                section.getString("name", id),
                type,
                world.trim(),
                location.getDouble("x"),
                location.getDouble("y"),
                location.getDouble("z"),
                (float) location.getDouble("yaw"),
                (float) location.getDouble("pitch"),
                NpcSkinSource.read(section.get("skin")),
                List.copyOf(section.getStringList("nametag")),
                section.isBoolean("appearance.nametag-visible")
                        ? section.getBoolean("appearance.nametag-visible") : null,
                Math.max(0L, section.getLong("nametag-refresh-ticks", 0L)),
                NpcNametagStyle.read(section.getConfigurationSection(STYLE_KEY), defaults.nametagStyle(),
                        id + " " + STYLE_KEY, warnings),
                section.getBoolean("look-at-players", true),
                Math.max(0.0D, section.getDouble("view-distance", 0.0D)),
                Math.max(0L, section.getLong("cooldown-ms", defaults.cooldownMillis())),
                section.getBoolean("appearance.glowing", false),
                glowColor,
                section.getBoolean("appearance.invisible", false),
                section.getBoolean("appearance.skin-layers", true),
                Math.max(MIN_SCALE, Math.min(MAX_SCALE, section.getDouble("appearance.scale", 1.0D))),
                section.getBoolean("appearance.collidable", true),
                section.getBoolean("appearance.show-in-tab", false),
                pose,
                section.getBoolean("baby", false),
                variant,
                variantName,
                registryName(section.getString("villager.profession")),
                registryName(section.getString("villager.type")),
                Math.max(1, Math.min(5, section.getInt("villager.level", 1))),
                NpcEquipment.read(section.getConfigurationSection("equipment"), id, warnings),
                blankToNull(section.getString("permission")),
                NpcActionSpec.parseAll(section.getMapList("actions"), id, warnings));
    }

    public static String fingerprint(ConfigurationSection section) {
        StringBuilder print = new StringBuilder();
        for (Map.Entry<String, Object> entry : section.getValues(true).entrySet()) {
            if (entry.getValue() instanceof ConfigurationSection || appliedLive(entry.getKey())) {
                continue;
            }
            print.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }
        return print.toString();
    }

    static boolean appliedLive(String key) {
        return key.equals(STYLE_KEY) || key.startsWith(STYLE_KEY + ".");
    }

    public boolean villager() {
        return villagerProfession != null || villagerType != null;
    }

    public long actionCount(NpcTrigger trigger) {
        return actions.stream().filter(action -> action.trigger() == trigger).count();
    }

    static EntityType entityType(String raw) {
        if (raw == null || raw.isBlank()) {
            return EntityType.PLAYER;
        }
        try {
            EntityType type = EntityType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            return type == EntityType.PLAYER || type.isAlive() ? type : null;
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    private static final java.util.Map<String, String> GLOW_ALIASES = java.util.Map.ofEntries(
            java.util.Map.entry("cyan", "dark_aqua"), java.util.Map.entry("light_blue", "aqua"),
            java.util.Map.entry("lime", "green"), java.util.Map.entry("orange", "gold"),
            java.util.Map.entry("pink", "light_purple"), java.util.Map.entry("magenta", "light_purple"),
            java.util.Map.entry("purple", "dark_purple"), java.util.Map.entry("brown", "gold"),
            java.util.Map.entry("grey", "gray"), java.util.Map.entry("light_gray", "gray"),
            java.util.Map.entry("light_grey", "gray"), java.util.Map.entry("silver", "gray"),
            java.util.Map.entry("dark_grey", "dark_gray"), java.util.Map.entry("violet", "light_purple"));

    private static NamedTextColor glowColor(String raw, String id, List<String> warnings) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String name = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        NamedTextColor color = NamedTextColor.NAMES.value(GLOW_ALIASES.getOrDefault(name, name));
        if (color == null && name.startsWith("#")) {
            net.kyori.adventure.text.format.TextColor hex = net.kyori.adventure.text.format.TextColor.fromHexString(name);
            color = hex == null ? null : NamedTextColor.nearestTo(hex);
        }
        if (color == null) {
            warnings.add(id + Tr.t(" : couleur de lueur inconnue \"") + raw + "\"");
        }
        return color;
    }

    private static NpcPose pose(String raw, String id, List<String> warnings) {
        if (raw == null || raw.isBlank()) {
            return NpcPose.STANDING;
        }
        try {
            return NpcPose.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            warnings.add(id + Tr.t(" : pose inconnue \"") + raw + Tr.t("\", debout par défaut"));
            return NpcPose.STANDING;
        }
    }

    private static String registryName(String value) {
        String trimmed = blankToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
