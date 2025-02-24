package co.tangia.minecraftmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.Arrays;

public class ChestComponent {
  public static class Data {
    public String chestName;
    public ItemStackComponent.Data[] items;
  }

  public String chestName;
  public ItemStackComponent[] items;

  public ChestComponent(Data data) {
    this.chestName = data.chestName;
    this.items = Arrays.stream(data.items).map(ItemStackComponent::new).toArray(ItemStackComponent[]::new);
  }

  public ChestBlockEntity setBlockEntity(Level level, double x, double y, double z, String displayName, HolderLookup.Provider lookupProvider) {
    BlockPos bp = new BlockPos((int) x, (int) y, (int) z);
    level.setBlockAndUpdate(bp, Blocks.CHEST.defaultBlockState());
    ChestBlockEntity cbe = new ChestBlockEntity(bp, Blocks.CHEST.defaultBlockState());
    if (this.chestName != null) {
      var name = this.chestName;
      if (displayName != null) {
        name = name.replaceAll("\\$DISPLAYNAME", displayName);
      }
      cbe.setComponents(DataComponentMap.builder().set(
          DataComponents.CUSTOM_NAME, MutableComponent.create(new PlainTextContents.LiteralContents(name))
      ).build());
    }
    if (this.items != null) {
      for (int i = 0; i < this.items.length; i++) {
        cbe.setItem(i, this.items[i].getItemStack(null, lookupProvider));
      }
    }
    level.setBlockEntity(cbe);
    return cbe;
  }
}
