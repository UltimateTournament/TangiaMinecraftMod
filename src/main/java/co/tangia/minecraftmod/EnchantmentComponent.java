package co.tangia.minecraftmod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

public class EnchantmentComponent {
  public String enchantmentID;
  public int level;

  public Enchantment getEnchantment() {
    if (this.enchantmentID != null) {
      return ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(this.enchantmentID));
    } else {
      return null;
    }
  }
}
