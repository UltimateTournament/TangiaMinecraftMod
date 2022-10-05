package co.tangia.minecraftmod;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.UUID;

@Mod.EventBusSubscriber
public class PrimedTntComponent {
    public int primeTicks;
    public float xOffset;
    public float yOffset;
    public float zOffset;
    public int delaySeconds;
    private final long startTick;
    private boolean stopListening;
    private final UUID playerUUID;

    private static final Logger LOGGER = LogUtils.getLogger();

    public PrimedTntComponent(long startTick, UUID playerUUID, float xOffset, float yOffset, float zOffset, int primeTicks, int delaySeconds) {
        this.startTick = startTick;
        this.stopListening = false;
        this.playerUUID = playerUUID;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
        this.primeTicks = primeTicks;
        this.delaySeconds = delaySeconds;
    }

    public void init() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onTick(TickEvent.WorldTickEvent event) {
        if (this.stopListening) {
            MinecraftForge.EVENT_BUS.unregister(this);
            return;
        }
        if (event.world.dayTime() <= this.startTick + this.delaySeconds * 20) {
            return;
        }
        var player = event.world.getPlayerByUUID(this.playerUUID);
        this.stopListening = true;
        if (player == null) {
            return;
        }
        LOGGER.info("Spawning tnt");
        var liveTNT = new PrimedTnt(player.level, player.getX()+this.xOffset, player.getY()+this.yOffset, player.getZ()+this.zOffset, null);
        if (this.primeTicks != 0) {
            liveTNT.setFuse(this.primeTicks);
        }
        player.level.addFreshEntity(liveTNT);
    }
}
