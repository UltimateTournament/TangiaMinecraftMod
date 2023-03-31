package co.tangia.minecraftmod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

public class ChestComponent {
  public String chestName;
  public ItemStackComponent[] items;

  public ChestBlockEntity setBlockEntity(Level level, double x, double y, double z, String displayName) {
    BlockPos bp = new BlockPos((int) x, (int) y, (int) z);
    level.setBlockAndUpdate(bp, Blocks.CHEST.defaultBlockState());
    ChestBlockEntity cbe = new ChestBlockEntity(bp, Blocks.CHEST.defaultBlockState());
    if (this.chestName != null) {
      if (displayName != null) {
        cbe.setCustomName(MutableComponent.create(new LiteralContents(this.chestName.replaceAll("\\$DISPLAYNAME", displayName))));
      } else {
        cbe.setCustomName(MutableComponent.create(new LiteralContents(this.chestName)));
      }
    }
    if (this.items != null) {
      for (int i = 0; i < this.items.length; i++) {
        cbe.setItem(i, this.items[i].getItemStack(null));
      }
    }
    level.setBlockEntity(cbe);
    return cbe;
  }
}
