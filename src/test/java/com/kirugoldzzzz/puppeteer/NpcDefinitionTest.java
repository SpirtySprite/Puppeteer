package com.kirugoldzzzz.puppeteer;

import net.folianpc.api.NpcPose;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcDefinitionTest {

    private static ConfigurationSection npc(String yaml) {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(yaml);
        } catch (Exception invalid) {
            throw new IllegalStateException(invalid);
        }
        return configuration;
    }

    @Test
    @DisplayName("Seuls les identifiants en minuscules, chiffres, _ et - sont valides")
    void identifiers() {
        assertTrue(NpcDefinition.validId("guide_2-b"));
        assertFalse(NpcDefinition.validId("Guide"));
        assertFalse(NpcDefinition.validId("guide spawn"));
        assertFalse(NpcDefinition.validId(""));
        assertFalse(NpcDefinition.validId("a".repeat(33)));
        assertFalse(NpcDefinition.validId(null));
    }

    @Test
    @DisplayName("Un PNJ minimal reçoit des valeurs par défaut sensées")
    void minimalDefaults() {
        List<String> warnings = new ArrayList<>();
        NpcDefinition definition = NpcDefinition.parse("guide",
                npc("location:\n  world: world\n  x: 1.5\n  y: 64\n  z: -2.5\n"), new NpcDefaults(750L, NpcNametagStyle.VANILLA), warnings);
        assertNotNull(definition);
        assertTrue(warnings.isEmpty());
        assertTrue(definition.enabled());
        assertEquals("guide", definition.name());
        assertEquals(EntityType.PLAYER, definition.type());
        assertEquals(1.5D, definition.x());
        assertEquals(-2.5D, definition.z());
        assertEquals(750L, definition.cooldownMillis());
        assertTrue(definition.lookAtPlayers());
        assertTrue(definition.skinLayers());
        assertTrue(definition.collidable());
        assertFalse(definition.showInTab());
        assertEquals(1.0D, definition.scale());
        assertEquals(NpcPose.STANDING, definition.pose());
        assertNull(definition.nametagVisible());
        assertNull(definition.permission());
        assertEquals(NpcSkinSource.Kind.NONE, definition.skin().kind());
        assertTrue(definition.actions().isEmpty());
        assertFalse(definition.villager());
    }

    @Test
    @DisplayName("Sans monde ou avec un type non vivant, le PNJ est ignoré")
    void invalidDefinitionsAreSkipped() {
        List<String> warnings = new ArrayList<>();
        assertNull(NpcDefinition.parse("sans_monde", npc("name: Test\n"), NpcDefaults.standard(), warnings));
        assertNull(NpcDefinition.parse("fleche", npc("type: ARROW\nlocation:\n  world: world\n"), NpcDefaults.standard(), warnings));
        assertNull(NpcDefinition.parse("Majuscule", npc("location:\n  world: world\n"), NpcDefaults.standard(), warnings));
        assertEquals(3, warnings.size());
    }

    @Test
    @DisplayName("Toutes les options d'apparence sont lues et bornées")
    void appearanceOptions() {
        List<String> warnings = new ArrayList<>();
        NpcDefinition definition = NpcDefinition.parse("chat", npc("""
                type: cat
                location:
                  world: world
                variant: BLACK
                baby: true
                pose: crouching
                view-distance: 24
                cooldown-ms: 2000
                nametag-refresh-ticks: 40
                permission: puppeteer.vip
                appearance:
                  glowing: true
                  glow-color: light_purple
                  invisible: true
                  scale: 99
                  collidable: false
                  nametag-visible: false
                villager:
                  profession: LIBRARIAN
                  level: 9
                """), NpcDefaults.standard(), warnings);
        assertNotNull(definition);
        assertTrue(warnings.isEmpty(), warnings.toString());
        assertEquals(EntityType.CAT, definition.type());
        assertEquals("black", definition.variantName());
        assertNull(definition.variant());
        assertTrue(definition.baby());
        assertEquals(NpcPose.CROUCHING, definition.pose());
        assertEquals(24.0D, definition.viewDistance());
        assertEquals(2000L, definition.cooldownMillis());
        assertEquals(40L, definition.nametagRefreshTicks());
        assertEquals("puppeteer.vip", definition.permission());
        assertTrue(definition.glowing());
        assertEquals(NamedTextColor.LIGHT_PURPLE, definition.glowColor());
        assertTrue(definition.invisible());
        assertEquals(NpcDefinition.MAX_SCALE, definition.scale());
        assertFalse(definition.collidable());
        assertEquals(Boolean.FALSE, definition.nametagVisible());
        assertEquals("librarian", definition.villagerProfession());
        assertEquals(5, definition.villagerLevel());
        assertTrue(definition.villager());
    }

    @Test
    void commonColourNamesAndHexCodesAreAcceptedForTheGlow() {
        Map<String, NamedTextColor> expected = Map.of(
                "cyan", NamedTextColor.DARK_AQUA,
                "light_blue", NamedTextColor.AQUA,
                "Light Blue", NamedTextColor.AQUA,
                "pink", NamedTextColor.LIGHT_PURPLE,
                "grey", NamedTextColor.GRAY,
                "\"#FF5555\"", NamedTextColor.RED);
        expected.forEach((raw, colour) -> {
            List<String> warnings = new ArrayList<>();
            NpcDefinition definition = NpcDefinition.parse("guide", npc("""
                    location:
                      world: world
                    appearance:
                      glow-color: %s
                    """.formatted(raw)), NpcDefaults.standard(), warnings);
            assertNotNull(definition, raw);
            assertEquals(colour, definition.glowColor(), raw);
            assertTrue(warnings.isEmpty(), raw + " " + warnings);
        });
    }

    @Test
    @DisplayName("Une couleur ou une pose inconnue avertit sans bloquer le PNJ")
    void softWarnings() {
        List<String> warnings = new ArrayList<>();
        NpcDefinition definition = NpcDefinition.parse("guide", npc("""
                location:
                  world: world
                pose: flying
                variant: 3
                appearance:
                  glow-color: rainbow
                """), NpcDefaults.standard(), warnings);
        assertNotNull(definition);
        assertEquals(2, warnings.size());
        assertEquals(NpcPose.STANDING, definition.pose());
        assertNull(definition.glowColor());
        assertEquals(3, definition.variant());
        assertNull(definition.variantName());
    }

    @Test
    @DisplayName("Le skin se lit en texte simple ou en section, la texture signée passe en premier")
    void skinSources() {
        assertEquals(NpcSkinSource.Kind.PLAYER, NpcSkinSource.read("Notch").kind());
        assertEquals(NpcSkinSource.Kind.URL, NpcSkinSource.read("https://example.org/skin.png").kind());
        assertTrue(NpcSkinSource.read("mirror").mirror());
        assertEquals(NpcSkinSource.Kind.NONE, NpcSkinSource.read(" ").kind());

        NpcSkinSource section = NpcSkinSource.read(npc("""
                value: abc
                signature: def
                url: https://example.org/skin.png
                player: Notch
                mirror: true
                """));
        assertEquals(NpcSkinSource.Kind.TEXTURE, section.kind());
        assertEquals("abc", section.value());
        assertEquals("def", section.signature());
        assertTrue(section.mirror());
        assertEquals(NpcSkinSource.Kind.URL, NpcSkinSource.read(npc("url: https://x.org/a.png\nplayer: Notch\n")).kind());
        assertEquals(NpcSkinSource.Kind.PLAYER, NpcSkinSource.read(npc("player: Notch\n")).kind());
    }

    @Test
    @DisplayName("L'équipement accepte les alias d'emplacement et les objets détaillés")
    void equipmentSlots() {
        List<String> warnings = new ArrayList<>();
        Map<EquipmentSlot, NpcEquipment> equipment = NpcEquipment.read(npc("""
                main-hand: DIAMOND_SWORD
                casque:
                  material: GOLDEN_HELMET
                  glow: true
                bottes: IRON_BOOTS
                queue: STICK
                """), "guide", warnings);
        assertEquals(3, equipment.size());
        assertEquals("DIAMOND_SWORD", equipment.get(EquipmentSlot.HAND).material());
        assertNotNull(equipment.get(EquipmentSlot.HEAD).item());
        assertEquals("IRON_BOOTS", equipment.get(EquipmentSlot.FEET).material());
        assertEquals(1, warnings.size());
        assertEquals(EquipmentSlot.OFF_HAND, NpcEquipment.slot("off-hand"));
        assertEquals(EquipmentSlot.CHEST, NpcEquipment.slot("plastron"));
    }

    @Test
    @DisplayName("Le rechargement ne touche que les PNJ ajoutés, modifiés ou retirés")
    void reloadPlan() {
        NpcReloadPlan plan = NpcReloadPlan.between(
                Map.of("garde", "a", "guide", "b", "ancien", "c"),
                Map.of("garde", "a", "guide", "b2", "nouveau", "d"));
        assertEquals(List.of("nouveau"), plan.spawn());
        assertEquals(List.of("guide"), plan.respawn());
        assertEquals(List.of("ancien"), plan.remove());
        assertEquals(List.of("garde"), plan.keep());
        assertFalse(plan.idle());
        assertTrue(NpcReloadPlan.between(Map.of("garde", "a"), Map.of("garde", "a")).idle());
    }

    @Test
    @DisplayName("L'empreinte est stable et change dès qu'une option bouge")
    void fingerprint() {
        String yaml = "location:\n  world: world\n  x: 1\nactions:\n- type: SWING\n";
        String first = NpcDefinition.fingerprint(npc(yaml));
        assertEquals(first, NpcDefinition.fingerprint(npc(yaml)));
        assertNotEquals(first, NpcDefinition.fingerprint(npc(yaml.replace("x: 1", "x: 2"))));
        assertNotEquals(first, NpcDefinition.fingerprint(npc(yaml.replace("SWING", "MESSAGE"))));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"npcs.yml", "lang/npcs_fr.yml"})
    @DisplayName("Le npcs.yml livré se charge sans aucun avertissement")
    void shippedConfigurationIsClean(String file) throws IOException {
        YamlConfiguration configuration;
        try (Reader reader = Files.newBufferedReader(Path.of("src", "main", "resources").resolve(file),
                StandardCharsets.UTF_8)) {
            configuration = YamlConfiguration.loadConfiguration(reader);
        }
        ConfigurationSection npcs = configuration.getConfigurationSection("npcs");
        assertNotNull(npcs);
        List<String> warnings = new ArrayList<>();
        int actions = 0;
        for (String id : npcs.getKeys(false)) {
            NpcDefinition definition = NpcDefinition.parse(id, npcs.getConfigurationSection(id), NpcDefaults.standard(), warnings);
            assertNotNull(definition, id);
            assertFalse(definition.enabled(), id + " doit être livré désactivé");
            actions += definition.actions().size();
        }
        assertTrue(warnings.isEmpty(), warnings.toString());
        assertEquals(4, npcs.getKeys(false).size());
        assertTrue(actions >= 20, "attendu une vitrine complète, obtenu " + actions);
        Set<NpcActionType> used = new TreeSet<>();
        for (String id : npcs.getKeys(false)) {
            NpcDefinition.parse(id, npcs.getConfigurationSection(id), NpcDefaults.standard(), warnings)
                    .actions().forEach(action -> used.add(action.type()));
        }
        assertEquals(Set.of(NpcActionType.values()), used);
    }

    @Test
    @DisplayName("Chaque clé de message utilisée par les PNJ existe dans messages.yml")
    void messageKeysExist() throws IOException {
        YamlConfiguration messages;
        try (Reader reader = Files.newBufferedReader(Path.of("src", "main", "resources", "lang", "messages_fr.yml"),
                StandardCharsets.UTF_8)) {
            messages = YamlConfiguration.loadConfiguration(reader);
        }
        Pattern literal = Pattern.compile("\"(npc\\.[a-z-]+)\"");
        Set<String> keys = new TreeSet<>();
        try (Stream<Path> files = Files.list(Path.of("src", "main", "java", "com", "kirugoldzzzz", "puppeteer"))) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                Matcher matcher = literal.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (matcher.find()) {
                    keys.add(matcher.group(1));
                }
            }
        }
        assertTrue(keys.size() >= 20, keys.toString());
        for (String key : keys) {
            assertTrue(messages.contains(key), key);
        }
        for (NpcService.Status status : NpcService.Status.values()) {
            assertTrue(messages.contains("npc.status." + status.name().toLowerCase(java.util.Locale.ROOT)), status.name());
        }
    }

    @Test
    @DisplayName("Sans style, le nom garde l'apparence vanilla")
    void nametagStyleDefaultsToVanilla() {
        NpcDefinition definition = NpcDefinition.parse("guide", npc("location:\n  world: world\n"),
                NpcDefaults.standard(), new ArrayList<>());
        assertNotNull(definition);
        assertEquals(NpcNametagStyle.VANILLA, definition.nametagStyle());
        assertEquals(net.folianpc.api.NametagStyle.defaults(), NpcNametagStyle.VANILLA.toLibrary());
    }

    @Test
    @DisplayName("Le style du nom hérite des réglages globaux, clé par clé")
    void nametagStyleInheritsGlobalSettings() {
        List<String> warnings = new ArrayList<>();
        NpcDefaults defaults = NpcDefaults.read(npc("""
                default-cooldown-ms: 900
                nametag-style:
                  background: "#112233"
                  background-opacity: 0
                  shadow: true
                """), warnings);
        NpcDefinition definition = NpcDefinition.parse("guide", npc("""
                location:
                  world: world
                nametag-style:
                  text-opacity: 50
                  see-through: true
                """), defaults, warnings);
        assertNotNull(definition);
        assertTrue(warnings.isEmpty(), warnings.toString());
        assertEquals(900L, definition.cooldownMillis());
        assertEquals(new NpcNametagStyle(0x112233, 0, 50, true, true), definition.nametagStyle());
        net.folianpc.api.NametagStyle library = definition.nametagStyle().toLibrary();
        assertEquals(0x00112233, library.background());
        assertEquals(128, library.textOpacity());
        assertTrue(library.shadow());
        assertTrue(library.seeThrough());
    }

    @Test
    @DisplayName("Les couleurs de fond acceptent hex et noms, les opacités sont bornées")
    void nametagStyleColoursAndClamps() {
        assertEquals(0x2B1A00, NpcNametagStyle.color("#2b1a00"));
        assertEquals(0x2B1A00, NpcNametagStyle.color("2B1A00"));
        assertEquals(0x2B1A00, NpcNametagStyle.color("0x2B1A00"));
        assertEquals(0xAA00AA, NpcNametagStyle.color("Dark_Purple"));
        assertNull(NpcNametagStyle.color("#12345"));
        assertNull(NpcNametagStyle.color("arc-en-ciel"));
        assertEquals(64, NpcNametagStyle.alpha(25));
        assertEquals(255, NpcNametagStyle.alpha(100));
        assertEquals(0, NpcNametagStyle.alpha(-10));
        assertEquals(255, NpcNametagStyle.alpha(250));

        List<String> warnings = new ArrayList<>();
        NpcNametagStyle style = NpcNametagStyle.read(npc("""
                background: arc-en-ciel
                background-opacity: 180
                text-opacity: -4
                """), NpcNametagStyle.VANILLA, "guide nametag-style", warnings);
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).startsWith("guide nametag-style"));
        assertEquals(new NpcNametagStyle(0, 100, 0, false, false), style);
        assertEquals("fond #000000 à 25%, texte à 100%", NpcNametagStyle.VANILLA.describe());
        assertEquals("fond #112233 à 0%, texte à 50%, ombre, visible à travers les murs",
                new NpcNametagStyle(0x112233, 0, 50, true, true).describe());
    }

    @Test
    @DisplayName("Changer le style du nom ne force pas la réapparition du PNJ")
    void nametagStyleIsLeftOutOfTheFingerprint() {
        String yaml = "location:\n  world: world\nnametag-style:\n  background-opacity: 10\n";
        String first = NpcDefinition.fingerprint(npc(yaml));
        assertEquals(first, NpcDefinition.fingerprint(npc(yaml.replace("10", "90"))));
        assertEquals(first, NpcDefinition.fingerprint(npc("location:\n  world: world\n")));
        assertTrue(NpcDefinition.appliedLive("nametag-style"));
        assertTrue(NpcDefinition.appliedLive("nametag-style.shadow"));
        assertFalse(NpcDefinition.appliedLive("nametag-styles"));
        assertFalse(NpcDefinition.appliedLive("nametag"));
    }
}
