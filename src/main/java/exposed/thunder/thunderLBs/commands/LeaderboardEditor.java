package exposed.thunder.thunderLBs.commands;

import org.bukkit.entity.Player;

public interface LeaderboardEditor {
    void openSelector(Player player);

    void openBoard(Player player, String id);
}
