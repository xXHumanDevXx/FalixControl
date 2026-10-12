package de.falixcontrol;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** /ultimate | info <Spieler> | grant <Spieler> <Tage> | remove <Spieler> | list */
public class PerkCommand implements CommandExecutor, TabCompleter {

    private record Target(UUID id, String name) { }

    private final FalixControl plugin;

    public PerkCommand(FalixControl plugin) {
        this.plugin = plugin;
    }

    private String perkName() {
        return plugin.getConfig().getString("perks.name", "Ultimate");
    }

    private String perkPrice() {
        return plugin.getConfig().getString("perks.price", "");
    }

    /** Nachricht mit automatisch eingesetztem Perk-Namen und Preis. */
    private String m(String key, String... pairs) {
        List<String> args = new ArrayList<>(List.of("{name}", perkName(), "{price}", perkPrice()));
        args.addAll(List.of(pairs));
        return plugin.msg(key, args.toArray(new String[0]));
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (a.length == 0) {
            status(s);
            return true;
        }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "info" -> {
                if (!admin(s)) return true;
                if (a.length < 2) {
                    s.sendMessage(m("perk-usage"));
                    return true;
                }
                Target t = resolve(s, a[1]);
                if (t == null) return true;
                PerkManager.Subscription sub = plugin.perks().get(t.id());
                if (sub != null && sub.active()) {
                    s.sendMessage(m("perk-info-active", "{player}", t.name(),
                            "{until}", plugin.perks().expiryText(sub)));
                } else {
                    s.sendMessage(m("perk-info-inactive", "{player}", t.name()));
                }
            }
            case "grant" -> grant(s, a);
            case "remove" -> {
                if (!admin(s)) return true;
                if (a.length < 2) {
                    s.sendMessage(m("perk-usage"));
                    return true;
                }
                Target t = resolve(s, a[1]);
                if (t == null) return true;
                if (plugin.perks().revoke(t.id())) {
                    s.sendMessage(m("perk-revoked", "{player}", t.name()));
                } else {
                    s.sendMessage(m("perk-not-active", "{player}", t.name()));
                }
            }
            case "list" -> {
                if (admin(s)) list(s);
            }
            default -> s.sendMessage(m("perk-usage"));
        }
        return true;
    }

    private void status(CommandSender s) {
        if (!(s instanceof Player p)) {
            s.sendMessage(m("perk-usage"));
            return;
        }
        PerkManager.Subscription sub = plugin.perks().get(p.getUniqueId());
        if (sub != null && sub.active()) {
            p.sendMessage(m("perk-status-active", "{until}", plugin.perks().expiryText(sub)));
        } else {
            p.sendMessage(m("perk-status-inactive"));
        }
    }

    private void grant(CommandSender s, String[] a) {
        if (!admin(s)) return;
        if (a.length < 3) {
            s.sendMessage(m("perk-usage"));
            return;
        }
        int days;
        try {
            days = Integer.parseInt(a[2]);
        } catch (NumberFormatException ex) {
            s.sendMessage(m("perk-invalid-days"));
            return;
        }
        if (days <= 0) {
            s.sendMessage(m("perk-invalid-days"));
            return;
        }
        Target t = resolve(s, a[1]);
        if (t == null) return;

        plugin.perks().grant(t.id(), t.name(), days);
        String until = plugin.perks().expiryText(plugin.perks().get(t.id()));
        s.sendMessage(m("perk-granted", "{player}", t.name(),
                "{days}", String.valueOf(days), "{until}", until));

        Player online = Bukkit.getPlayer(t.id());
        if (online != null) {
            online.sendMessage(m("perk-granted-you", "{days}", String.valueOf(days), "{until}", until));
        }
    }

    private void list(CommandSender s) {
        List<UUID> active = new ArrayList<>();
        for (Map.Entry<UUID, PerkManager.Subscription> en : plugin.perks().all().entrySet()) {
            if (en.getValue().active()) active.add(en.getKey());
        }
        if (active.isEmpty()) {
            s.sendMessage(m("perk-list-none"));
            return;
        }
        s.sendMessage(m("perk-list-header", "{count}", String.valueOf(active.size())));
        for (UUID id : active) {
            PerkManager.Subscription sub = plugin.perks().get(id);
            s.sendMessage(m("perk-list-entry", "{player}", sub.player(),
                    "{until}", plugin.perks().expiryText(sub)));
        }
    }

    private boolean admin(CommandSender s) {
        if (s.hasPermission("falixcontrol.perks.admin")) return true;
        s.sendMessage(m("perk-no-permission"));
        return false;
    }

    /** Findet Online- und Offline-Spieler, die schon einmal auf dem Server waren. */
    private Target resolve(CommandSender s, String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return new Target(online.getUniqueId(), online.getName());
        OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        if (!op.hasPlayedBefore()) {
            s.sendMessage(m("perk-player-not-found", "{player}", name));
            return null;
        }
        return new Target(op.getUniqueId(), name);
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            for (String o : List.of("info", "grant", "remove", "list")) {
                if (o.startsWith(a[0].toLowerCase(Locale.ROOT))) out.add(o);
            }
        }
        return out;
    }
}
