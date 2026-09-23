package com.kirugoldzzzz.puppeteer;

import com.foliagui.FoliaGUI;
import com.kirugoldzzzz.puppeteer.api.PuppeteerApi;
import com.kirugoldzzzz.puppeteer.common.command.NexusCommand;
import com.kirugoldzzzz.puppeteer.common.config.ConfigFile;
import com.kirugoldzzzz.puppeteer.common.gui.Guis;
import com.kirugoldzzzz.puppeteer.common.platform.Telemetry;
import com.kirugoldzzzz.puppeteer.common.platform.UpdateChecker;
import com.kirugoldzzzz.puppeteer.common.scheduler.Scheduling;
import com.kirugoldzzzz.puppeteer.common.text.Messages;
import com.kirugoldzzzz.puppeteer.common.text.Tr;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class Puppeteer extends JavaPlugin {

    private static final int BSTATS_ID = 0;
    private static final String REPOSITORY = "SpirtySprite/Puppeteer";

    private final Telemetry telemetry = new Telemetry();
    private NpcService service;

    @Override
    public void onEnable() {
        Scheduling.bind(this);
        ConfigFile settings = loadSettings();
        FoliaGUI.init(this);
        Guis.installTheme();
        Tr.seedLocalized(this, "npcs.yml");
        ConfigFile npcs = new ConfigFile(this, "npcs.yml", "npcs").load();
        VaultEconomy economy = new VaultEconomy();
        service = NpcService.create(this, npcs, economy);
        if (service == null) {
            getLogger().severe(Tr.t("Les PNJ ne peuvent pas être chargés sur ce serveur, le plugin reste inactif."));
            return;
        }
        NpcCommand command = new NpcCommand(service);
        command.onReload(this::loadSettings);
        bind("npc", command);
        getServer().getPluginManager().registerEvents(new NpcListener(service), this);
        Scheduling.global(service::apply);
        if (!economy.available()) {
            getLogger().info(Tr.t("Vault est absent : les actions GIVE_MONEY et TAKE_MONEY sont inactives."));
        }
        getServer().getServicesManager().register(PuppeteerApi.class, new PuppeteerService(service), this,
                ServicePriority.Normal);
        if (settings.get().getBoolean("update-checker", true)) {
            new UpdateChecker(this, REPOSITORY, "puppeteer.admin.npc").start();
        }
        NpcService running = service;
        telemetry.start(this, BSTATS_ID, Map.of("npcs", () -> String.valueOf(running.activeCount())));
    }

    private ConfigFile loadSettings() {
        ConfigFile settings = new ConfigFile(this, "config.yml").load();
        Tr.configure(this, settings.get().getString("language", "en"));
        new ConfigFile(this, "lang/messages_fr.yml").load();
        Messages.load(new ConfigFile(this, Tr.messagesFile(this)).load().get());
        return settings;
    }

    @Override
    public void onDisable() {
        telemetry.stop();
        getServer().getServicesManager().unregisterAll(this);
        if (service != null) {
            service.close();
        }
        FoliaGUI.shutdown();
    }

    private void bind(String name, NexusCommand executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning(Tr.t("La commande ") + name + Tr.t(" est absente du plugin.yml"));
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}
