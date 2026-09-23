package com.kirugoldzzzz.puppeteer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record NpcActionSpec(NpcTrigger trigger, NpcSneak sneak, NpcActionType type, String value,
                            String subtitle, double amount, int duration, int fadeIn, int fadeOut,
                            float volume, float pitch, long delayTicks, String permission,
                            String denyMessage) {

    public static final long MAX_DELAY_TICKS = 72_000L;
    public static final int DEFAULT_TITLE_STAY = 60;
    public static final int DEFAULT_EFFECT_DURATION = 200;

    public static NpcActionSpec parse(Map<?, ?> raw, String context, List<String> warnings) {
        if (raw == null) {
            warnings.add(context + " : action vide");
            return null;
        }
        NpcActionType type = NpcActionType.parse(text(raw, "type"));
        if (type == null) {
            warnings.add(context + " : type d'action inconnu \"" + text(raw, "type") + "\"");
            return null;
        }
        NpcTrigger trigger = NpcTrigger.parse(text(raw, "click"));
        if (trigger == null) {
            warnings.add(context + " : clic inconnu \"" + text(raw, "click")
                    + "\", attendu LEFT, RIGHT ou BOTH");
            return null;
        }
        NpcSneak sneak = NpcSneak.parse(raw.get("sneak"));
        if (sneak == null) {
            warnings.add(context + " : accroupissement inconnu \"" + raw.get("sneak")
                    + "\", attendu ANY, SNEAKING ou STANDING");
            return null;
        }
        String value = joined(raw.get("value"));
        if (type.needsValue() && (value == null || value.isBlank())) {
            warnings.add(context + " : l'action " + type + " demande une valeur");
            return null;
        }
        double amount = Math.max(0.0D, number(raw, "amount", 0.0D));
        if (type.needsAmount() && amount <= 0.0D) {
            warnings.add(context + " : l'action " + type + " demande un montant positif");
            return null;
        }
        int defaultDuration = type == NpcActionType.TITLE ? DEFAULT_TITLE_STAY : DEFAULT_EFFECT_DURATION;
        return new NpcActionSpec(trigger, sneak, type, value, joined(raw.get("subtitle")), amount,
                (int) clamp(number(raw, "duration", defaultDuration), 1, 1_000_000),
                (int) clamp(number(raw, "fade-in", 10), 0, 200),
                (int) clamp(number(raw, "fade-out", 20), 0, 200),
                (float) clamp(number(raw, "volume", 1.0D), 0.0D, 10.0D),
                (float) clamp(number(raw, "pitch", 1.0D), 0.5D, 2.0D),
                (long) clamp(number(raw, "delay", 0), 0, MAX_DELAY_TICKS),
                blankToNull(text(raw, "permission")),
                blankToNull(joined(raw.get("deny-message"))));
    }

    public static List<NpcActionSpec> parseAll(List<Map<?, ?>> raw, String id, List<String> warnings) {
        List<NpcActionSpec> actions = new ArrayList<>();
        if (raw == null) {
            return actions;
        }
        for (int index = 0; index < raw.size(); index++) {
            NpcActionSpec spec = parse(raw.get(index), id + " action " + (index + 1), warnings);
            if (spec != null) {
                actions.add(spec);
            }
        }
        return List.copyOf(actions);
    }

    private static String text(Map<?, ?> raw, String key) {
        Object value = raw.get(key);
        return value == null ? null : value.toString();
    }

    private static String joined(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List<?> lines) {
            List<String> parts = new ArrayList<>(lines.size());
            for (Object line : lines) {
                parts.add(line == null ? "" : line.toString());
            }
            return String.join("\n", parts);
        }
        return value.toString();
    }

    private static double number(Map<?, ?> raw, String key, double fallback) {
        Object value = raw.get(key);
        if (value instanceof Number numeric) {
            return numeric.doubleValue();
        }
        if (value != null) {
            try {
                return Double.parseDouble(value.toString().trim());
            } catch (NumberFormatException invalid) {
                return fallback;
            }
        }
        return fallback;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
