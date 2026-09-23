package com.kirugoldzzzz.puppeteer;

import com.foliagui.FoliaGUI;
import com.kirugoldzzzz.puppeteer.common.command.NexusCommand;
import com.kirugoldzzzz.puppeteer.common.config.ConfigFile;
import com.kirugoldzzzz.puppeteer.common.gui.Guis;
import com.kirugoldzzzz.puppeteer.common.scheduler.Scheduling;
import com.kirugoldzzzz.puppeteer.common.text.Messages;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Puppeteer extends JavaPlugin {

    private NpcService service;

    @Override
    public void onEnable() {
        Scheduling.bind(this);
        FoliaGUI.init(this);
        Guis.installTheme();
        Messages.load(new ConfigFile(this, "messages.yml").load().get());
        ConfigFile npcs = new ConfigFile(this, "npcs.yml", "npcs").load();
        VaultEconomy economy = new VaultEconomy();
        service = NpcService.create(this, npcs, economy);
        if (service == null) {
            getLogger().severe("Les PNJ ne peuvent pas être chargés sur ce serveur, le plugin reste inactif.");
            return;
        }
        bind("npc", new NpcCommand(service));
        getServer().getPluginManager().registerEvents(new NpcListener(service), this);
        Scheduling.global(service::apply);
        if (!economy.available()) {
            getLogger().info("Vault est absent : les actions GIVE_MONEY et TAKE_MONEY sont inactives.");
        }
    }

    @Override
    public void onDisable() {
        if (service != null) {
            service.close();
        }
        FoliaGUI.shutdown();
    }

    private void bind(String name, NexusCommand executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("La commande " + name + " est absente du plugin.yml");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}
