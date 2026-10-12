package de.falixcontrol;

import de.falixcontrol.AccessManager.Access;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;

/** Blendet Befehle in der Tab-Liste aus und blockiert nicht erlaubte Befehle. */
public class CommandFilterListener implements Listener {

    private final FalixControl plugin;

    public CommandFilterListener(FalixControl plugin) {
        this.plugin = plugin;
    }

    /** Befehle, die der Spieler nicht darf, werden ihm gar nicht erst angezeigt. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandSend(PlayerCommandSendEvent e) {
        if (!plugin.getConfig().getBoolean("settings.hide-commands", true)) return;
        Access a = plugin.access().access(e.getPlayer());
        if (a.all() || a.commands().contains("*")) return;
        e.getCommands().removeIf(cmd -> !a.allows(cmd));
    }

    /** Auch wenn jemand den Befehl selbst tippt, wird er blockiert. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage().trim();
        if (msg.length() < 2) return;
        String label = msg.substring(1).split(" ")[0];
        if (plugin.access().access(e.getPlayer()).allows(label)) return;

        e.setCancelled(true);
        e.getPlayer().sendMessage(plugin.msg("command-blocked", "{command}", label));
    }
}
