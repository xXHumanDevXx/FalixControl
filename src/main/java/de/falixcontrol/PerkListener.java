package de.falixcontrol;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Vergibt beim Join die Perk-Rechte und entfernt sie beim Verlassen. */
public class PerkListener implements Listener {

    private final FalixControl plugin;

    public PerkListener(FalixControl plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.perks().apply(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.perks().clear(e.getPlayer());
    }
}
