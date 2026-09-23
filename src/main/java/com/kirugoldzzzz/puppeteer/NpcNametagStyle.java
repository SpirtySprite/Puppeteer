package com.kirugoldzzzz.puppeteer;

import com.kirugoldzzzz.puppeteer.common.text.Tr;

import net.folianpc.api.NametagStyle;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Locale;

public record NpcNametagStyle(int backgroundRgb, int backgroundOpacity, int textOpacity, boolean shadow,
                              boolean seeThrough) {

    public static final NpcNametagStyle VANILLA = new NpcNametagStyle(0x000000, 25, 100, false, false);

    public NpcNametagStyle {
        backgroundRgb &= 0xFFFFFF;
        backgroundOpacity = percent(backgroundOpacity);
        textOpacity = percent(textOpacity);
    }

    public static NpcNametagStyle read(ConfigurationSection section, NpcNametagStyle fallback, String context,
                                       List<String> warnings) {
        if (section == null) {
            return fallback;
        }
        int rgb = fallback.backgroundRgb();
        String background = section.getString("background");
        if (background != null && !background.isBlank()) {
            Integer parsed = color(background);
            if (parsed == null) {
                warnings.add(context + Tr.t(" : couleur de fond inconnue \"") + background
                        + Tr.t("\", attendu #RRGGBB ou un nom comme black"));
            } else {
                rgb = parsed;
            }
        }
        return new NpcNametagStyle(rgb,
                (int) Math.round(section.getDouble("background-opacity", fallback.backgroundOpacity())),
                (int) Math.round(section.getDouble("text-opacity", fallback.textOpacity())),
                section.getBoolean("shadow", fallback.shadow()),
                section.getBoolean("see-through", fallback.seeThrough()));
    }

    public NametagStyle toLibrary() {
        return NametagStyle.defaults()
                .withBackground(backgroundRgb, alpha(backgroundOpacity))
                .withTextOpacity(alpha(textOpacity))
                .withShadow(shadow)
                .withSeeThrough(seeThrough);
    }

    public String describe() {
        StringBuilder text = new StringBuilder(String.format(Locale.ROOT, Tr.t("fond #%06X à %d%%, texte à %d%%"),
                backgroundRgb, backgroundOpacity, textOpacity));
        if (shadow) {
            text.append(", ombre");
        }
        if (seeThrough) {
            text.append(Tr.t(", visible à travers les murs"));
        }
        return text.toString();
    }

    static int alpha(int percent) {
        return Math.round(percent(percent) * 255.0F / 100.0F);
    }

    static Integer color(String raw) {
        String value = raw.trim().toLowerCase(Locale.ROOT);
        NamedTextColor named = NamedTextColor.NAMES.value(value);
        if (named != null) {
            return named.value();
        }
        String hex = value.startsWith("0x") ? value.substring(2) : value.startsWith("#") ? value.substring(1) : value;
        TextColor parsed = hex.length() == 6 ? TextColor.fromHexString("#" + hex) : null;
        return parsed == null ? null : parsed.value();
    }

    private static int percent(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
