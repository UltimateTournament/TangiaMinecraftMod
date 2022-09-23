package co.tangia.minecraftmod;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber
public class TowerComponent {
    private final int width = 30;
    private final int depth = 30;
    private final int height = 100;
    private final int baseHeight = 5;
    private final int floorHeight = 12;
    private final int doorHeight = 3;
    private final int doorWidth = 3;
    private final int spaceGap = 3;
    private final int wallThick = 3;
    private final int stairLen = width / 3;
    private final int stairWidth = 2;
    private final int floors;
    private final int xStart;
    private final int yStart;
    private final int zStart;
    private final double lavaPerGameTime = 1.0 / (100_000);
    private long lastLavaEvent;
    private boolean lavaRising;
    private double lavaY;
    private int lavaLastYBlock;
    private BlockPos leverPos;

    private static final Logger LOGGER = LogUtils.getLogger();

    public TowerComponent(double xStart, double yStart, double zStart, int floors) {
        this.xStart = (int) xStart;
        this.yStart = (int) yStart;
        this.zStart = (int) zStart;
        this.floors = floors;
    }

    public void instantiate(Level level, boolean withLavaLever) {
        clearSpace(level);
        placeBasePlate(level);
        placeFloor(level, yStart, 0);
        placeLight(level, xStart + width / 2, yStart, zStart + depth / 2);
        if (withLavaLever)
            placeStartLever(level, xStart + width / 2 + 1, yStart, zStart + depth / 2);
        createDoor(level);
        for (int i = 1; i <= floors; i++) {
            placeFloor(level, yStart + i * floorHeight, 2);
            createStairs(level, yStart + (i - 1) * floorHeight);
            placeLight(level, xStart + width / 2, yStart + i * floorHeight, zStart + depth / 2);
        }
        LOGGER.info("tower built v2 {},{},{}", xStart, yStart, zStart);
        MinecraftForge.EVENT_BUS.register(this);
        TowerLog.append(new TowerData(xStart, yStart, zStart, floors));
    }

    private void placeStartLever(Level level, int x, int y, int z) {
        this.leverPos = new BlockPos(x, y, z);
        level.setBlockAndUpdate(leverPos, Blocks.LEVER.defaultBlockState());
        var blockPos = new BlockPos(x, y, z + 1);
        level.setBlockAndUpdate(blockPos, Blocks.BEDROCK.defaultBlockState());
    }

    private void placeLight(Level level, int x, int y, int z) {
        var bp = new BlockPos(x, y, z);
        level.setBlockAndUpdate(bp, Blocks.CAMPFIRE.defaultBlockState());
        var block = new CampfireBlockEntity(bp, Blocks.CAMPFIRE.defaultBlockState());
        level.setBlockEntity(block);
    }

    private void createDoor(Level level) {
        setDoor(level, Blocks.AIR.defaultBlockState());
    }

    private void sealDoor(Level level) {
        setDoor(level, Blocks.BEDROCK.defaultBlockState());
    }

    private void setDoor(Level level, BlockState doorBlock) {
        for (int y = yStart; y < yStart + doorHeight; y++) {
            for (int z = zStart + depth / 2; z < zStart + depth / 2 + doorWidth; z++) {
                for (int x = xStart; x < xStart + wallThick; x++) {
                    var bp = new BlockPos(x, y, z);
                    level.setBlock(bp, doorBlock, 3);
                }
            }
        }
    }

    private void placeFloor(Level level, int yStart, int maxRimWidth) {
        placeBase(level, yStart, 3, maxRimWidth);
        // walls are inset
        setBlocks(level,
            xStart, xStart + wallThick,
            yStart, yStart + floorHeight,
            zStart, zStart + depth,
            Blocks.BEDROCK.defaultBlockState());
        setBlocks(level,
            xStart + width - wallThick, xStart + width,
            yStart, yStart + floorHeight,
            zStart, zStart + depth,
            Blocks.BEDROCK.defaultBlockState());
        setBlocks(level,
            xStart, xStart + width,
            yStart, yStart + floorHeight,
            zStart, zStart + wallThick,
            Blocks.BEDROCK.defaultBlockState());
        setBlocks(level,
            xStart, xStart + width,
            yStart, yStart + floorHeight,
            zStart + depth - wallThick, zStart + depth,
            Blocks.BEDROCK.defaultBlockState());
    }

