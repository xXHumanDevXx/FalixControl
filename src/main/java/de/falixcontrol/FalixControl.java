package de.falixcontrol;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Hauptklasse: lädt die Config und registriert Listener und Befehl. */
public class FalixControl extends JavaPlugin {

    private AccessManager access;
    private PerkManager perks;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        access = new AccessManager(this);
        access.reload();

        Bukkit.getPluginManager().registerEvents(new CommandFilterListener(this), this);

        ControlCommand executor = new ControlCommand(this);
        PluginCommand cmd = getCommand("falixcontrol");
        if (cmd != null) {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }
        perks = new PerkManager(this);
        perks.load();
        Bukkit.getPluginManager().registerEvents(new PerkListener(this), this);
        PerkCommand perkExecutor = new PerkCommand(this);
        PluginCommand ultimate = getCommand("ultimate");
        if (ultimate != null) {
            ultimate.setExecutor(perkExecutor);
            ultimate.setTabCompleter(perkExecutor);
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            perks.apply(p);
        }
        // Jede Minute abgelaufene Abos entfernen (1200 Ticks = 60 Sekunden)
        Bukkit.getScheduler().runTaskTimer(this, perks::checkExpired, 1200L, 1200L);

        getLogger().info("FalixControl aktiviert - Befehlssichtbarkeit ist aktiv.");
    }

    @Override
    public void onDisable() {
        if (perks != null) perks.save();
    }

    public PerkManager perks() {
        return perks;
    }

    public AccessManager access() {
        return access;
    }

    /** Config neu laden und die Befehlsliste aller Online-Spieler aktualisieren. */
    public void reloadAll() {
        reloadConfig();
        access.reload();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.updateCommands();
        }
    }

    public String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    /** Nachricht aus messages.<key>, Platzhalter als Paare: "{player}", name, ... */
    public String msg(String key, String... pairs) {
        String raw = getConfig().getString("messages." + key, "&cFehlende Nachricht: " + key);
        raw = raw.replace("{prefix}", getConfig().getString("messages.prefix", ""));
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            raw = raw.replace(pairs[i], pairs[i + 1]);
        }
        return color(raw);
    }
}
