package de.falixcontrol;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/** /falixcontrol reload | check <Spieler> | test <Spieler> <Befehl> */
public class ControlCommand implements CommandExecutor, TabCompleter {

    private final FalixControl plugin;

    public ControlCommand(FalixControl plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (a.length == 0) {
            usage(s);
            return true;
        }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadAll();
                s.sendMessage(plugin.msg("reload-done"));
            }
            case "check" -> check(s, a);
            case "test" -> test(s, a);
            default -> usage(s);
        }
        return true;
    }

    private void check(CommandSender s, String[] a) {
        if (a.length < 2) {
            usage(s);
            return;
        }
        Player t = Bukkit.getPlayerExact(a[1]);
        if (t == null) {
            s.sendMessage(plugin.msg("player-offline", "{player}", a[1]));
            return;
        }
        AccessManager.Access acc = plugin.access().access(t);
        String cmds;
        if (acc.all()) {
            cmds = "alle Befehle";
        } else if (acc.commands().isEmpty()) {
            cmds = "keine";
        } else {
            cmds = String.join(", ", new TreeSet<>(acc.commands()));
        }
        s.sendMessage(plugin.msg("check-header", "{player}", t.getName()));
        s.sendMessage(plugin.msg("check-source", "{source}", acc.source()));
        s.sendMessage(plugin.msg("check-commands", "{commands}", cmds));
    }

    private void test(CommandSender s, String[] a) {
        if (a.length < 3) {
            usage(s);
            return;
        }
        Player t = Bukkit.getPlayerExact(a[1]);
        if (t == null) {
            s.sendMessage(plugin.msg("player-offline", "{player}", a[1]));
            return;
        }
        String cmd = a[2].startsWith("/") ? a[2].substring(1) : a[2];
        boolean ok = plugin.access().access(t).allows(cmd);
        s.sendMessage(plugin.msg(ok ? "test-allowed" : "test-denied",
                "{player}", t.getName(), "{command}", cmd));
    }

    private void usage(CommandSender s) {
        s.sendMessage(plugin.msg("usage", "{usage}",
                "/falixcontrol <reload | check <Spieler> | test <Spieler> <Befehl>>"));
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        if (a.length == 1) return filter(List.of("reload", "check", "test"), a[0]);
        if (a.length == 2 && (a[0].equalsIgnoreCase("check") || a[0].equalsIgnoreCase("test"))) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return filter(names, a[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String start) {
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(start.toLowerCase(Locale.ROOT))) out.add(o);
        }
        return out;
    }
}
