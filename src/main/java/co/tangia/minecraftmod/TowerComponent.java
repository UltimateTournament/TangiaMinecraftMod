package co.tangia.minecraftmod;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber
public class TowerComponent {
    private final int width = 30;
    private final int depth = 30;
    private final int height = 100;
    private final int baseHeight = 3;
    private final int floorHeight = 5;
    private final int doorHeight = 3;
    private final int doorWidth = 3;
    private final int spaceGap = 3;
    private final int wallThick = 3;
    private final int stairLen = 10;
    private final int stairWidth = 2;
    private final int floors = 20;
    private final int xStart;
    private final int yStart;
    private final int zStart;
    private final double lavaPerTick = 1.0 / (10 * 30);
    private boolean lavaRising;
    private double lavaY;
    private int lavaLastYBlock;

    //HACK: how to get tick events in all class instances?
    private static TowerComponent lastInstance;

    private static final Logger LOGGER = LogUtils.getLogger();

    public TowerComponent(double xStart, double yStart, double zStart) {
        this.xStart = (int) xStart;
        this.yStart = (int) yStart;
        this.zStart = (int) zStart;
        lastInstance = this;
    }

    public void setBlockEntities(Level level) {
        clearSpace(level);
        placeBase(level, yStart, baseHeight);
        placeFloor(level, yStart);
        createDoor(level);
        for (int i = 1; i <= floors; i++) {
            placeFloor(level, yStart + i * floorHeight);
            createStairs(level, yStart + (i - 1) * floorHeight);
            placeLight(level, xStart + width / 2, yStart + i * floorHeight, zStart + depth / 2);
        }
        LOGGER.info("tower built v2 {},{},{}", xStart, yStart, zStart);
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

    private void placeFloor(Level level, int yStart) {
        placeBase(level, yStart, 1);
        for (int x = xStart; x < xStart + width; x++) {
            var zStep = depth - 1;
            if (x == xStart || x == xStart + width - 1)
                zStep = 1;
            for (int z = zStart; z < zStart + depth; z += zStep) {
                for (int y = yStart; y < yStart + floorHeight; y++) {
                    var bp = new BlockPos(x, y, z);
                    level.setBlock(bp, Blocks.BEDROCK.defaultBlockState(), 3);
                }
            }
        }
        // walls are inset
        setBlocks(level,
            xStart, xStart + wallThick,
            zStart, zStart + depth,
            yStart, yStart + floorHeight,
            Blocks.BEDROCK.defaultBlockState());
        setBlocks(level,
            xStart + width - wallThick, xStart + width,
            zStart, zStart + depth,
            yStart, yStart + floorHeight,
            Blocks.BEDROCK.defaultBlockState());
        setBlocks(level,
            xStart, xStart + width,
            zStart, zStart + wallThick,
            yStart, yStart + floorHeight,
            Blocks.BEDROCK.defaultBlockState());
        setBlocks(level,
            xStart, xStart + width,
            zStart + depth - wallThick, zStart + depth,
            yStart, yStart + floorHeight,
            Blocks.BEDROCK.defaultBlockState());
    }

    private void createStairs(Level level, int yStart) {
        setBlocks(level,
            xStart + width - wallThick - stairLen, xStart + width - wallThick,
            zStart + depth - wallThick - stairWidth, zStart + depth - wallThick,
            yStart, yStart + floorHeight + 1,
            Blocks.AIR.defaultBlockState());
        var yStep = (double) (floorHeight - 1) / (double) stairLen;
        for (int i = 0; i < stairLen; i++) {
            var y = (int) Math.round(yStart + yStep * i);
            setBlocks(level,
                xStart + width - wallThick - stairLen + i, xStart + width - wallThick - stairLen + i + 1,
                zStart + depth - wallThick - stairWidth, zStart + depth - wallThick,
                y, y + 1,
                Blocks.STONE.defaultBlockState());
        }
    }

    private void placeBase(Level level, int yStart, int thick) {
        setBlocks(level,
            xStart, xStart + width,
            zStart, zStart + depth,
            yStart - thick, yStart,
            Blocks.BEDROCK.defaultBlockState());
    }

    private void clearSpace(Level level) {
        setBlocks(level,
            xStart - spaceGap, xStart + width + spaceGap,
            zStart - spaceGap, zStart + depth + spaceGap,
            yStart, yStart + height,
            Blocks.AIR.defaultBlockState());
    }

    private void setBlocks(Level level, int xRangeStart, int xRangeEnd, int zRangeStart, int zRangeEnd, int yRangeStart, int yRangeEnd, BlockState blockState) {
        for (int x = xRangeStart; x < xRangeEnd; x++) {
            for (int z = zRangeStart; z < zRangeEnd; z++) {
                for (int y = yRangeStart; y < yRangeEnd; y++) {
                    var bp = new BlockPos(x, y, z);
                    level.setBlock(bp, blockState, 3);
                }
            }
        }
    }

    public void startLavaRising(Level level) {
        LOGGER.info("LAVA starts rising");
        this.lavaRising = true;
        this.lavaY = this.yStart;
        this.lavaLastYBlock = this.yStart - 1;
        sealDoor(level);
    }
    public void stopLavaRising() {
        LOGGER.info("LAVA stopped rising");
        this.lavaRising = false;
    }

    private void worldTick(TickEvent.WorldTickEvent event) {
        if (!lavaRising) {
            return;
        }
        lavaY += lavaPerTick;
        if ((int) lavaY > lavaLastYBlock) {
            LOGGER.info("LAVA IS RISING");
            setBlocks(event.world,
                xStart + wallThick, xStart + width - wallThick,
                zStart + wallThick, zStart + depth - wallThick,
                lavaLastYBlock + 1, (int) lavaY + 1,
                Blocks.LAVA.defaultBlockState());
            lavaLastYBlock = (int) lavaY;
        }
        if (lavaLastYBlock > yStart + height)
            stopLavaRising();
    }

    @SubscribeEvent
    public static void onTick(TickEvent.WorldTickEvent event) {
        if (lastInstance == null)
            return;
        lastInstance.worldTick(event);
    }
}
