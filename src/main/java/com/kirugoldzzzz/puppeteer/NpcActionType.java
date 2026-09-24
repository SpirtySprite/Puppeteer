package com.kirugoldzzzz.puppeteer;

import java.util.Locale;

public enum NpcActionType {

    MESSAGE(true, false),
    BROADCAST(true, false),
    ACTIONBAR(true, false),
    TITLE(true, false),
    PLAYER_COMMAND(true, false),
    CONSOLE_COMMAND(true, false),
    SOUND(true, false),
    TELEPORT(true, false),
    SERVER(true, false),
    EFFECT(true, false),
    PARTICLE(true, false),
    GIVE_MONEY(false, true),
    TAKE_MONEY(false, true),
    REQUIRE_PERMISSION(true, false),
    SWING(false, false),
    DIALOGUE(true, false);

    private final boolean needsValue;
    private final boolean needsAmount;

    NpcActionType(boolean needsValue, boolean needsAmount) {
        this.needsValue = needsValue;
        this.needsAmount = needsAmount;
    }

    public boolean needsValue() {
        return needsValue;
    }

    public boolean needsAmount() {
        return needsAmount;
    }

    public boolean gate() {
        return this == TAKE_MONEY || this == REQUIRE_PERMISSION;
    }

    public static NpcActionType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String key = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (key) {
            case "COMMAND", "CMD" -> PLAYER_COMMAND;
            case "CONSOLE" -> CONSOLE_COMMAND;
            case "ACTION_BAR" -> ACTIONBAR;
            case "POTION" -> EFFECT;
            case "MONEY", "PAY" -> GIVE_MONEY;
            case "COST", "CHARGE" -> TAKE_MONEY;
            case "PERMISSION" -> REQUIRE_PERMISSION;
            case "CONNECT" -> SERVER;
            default -> {
                try {
                    yield valueOf(key);
                } catch (IllegalArgumentException unknown) {
                    yield null;
                }
            }
        };
    }
}
