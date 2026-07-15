package exposed.thunder.thunderLBs.render;

import exposed.thunder.thunderLBs.leaderboard.Leaderboard;
import org.bukkit.Location;

public interface RenderBackend {
    String name();
    void register(Leaderboard board);
    void unregister(Leaderboard board);
    BoardDisplay spawn(Leaderboard board, Location location, DisplayOptions options);
    void shutdown();
}