    private void createStairs(Level level, int yStart) {
        var inset = 3;
        setBlocks(level,
            xStart + width - wallThick - stairLen - inset, xStart + width - wallThick - inset,
            yStart, yStart + floorHeight + 1,
            zStart + depth - wallThick - stairWidth - inset, zStart + depth - wallThick - inset,
            Blocks.AIR.defaultBlockState());
        var yStep = (double) (floorHeight - 1) / (double) stairLen;
        for (int i = 0; i < stairLen; i++) {
            var y = (int) Math.round(yStart + yStep * i);
            setBlocks(level,
                xStart + width - wallThick - stairLen - inset + i, xStart + width - wallThick - stairLen - inset + i + 1,
                y, y + 1,
                zStart + depth - wallThick - stairWidth - inset, zStart + depth - wallThick - inset,
                Blocks.STONE.defaultBlockState());
        }
    }

    private void placeBase(Level level, int yStart, int thick, int maxRimWidth) {
        for (int i = 1; i <= thick; i++) {
            var gap = Math.max(i - thick + maxRimWidth, 0);
            setBlocks(level,
                xStart + wallThick + gap, xStart + width - wallThick - gap,
                yStart - thick + i - 1, yStart - thick + i,
                zStart + wallThick + gap, zStart + depth - wallThick - gap,
                Blocks.BEDROCK.defaultBlockState());
        }
    }

    private void placeBasePlate(Level level) {
        setBlocks(level,
            xStart, xStart + width,
            yStart - baseHeight, yStart,
            zStart, zStart + depth,
            Blocks.BEDROCK.defaultBlockState());
    }

    private void clearSpace(Level level) {
        setBlocks(level,
            xStart - spaceGap, xStart + width + spaceGap,
            yStart, yStart + height, zStart - spaceGap,
            zStart + depth + spaceGap,
            Blocks.AIR.defaultBlockState());
    }

    private void setBlocksRandom(Level level, int xRangeStart, int xRangeEnd, int yRangeStart, int yRangeEnd, int zRangeStart, int zRangeEnd, BlockState blockState, double chance) {
        for (int y = yRangeStart; y < yRangeEnd; y++) {
            for (int x = xRangeStart; x < xRangeEnd; x++) {
                for (int z = zRangeStart; z < zRangeEnd; z++) {
                    var bp = new BlockPos(x, y, z);
                    if (chance < 1.0 && Math.random() > chance)
                        continue;
                    level.setBlock(bp, blockState, 3);
                }
            }
        }
    }

    private void setBlocks(Level level, int xRangeStart, int xRangeEnd, int yRangeStart, int yRangeEnd, int zRangeStart, int zRangeEnd, BlockState blockState) {
        setBlocksRandom(level, xRangeStart, xRangeEnd, yRangeStart, yRangeEnd, zRangeStart, zRangeEnd, blockState, 1.0);
    }

    public void startLavaRising(Level level) {
        LOGGER.info("LAVA starts rising: " + level.dayTime());
        this.lavaRising = true;
        this.lastLavaEvent = level.dayTime() + 200;
        this.lavaY = this.yStart - 1;
        this.lavaLastYBlock = this.yStart - 2;
        sealDoor(level);
    }

    public void stopLavaRising() {
        LOGGER.info("LAVA stopped rising");
        this.lavaRising = false;
    }

    @SubscribeEvent
    public void onTick(TickEvent.WorldTickEvent event) {
        try {
            if (!lavaRising) {
                return;
            }
            long now = event.world.dayTime();
            lavaY += (now - lastLavaEvent) * lavaPerGameTime;
            if ((int) lavaY > lavaLastYBlock) {
                LOGGER.info("LAVA IS RISING: " + lavaY);
                setBlocksRandom(event.world,
                    xStart + wallThick, xStart + width - wallThick,
                    (int) lavaY, ((int) lavaY) + 1,
                    zStart + wallThick, zStart + depth - wallThick,
                    Blocks.LAVA.defaultBlockState(),
                    0.2);
                lavaLastYBlock = (int) lavaY;
                this.lastLavaEvent = now;
            }
            if (lavaLastYBlock > yStart + height)
                stopLavaRising();
        } catch (Exception e) {
            LOGGER.error("exception in onTick", e);
        }
    }

    @SubscribeEvent
    public void onLever(PlayerInteractEvent.RightClickBlock rcbEvent) {
        try {
            if (leverPos == null)
                return;
            if (leverPos.equals(rcbEvent.getPos())) {
                startLavaRising(rcbEvent.getWorld());
            }

        } catch (Exception e) {
            LOGGER.error("exception in onLever", e);
        }
    }
}
