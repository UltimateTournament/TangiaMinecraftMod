package co.tangia.minecraftmod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

public class EnchantmentComponent {
  public String enchantentID;
  public int level;

  public Enchantment getEnchantment() {
    return ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(this.enchantentID));
  }
}
