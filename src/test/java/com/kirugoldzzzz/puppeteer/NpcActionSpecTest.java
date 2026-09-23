package com.kirugoldzzzz.puppeteer;

import net.folianpc.api.ClickType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcActionSpecTest {

    private static Map<String, Object> action(Object... pairs) {
        Map<String, Object> raw = new HashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            raw.put((String) pairs[index], pairs[index + 1]);
        }
        return raw;
    }

    @Test
    @DisplayName("Le clic vaut droit par défaut et accepte les alias français et anglais")
    void triggerAliases() {
        assertEquals(NpcTrigger.RIGHT, NpcTrigger.parse(null));
        assertEquals(NpcTrigger.RIGHT, NpcTrigger.parse(" "));
        assertEquals(NpcTrigger.LEFT, NpcTrigger.parse("gauche"));
        assertEquals(NpcTrigger.LEFT, NpcTrigger.parse("left_click"));
        assertEquals(NpcTrigger.RIGHT, NpcTrigger.parse("clic-droit"));
        assertEquals(NpcTrigger.BOTH, NpcTrigger.parse("les deux"));
        assertEquals(NpcTrigger.BOTH, NpcTrigger.parse("any"));
        assertNull(NpcTrigger.parse("milieu"));
        assertEquals(List.of(ClickType.LEFT, ClickType.RIGHT), NpcTrigger.BOTH.clicks());
    }

    @Test
    @DisplayName("L'accroupissement filtre selon l'état du joueur")
    void sneakFilter() {
        assertEquals(NpcSneak.ANY, NpcSneak.parse(null));
        assertEquals(NpcSneak.SNEAKING, NpcSneak.parse(true));
        assertEquals(NpcSneak.STANDING, NpcSneak.parse(false));
        assertEquals(NpcSneak.SNEAKING, NpcSneak.parse("accroupi"));
        assertEquals(NpcSneak.STANDING, NpcSneak.parse("debout"));
        assertNull(NpcSneak.parse("allonge"));
        assertTrue(NpcSneak.ANY.accepts(true) && NpcSneak.ANY.accepts(false));
        assertTrue(NpcSneak.SNEAKING.accepts(true));
        assertFalse(NpcSneak.SNEAKING.accepts(false));
        assertTrue(NpcSneak.STANDING.accepts(false));
        assertFalse(NpcSneak.STANDING.accepts(true));
    }

    @Test
    @DisplayName("Les types d'action acceptent leurs alias")
    void typeAliases() {
        assertEquals(NpcActionType.PLAYER_COMMAND, NpcActionType.parse("cmd"));
        assertEquals(NpcActionType.CONSOLE_COMMAND, NpcActionType.parse("console"));
        assertEquals(NpcActionType.TAKE_MONEY, NpcActionType.parse("cost"));
        assertEquals(NpcActionType.GIVE_MONEY, NpcActionType.parse("pay"));
        assertEquals(NpcActionType.EFFECT, NpcActionType.parse("potion"));
        assertEquals(NpcActionType.ACTIONBAR, NpcActionType.parse("action-bar"));
        assertEquals(NpcActionType.SERVER, NpcActionType.parse("connect"));
        assertNull(NpcActionType.parse("explode"));
        assertTrue(NpcActionType.TAKE_MONEY.gate());
        assertTrue(NpcActionType.REQUIRE_PERMISSION.gate());
        assertFalse(NpcActionType.MESSAGE.gate());
    }

    @Test
    @DisplayName("Une action complète garde toutes ses options")
    void fullActionIsRead() {
        List<String> warnings = new ArrayList<>();
        NpcActionSpec spec = NpcActionSpec.parse(action("click", "BOTH", "sneak", true, "type", "TITLE",
                "value", "Titre", "subtitle", "Sous-titre", "duration", 80, "fade-in", 5, "fade-out", 15,
                "delay", 40, "permission", "puppeteer.vip", "deny-message", "Refusé"), "test", warnings);
        assertNotNull(spec);
        assertTrue(warnings.isEmpty());
        assertEquals(NpcTrigger.BOTH, spec.trigger());
        assertEquals(NpcSneak.SNEAKING, spec.sneak());
        assertEquals("Titre", spec.value());
        assertEquals("Sous-titre", spec.subtitle());
        assertEquals(80, spec.duration());
        assertEquals(5, spec.fadeIn());
        assertEquals(15, spec.fadeOut());
        assertEquals(40L, spec.delayTicks());
        assertEquals("puppeteer.vip", spec.permission());
        assertEquals("Refusé", spec.denyMessage());
    }

    @Test
    @DisplayName("Les valeurs hors limites sont ramenées dans l'intervalle permis")
    void valuesAreClamped() {
        NpcActionSpec spec = NpcActionSpec.parse(action("type", "SOUND", "value", "entity.cat.purr",
                "volume", 99, "pitch", 0.01, "delay", 9_999_999), "test", new ArrayList<>());
        assertNotNull(spec);
        assertEquals(10.0F, spec.volume());
        assertEquals(0.5F, spec.pitch());
        assertEquals(NpcActionSpec.MAX_DELAY_TICKS, spec.delayTicks());
        assertEquals(NpcTrigger.RIGHT, spec.trigger());
        assertEquals(NpcSneak.ANY, spec.sneak());
    }

    @Test
    @DisplayName("Une liste de lignes devient un message sur plusieurs lignes")
    void listValueIsJoined() {
        NpcActionSpec spec = NpcActionSpec.parse(action("type", "MESSAGE", "value", List.of("un", "deux")),
                "test", new ArrayList<>());
        assertNotNull(spec);
        assertEquals("un\ndeux", spec.value());
    }

    @Test
    @DisplayName("Les actions incomplètes sont refusées avec un avertissement clair")
    void incompleteActionsAreRejected() {
        List<String> warnings = new ArrayList<>();
        assertNull(NpcActionSpec.parse(action("value", "x"), "pnj action 1", warnings));
        assertNull(NpcActionSpec.parse(action("type", "MESSAGE"), "pnj action 2", warnings));
        assertNull(NpcActionSpec.parse(action("type", "TAKE_MONEY", "amount", 0), "pnj action 3", warnings));
        assertNull(NpcActionSpec.parse(action("type", "MESSAGE", "value", "x", "click", "milieu"),
                "pnj action 4", warnings));
        assertNull(NpcActionSpec.parse(action("type", "MESSAGE", "value", "x", "sneak", "allonge"),
                "pnj action 5", warnings));
        assertEquals(5, warnings.size());
        assertTrue(warnings.get(0).startsWith("pnj action 1"));
        assertTrue(warnings.get(2).contains("montant"));
    }

    @Test
    @DisplayName("Le balancement et les gains n'exigent pas de valeur")
    void valuelessActions() {
        List<String> warnings = new ArrayList<>();
        assertNotNull(NpcActionSpec.parse(action("type", "SWING"), "test", warnings));
        NpcActionSpec money = NpcActionSpec.parse(action("type", "GIVE_MONEY", "amount", "12.5"), "test", warnings);
        assertNotNull(money);
        assertEquals(12.5D, money.amount());
        assertTrue(warnings.isEmpty());
    }

    @Test
    @DisplayName("parseAll garde les actions valides et numérote les erreurs")
    void parseAllKeepsValidActions() {
        List<String> warnings = new ArrayList<>();
        List<Map<?, ?>> raw = List.of(action("type", "SWING"), action("type", "NOPE"),
                action("type", "MESSAGE", "value", "ok"));
        List<NpcActionSpec> parsed = NpcActionSpec.parseAll(raw, "guide", warnings);
        assertEquals(2, parsed.size());
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).startsWith("guide action 2"));
    }

    @Test
    @DisplayName("Les sons acceptent une clé du jeu ou un nom d'énumération")
    void soundKeys() {
        assertEquals("entity.player.levelup", NpcActionCompiler.soundKey("Entity.Player.LevelUp"));
        assertEquals("nexus:ui.click", NpcActionCompiler.soundKey("nexus:ui.click"));
        assertNull(NpcActionCompiler.soundKey(" "));
        assertNull(NpcActionCompiler.soundKey(null));
    }
}
