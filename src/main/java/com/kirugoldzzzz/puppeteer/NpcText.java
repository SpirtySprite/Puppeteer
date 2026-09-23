package com.kirugoldzzzz.puppeteer;

import com.kirugoldzzzz.puppeteer.common.text.Mini;
import com.kirugoldzzzz.puppeteer.common.util.LruCache;
import net.foliaboard.api.text.Legacy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

final class NpcText {

    private static final int CACHE_SIZE = 512;

    private final LruCache<String, String> nametags = new LruCache<>(CACHE_SIZE);
    private final boolean placeholderApi;

    NpcText(boolean placeholderApi) {
        this.placeholderApi = placeholderApi;
    }

    String resolve(Player player, String raw, String npcName) {
        if (raw == null) {
            return "";
        }
        String text = raw;
        if (player != null) {
            text = text.replace("%player%", player.getName());
        }
        if (npcName != null) {
            text = text.replace("%npc%", npcName);
        }
        if (placeholderApi && player != null && text.indexOf('%') >= 0) {
            text = NpcPlaceholders.apply(player, text);
        }
        return text;
    }

    Component chat(Player player, String raw, String npcName) {
        return Mini.parse(Legacy.toMini(resolve(player, raw, npcName)));
    }

    String nametag(Player player, String raw) {
        String resolved = Legacy.toMini(resolve(player, raw, null));
        String cached = nametags.get(resolved);
        if (cached != null) {
            return cached;
        }
        String serialized = MiniMessage.miniMessage().serialize(Mini.parse(resolved));
        nametags.put(resolved, serialized);
        return serialized;
    }

    void clear() {
        nametags.clear();
    }

    static String command(String resolved) {
        String trimmed = resolved.trim();
        return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
    }
}
