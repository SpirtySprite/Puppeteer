package com.kirugoldzzzz.puppeteer;

import net.folianpc.api.ClickType;

import java.util.List;
import java.util.Locale;

public enum NpcTrigger {

    LEFT(List.of(ClickType.LEFT)),
    RIGHT(List.of(ClickType.RIGHT)),
    BOTH(List.of(ClickType.LEFT, ClickType.RIGHT));

    private final List<ClickType> clicks;

    NpcTrigger(List<ClickType> clicks) {
        this.clicks = clicks;
    }

    public List<ClickType> clicks() {
        return clicks;
    }

    public static NpcTrigger parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return RIGHT;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_')) {
            case "LEFT", "GAUCHE", "LEFT_CLICK", "CLIC_GAUCHE" -> LEFT;
            case "RIGHT", "DROIT", "RIGHT_CLICK", "CLIC_DROIT" -> RIGHT;
            case "BOTH", "ANY", "ALL", "LES_DEUX", "TOUS" -> BOTH;
            default -> null;
        };
    }
}
