package exposed.thunder.thunderLBs.placeholder;

import exposed.thunder.thunderLBs.ThunderLBs;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Objects;

public final class PlaceholderBridge {
    private final ThunderLBs plugin;
    private final boolean available;

    public PlaceholderBridge(ThunderLBs plugin) {
        this.plugin = plugin;
        this.available = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (!available) {
            plugin.getLogger().warning("PlaceholderAPI was not detected. Placeholders will not be resolved.");
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public String resolve(String placeholder, OfflinePlayer player) {
        if (!available) {
            return placeholder;
        }
        try {
            return PlaceholderAPI.setPlaceholders(player, placeholder);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Failed to resolve placeholder '" + placeholder + "': " + throwable.getMessage());
            return placeholder;
        }
    }

    public String resolve(String placeholder) {
        return resolve(placeholder, null);
    }
}
