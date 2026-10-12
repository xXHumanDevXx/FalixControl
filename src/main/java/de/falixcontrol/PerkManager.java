package de.falixcontrol;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Verwaltet zeitlich begrenzte Perks (Abos). Vergibt und entzieht die Rechte aus config.yml. */
public class PerkManager {

    public record Subscription(String player, long until) {
        public boolean active() {
            return until > System.currentTimeMillis();
        }
    }

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final FalixControl plugin;
    private final File file;
    private final Map<UUID, Subscription> subs = new HashMap<>();
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public PerkManager(FalixControl plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "perks.yml");
    }

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = y.getConfigurationSection("players");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                subs.put(UUID.fromString(key),
                        new Subscription(sec.getString(key + ".name", "?"), sec.getLong(key + ".until", 0L)));
            } catch (IllegalArgumentException ignored) { }
        }
    }

    public synchronized void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Subscription> en : subs.entrySet()) {
            String base = "players." + en.getKey() + ".";
            y.set(base + "name", en.getValue().player());
            y.set(base + "until", en.getValue().until());
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("perks.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    /** Verlängert das Abo. Läuft noch eines, wird ab dessen Ende addiert. */
    public void grant(UUID id, String name, int days) {
        Subscription old = subs.get(id);
        long now = System.currentTimeMillis();
        long base = (old != null && old.until() > now) ? old.until() : now;
        subs.put(id, new Subscription(name, base + days * 86_400_000L));
        save();
        Player p = Bukkit.getPlayer(id);
        if (p != null) apply(p);
    }

    public boolean revoke(UUID id) {
        Subscription removed = subs.remove(id);
        save();
        Player p = Bukkit.getPlayer(id);
        if (p != null) clear(p);
        return removed != null;
    }

    public Subscription get(UUID id) {
        return subs.get(id);
    }

    public boolean isActive(UUID id) {
        Subscription s = subs.get(id);
        return s != null && s.active();
    }

    public Map<UUID, Subscription> all() {
        return Collections.unmodifiableMap(subs);
    }

    public String expiryText(Subscription s) {
        return FMT.format(Instant.ofEpochMilli(s.until()));
    }

    /** Gibt dem Spieler die Perk-Rechte aus config.yml, wenn sein Abo aktiv ist. */
    public void apply(Player p) {
        clear(p);
        if (!isActive(p.getUniqueId())) return;
        PermissionAttachment att = p.addAttachment(plugin);
        for (String node : plugin.getConfig().getStringList("perks.permissions")) {
            att.setPermission(node, true);
        }
        attachments.put(p.getUniqueId(), att);
    }

    /** Entfernt die Perk-Rechte des Spielers. */
    public void clear(Player p) {
        PermissionAttachment att = attachments.remove(p.getUniqueId());
        if (att != null) p.removeAttachment(att);
    }

    /** Entfernt abgelaufene Abos und informiert online Spieler. */
    public void checkExpired() {
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, Subscription> en : subs.entrySet()) {
            if (!en.getValue().active()) expired.add(en.getKey());
        }
        if (expired.isEmpty()) return;
        for (UUID id : expired) {
            subs.remove(id);
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                clear(p);
                p.sendMessage(plugin.msg("perk-expired",
                        "{name}", plugin.getConfig().getString("perks.name", "Ultimate")));
            }
        }
        save();
    }
}
