package com.kirugoldzzzz.puppeteer;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public record NpcDefaults(long cooldownMillis, NpcNametagStyle nametagStyle) {

    public static final long DEFAULT_COOLDOWN = 500L;

    public static NpcDefaults standard() {
        return new NpcDefaults(DEFAULT_COOLDOWN, NpcNametagStyle.VANILLA);
    }

    public static NpcDefaults read(ConfigurationSection settings, List<String> warnings) {
        if (settings == null) {
            return standard();
        }
        return new NpcDefaults(Math.max(0L, settings.getLong("default-cooldown-ms", DEFAULT_COOLDOWN)),
                NpcNametagStyle.read(settings.getConfigurationSection("nametag-style"), NpcNametagStyle.VANILLA,
                        "settings.nametag-style", warnings));
    }
}
