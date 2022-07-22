package co.tangia.minecraftmod;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemStackComponent {
  public String itemID;
  public int stackSize;
  public EnchantmentComponent[] enchantments;
  public String hoverName;
  public Boolean drop;

  public ItemStack getItemStack(String displayName) {
    ItemStack is = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(this.itemID)), this.stackSize);
    if (this.enchantments != null) {
      for (int i = 0; i < this.enchantments.length; i++) {
        is.enchant(this.enchantments[i].getEnchantment(), this.enchantments[i].level);
      }
    }
    if (this.hoverName != null && displayName != null) {
      is.setHoverName(new TextComponent(this.hoverName.replaceAll("$DISPLAYNAME", displayName)));
    } else if (this.hoverName != null) {
      is.setHoverName(new TextComponent(this.hoverName));
    }
    return is;
  }
}
