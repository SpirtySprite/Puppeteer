package com.kirugoldzzzz.puppeteer;

import com.kirugoldzzzz.puppeteer.common.text.Tr;
import org.bukkit.configuration.ConfigurationSection;

public record NpcSkinSource(Kind kind, String value, String signature, boolean mirror) {

    public enum Kind {
        NONE,
        PLAYER,
        URL,
        TEXTURE
    }

    private static final NpcSkinSource NONE = new NpcSkinSource(Kind.NONE, null, null, false);

    public static NpcSkinSource none() {
        return NONE;
    }

    public static NpcSkinSource read(Object raw) {
        if (raw == null) {
            return NONE;
        }
        if (!(raw instanceof ConfigurationSection section)) {
            String value = raw.toString().trim();
            if (value.isEmpty()) {
                return NONE;
            }
            if (value.equalsIgnoreCase("mirror")) {
                return new NpcSkinSource(Kind.NONE, null, null, true);
            }
            return value.startsWith("http://") || value.startsWith("https://")
                    ? new NpcSkinSource(Kind.URL, value, null, false)
                    : new NpcSkinSource(Kind.PLAYER, value, null, false);
        }
        boolean mirror = section.getBoolean("mirror", false);
        String texture = trimmed(section.getString("value"));
        if (texture != null) {
            return new NpcSkinSource(Kind.TEXTURE, texture, trimmed(section.getString("signature")), mirror);
        }
        String url = trimmed(section.getString("url"));
        if (url != null) {
            return new NpcSkinSource(Kind.URL, url, null, mirror);
        }
        String player = trimmed(section.getString("player"));
        if (player != null) {
            return new NpcSkinSource(Kind.PLAYER, player, null, mirror);
        }
        return new NpcSkinSource(Kind.NONE, null, null, mirror);
    }

    public String describe() {
        if (mirror) {
            return Tr.t("reflet du joueur");
        }
        return switch (kind) {
            case NONE -> Tr.t("par défaut");
            case PLAYER -> Tr.t("joueur ") + value;
            case URL -> Tr.t("image distante");
            case TEXTURE -> Tr.t("texture signée");
        };
    }

    private static String trimmed(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
