package com.kirugoldzzzz.puppeteer;

import java.util.Locale;

public enum NpcSneak {

    ANY,
    SNEAKING,
    STANDING;

    public boolean accepts(boolean sneaking) {
        return switch (this) {
            case ANY -> true;
            case SNEAKING -> sneaking;
            case STANDING -> !sneaking;
        };
    }

    public static NpcSneak parse(Object raw) {
        if (raw == null) {
            return ANY;
        }
        if (raw instanceof Boolean flag) {
            return flag ? SNEAKING : STANDING;
        }
        String value = raw.toString().trim();
        if (value.isEmpty()) {
            return ANY;
        }
        return switch (value.toUpperCase(Locale.ROOT).replace('-', '_')) {
            case "ANY", "TOUS", "IGNORE" -> ANY;
            case "SNEAKING", "SNEAK", "SHIFT", "ACCROUPI", "TRUE" -> SNEAKING;
            case "STANDING", "STAND", "NO_SHIFT", "DEBOUT", "FALSE" -> STANDING;
            default -> null;
        };
    }
}
