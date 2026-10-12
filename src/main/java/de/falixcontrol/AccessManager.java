package de.falixcontrol;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Entscheidet, welche Befehle ein Spieler sehen und nutzen darf. */
public class AccessManager {

    /** Ergebnis für einen Spieler: erlaubte Befehle und woher die Regel kommt. */
    public record Access(boolean all, Set<String> commands, String source) {

        public boolean allows(String label) {
            if (all || commands.contains("*")) return true;
            String l = label.toLowerCase(Locale.ROOT);
            if (l.startsWith("/")) l = l.substring(1);
            // "minecraft:ban" oder "falixguard:ban" zählt auch als "ban"
            String base = l.contains(":") ? l.substring(l.indexOf(':') + 1) : l;
            return commands.contains(l) || commands.contains(base);
        }
    }

    private final FalixControl plugin;
    private final Map<String, Set<String>> groups = new LinkedHashMap<>();
    private final Map<String, Set<String>> players = new HashMap<>();
    private String defaultGroup = "spieler";

    public AccessManager(FalixControl plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        groups.clear();
        players.clear();
        var cfg = plugin.getConfig();
        defaultGroup = cfg.getString("settings.default-group", "spieler").toLowerCase(Locale.ROOT);

        ConfigurationSection g = cfg.getConfigurationSection("groups");
        if (g != null) {
            for (String key : g.getKeys(false)) {
                groups.put(key.toLowerCase(Locale.ROOT), normalize(g.getStringList(key + ".commands")));
            }
        }

        ConfigurationSection p = cfg.getConfigurationSection("players");
        if (p != null) {
            for (String key : p.getKeys(false)) {
                players.put(key.toLowerCase(Locale.ROOT), normalize(p.getStringList(key)));
            }
        }
    }

    private Set<String> normalize(List<String> list) {
        Set<String> out = new LinkedHashSet<>();
        for (String s : list) {
            if (s != null && !s.isBlank()) out.add(s.trim().toLowerCase(Locale.ROOT));
        }
        return out;
    }

    /** Reihenfolge: Bypass-Recht, dann Spieler-Eintrag, dann Gruppe, dann Standardgruppe. */
    public Access access(Player p) {
        if (p.hasPermission("falixcontrol.bypass")) {
            return new Access(true, Set.of(), "Bypass (falixcontrol.bypass)");
        }
        Set<String> own = players.get(p.getName().toLowerCase(Locale.ROOT));
        if (own != null) {
            return new Access(false, own, "Spieler-Eintrag");
        }
        for (Map.Entry<String, Set<String>> e : groups.entrySet()) {
            if (p.hasPermission("falixcontrol.group." + e.getKey())) {
                return new Access(false, e.getValue(), "Gruppe " + e.getKey());
            }
        }
        return new Access(false, groups.getOrDefault(defaultGroup, Set.of()), "Standardgruppe " + defaultGroup);
    }
}
