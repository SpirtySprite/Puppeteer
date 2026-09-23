package com.kirugoldzzzz.puppeteer.importer;

import com.kirugoldzzzz.puppeteer.NpcActionType;
import com.kirugoldzzzz.puppeteer.NpcDefaults;
import com.kirugoldzzzz.puppeteer.NpcDefinition;
import com.kirugoldzzzz.puppeteer.NpcSkinSource;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcSourceTest {

    @TempDir
    Path folder;

    private void write(String path, String... lines) throws IOException {
        Path target = folder.resolve(path);
        Files.createDirectories(target.getParent());
        Files.writeString(target, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    }

    private static NpcDefinition parse(NpcSource.Npc npc) {
        YamlConfiguration root = new YamlConfiguration();
        root.createSection("npcs." + npc.id(), npc.section());
        List<String> warnings = new ArrayList<>();
        NpcDefinition definition = NpcDefinition.parse(npc.id(), root.getConfigurationSection("npcs." + npc.id()),
                NpcDefaults.standard(), warnings);
        assertTrue(warnings.isEmpty(), warnings.toString());
        assertNotNull(definition);
        return definition;
    }

    @Test
    void citizensNpcWithSkinLookAndCommands() throws IOException {
        write("saves.yml",
                "npc:",
                "  '3':",
                "    name: '&6Shop Keeper'",
                "    traits:",
                "      type: VILLAGER",
                "      location:",
                "        world: world",
                "        x: 10.5",
                "        y: 64.0",
                "        z: -3.5",
                "        yaw: 90.0",
                "        pitch: 0.0",
                "      lookclose:",
                "        enabled: true",
                "      skintrait:",
                "        skinName: Notch",
                "      commandtrait:",
                "        commands:",
                "          '0':",
                "            command: 'shop open <p>'",
                "            hand: RIGHT",
                "            player: false",
                "            delay: 5",
                "          '1':",
                "            command: 'spawn'",
                "            hand: SHIFT_LEFT",
                "            player: true",
                "            op: true",
                "  '4':",
                "    name: Ghost");

        NpcSource.Result result = new CitizensSource().read(folder.toFile());

        assertEquals(1, result.npcs().size());
        NpcSource.Npc npc = result.npcs().getFirst();
        assertEquals("shop_keeper_3", npc.id());
        NpcDefinition definition = parse(npc);
        assertEquals(EntityType.VILLAGER, definition.type());
        assertEquals("Shop Keeper", definition.name());
        assertTrue(definition.lookAtPlayers());
        assertEquals(NpcSkinSource.Kind.PLAYER, definition.skin().kind());
        assertEquals(2, definition.actions().size());
        assertEquals(NpcActionType.CONSOLE_COMMAND, definition.actions().get(0).type());
        assertEquals(NpcActionType.PLAYER_COMMAND, definition.actions().get(1).type());
        assertEquals(2, result.warnings().size());
    }

    @Test
    void fancyNpcsWithTextureActionsAndWaits() throws IOException {
        write("npcs.yml",
                "npcs:",
                "  6f1a:",
                "    name: guide",
                "    displayName: '<gold>Guide'",
                "    type: PLAYER",
                "    location:",
                "      world: world",
                "      x: 0.5",
                "      y: 70.0",
                "      z: 0.5",
                "      yaw: 0.0",
                "      pitch: 0.0",
                "    glowing: true",
                "    glowingColor: aqua",
                "    turnToPlayer: true",
                "    interactionCooldown: 1.5",
                "    skin:",
                "      value: abc",
                "      signature: def",
                "    actions:",
                "      RIGHT_CLICK:",
                "        '1':",
                "          action: message",
                "          value: '<green>Hello {player}'",
                "        '2':",
                "          action: wait",
                "          value: '2'",
                "        '3':",
                "          action: player_command",
                "          value: 'warp spawn'",
                "        '4':",
                "          action: execute_random_action",
                "          value: ''");

        NpcSource.Result result = new FancyNpcsSource().read(folder.toFile());
        NpcSource.Npc npc = result.npcs().getFirst();
        NpcDefinition definition = parse(npc);

        assertEquals("guide", npc.id());
        assertEquals(NpcSkinSource.Kind.TEXTURE, definition.skin().kind());
        assertTrue(definition.glowing());
        assertEquals(1500L, definition.cooldownMillis());
        assertEquals(2, definition.actions().size());
        assertEquals(40L, definition.actions().get(1).delayTicks());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void znpcsPlusWithPrefetchedSkinAndEncodedActions() throws IOException {
        String command = Base64.getEncoder().encodeToString("say hi {player}".getBytes(StandardCharsets.UTF_8));
        write("data/Merchant.yml",
                "id: Merchant",
                "enabled: true",
                "world: world",
                "type: player",
                "location:",
                "  x: 1.0",
                "  y: 65.0",
                "  z: 1.0",
                "  yaw: 0.0",
                "  pitch: 0.0",
                "properties:",
                "  skin: 'prefetched;textures;VALUE;SIGNATURE;'",
                "  look: 'true'",
                "hologram:",
                "  lines:",
                "    - '<yellow>Merchant'",
                "actions:",
                "  - 'lol.pyr.znpcsplus.interaction.consolecommand.ConsoleCommandAction;" + command + ";0;RIGHT_CLICK;10'",
                "  - 'lol.pyr.znpcsplus.interaction.playerchat.PlayerChatAction;eA==;0;ANY_CLICK;0'");

        NpcSource.Result result = new ZnpcsPlusSource().read(folder.toFile());
        NpcSource.Npc npc = result.npcs().getFirst();
        NpcDefinition definition = parse(npc);

        assertEquals("merchant", npc.id());
        assertEquals(NpcSkinSource.Kind.TEXTURE, definition.skin().kind());
        assertEquals("VALUE", definition.skin().value());
        assertTrue(definition.lookAtPlayers());
        assertEquals(1, definition.actions().size());
        assertEquals("say hi %player%", definition.actions().getFirst().value());
        assertEquals(10L, definition.actions().getFirst().delayTicks());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void emptyFoldersAreReported() {
        for (NpcSource source : NpcSource.ALL) {
            NpcSource.Result result = source.read(folder.toFile());
            assertTrue(result.npcs().isEmpty());
            assertFalse(result.warnings().isEmpty());
        }
    }
}
