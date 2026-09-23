package com.kirugoldzzzz.puppeteer;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

public final class VaultEconomy {

    private volatile Economy economy;

    public boolean available() {
        return provider() != null;
    }

    public boolean deposit(UUID player, double amount) {
        Economy current = provider();
        if (current == null || amount <= 0.0D) {
            return false;
        }
        return current.depositPlayer(offline(player), amount).transactionSuccess();
    }

    public boolean withdraw(UUID player, double amount) {
        Economy current = provider();
        if (current == null) {
            return false;
        }
        if (amount <= 0.0D) {
            return true;
        }
        OfflinePlayer target = offline(player);
        if (!current.has(target, amount)) {
            return false;
        }
        return current.withdrawPlayer(target, amount).transactionSuccess();
    }

    private Economy provider() {
        Economy current = economy;
        if (current != null) {
            return current;
        }
        if (Bukkit.getServer() == null || Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return null;
        }
        RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (registration != null) {
            economy = registration.getProvider();
        }
        return economy;
    }

    private static OfflinePlayer offline(UUID player) {
        return Bukkit.getOfflinePlayer(player);
    }
}
