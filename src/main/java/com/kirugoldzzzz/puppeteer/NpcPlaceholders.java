package com.kirugoldzzzz.puppeteer;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

final class NpcPlaceholders {

    private NpcPlaceholders() {
    }

    static String apply(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
