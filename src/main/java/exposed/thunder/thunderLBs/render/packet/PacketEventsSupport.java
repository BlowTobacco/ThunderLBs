package exposed.thunder.thunderLBs.render.packet;

import com.github.retrooper.packetevents.PacketEvents;
import exposed.thunder.thunderLBs.ThunderLBs;
import exposed.thunder.thunderLBs.render.RenderBackend;

public final class PacketEventsSupport {
    private PacketEventsSupport() {
    }

    public static boolean isReady() {
        return PacketEvents.getAPI() != null && PacketEvents.getAPI().isInitialized();
    }

    public static RenderBackend createBackend(ThunderLBs plugin) {
        return new PacketRenderBackend(plugin);
    }
}
