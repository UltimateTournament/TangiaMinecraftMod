package co.tangia.minecraftmod;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;

public class TowerComponent {
    private final int width = 30;
    private final int depth = 30;
    private final int height = 120;
    private final int baseHeight = 3;
    private final int xStart;
    private final int yStart;
    private final int zStart;

    private static final Logger LOGGER = LogUtils.getLogger();

    public TowerComponent(double xStart, double yStart, double zStart) {
        this.xStart = (int) xStart;
        this.yStart = (int) yStart;
        this.zStart = (int) zStart;
    }

    public void setBlockEntities(Level level) {
        clearSpace(level);
        placeBase(level);
        LOGGER.info("tower built {},{},{}", xStart, yStart, zStart);
    }

    private void placeBase(Level level) {
        for (int x = xStart; x < xStart + width; x++) {
            for (int y = yStart; y < yStart + depth; y++) {
                for (int z = zStart; z < zStart + height; z++) {
                    var bp = new BlockPos(x, y, z);
                    level.setBlockAndUpdate(bp, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private void clearSpace(Level level) {
        for (int x = xStart; x < xStart + width; x++) {
            for (int y = yStart; y < yStart + depth; y++) {
                for (int z = zStart; z < zStart + baseHeight; z++) {
                    var bp = new BlockPos(x, y, z);
                    level.setBlockAndUpdate(bp, Blocks.BEDROCK.defaultBlockState());
                }
            }
        }
    }

}
