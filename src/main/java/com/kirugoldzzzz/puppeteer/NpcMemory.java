package com.kirugoldzzzz.puppeteer;

import com.kirugoldzzzz.puppeteer.common.log.LogTopic;
import com.kirugoldzzzz.puppeteer.common.log.PluginLog;
import com.kirugoldzzzz.puppeteer.common.text.Tr;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class NpcMemory {

    private static volatile NpcMemory active = new NpcMemory(null);

    private final File file;
    private final Set<String> done = new HashSet<>();

    public NpcMemory(File file) {
        this.file = file;
    }

    public static NpcMemory active() {
        return active;
    }

    public static void activate(NpcMemory memory) {
        active = memory;
    }

    public synchronized void load() {
        done.clear();
        if (file != null && file.isFile()) {
            done.addAll(YamlConfiguration.loadConfiguration(file).getStringList("done"));
        }
    }

    public synchronized boolean claim(String action, UUID player) {
        boolean first = done.add(action + "|" + player);
        if (first) {
            persist();
        }
        return first;
    }

    public synchronized boolean seen(String action, UUID player) {
        return done.contains(action + "|" + player);
    }

    public synchronized int forget(String npc, UUID player) {
        List<String> removed = new ArrayList<>();
        for (String entry : done) {
            if (entry.startsWith(npc + " ") && (player == null || entry.endsWith("|" + player))) {
                removed.add(entry);
            }
        }
        removed.forEach(done::remove);
        if (!removed.isEmpty()) {
            persist();
        }
        return removed.size();
    }

    private void persist() {
        if (file == null) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("done", new ArrayList<>(done));
        try {
            yaml.save(file);
        } catch (IOException failure) {
            PluginLog.warn(LogTopic.NPC, Tr.t("Mémoire des PNJ non enregistrée : ") + failure.getMessage());
        }
    }
}
