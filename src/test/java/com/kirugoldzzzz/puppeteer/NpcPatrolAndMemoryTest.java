package com.kirugoldzzzz.puppeteer;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcPatrolAndMemoryTest {

    private static final UUID ALEX = new UUID(0L, 1L);
    private static final UUID SAM = new UUID(0L, 2L);

    @TempDir
    Path folder;

    @Test
    void patrolPointsParseAndInvalidOnesAreReported() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("""
                speed: 50
                pause-ticks: -5
                points:
                  - "4.5 64 4.5"
                  - "1, 70, -3"
                  - "somewhere"
                """);
        List<String> warnings = new ArrayList<>();
        NpcPatrol patrol = NpcPatrol.read(yaml.getRoot(), "cat", warnings);

        assertTrue(patrol.active());
        assertEquals(2, patrol.points().size());
        assertEquals(new NpcPatrol.Point(1.0D, 70.0D, -3.0D), patrol.points().get(1));
        assertEquals(10.0D, patrol.speed());
        assertEquals(0L, patrol.pauseTicks());
        assertEquals(1, warnings.size());
        assertNull(NpcPatrol.parse("1 2"));
        assertFalse(NpcPatrol.read(null, "cat", warnings).active());
    }

    @Test
    void oneTimeActionsAreRememberedPerPlayerAndSurviveARestart() {
        File file = folder.resolve("memory.yml").toFile();
        NpcMemory memory = new NpcMemory(file);
        memory.load();

        assertTrue(memory.claim("cat action 4", ALEX));
        assertFalse(memory.claim("cat action 4", ALEX));
        assertTrue(memory.claim("cat action 4", SAM));
        assertTrue(memory.claim("guide action 1", ALEX));

        NpcMemory reloaded = new NpcMemory(file);
        reloaded.load();
        assertTrue(reloaded.seen("cat action 4", ALEX));
        assertEquals(1, reloaded.forget("cat", ALEX));
        assertFalse(reloaded.seen("cat action 4", ALEX));
        assertTrue(reloaded.seen("cat action 4", SAM));
        assertEquals(1, reloaded.forget("cat", null));
        assertTrue(reloaded.seen("guide action 1", ALEX));
    }
}
