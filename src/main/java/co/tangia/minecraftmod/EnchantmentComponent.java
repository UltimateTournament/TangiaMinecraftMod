package co.tangia.minecraftmod;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

public class EnchantmentComponent {
  public String enchantmentID;
  public int level;

  public Holder<Enchantment> getEnchantment(HolderLookup.Provider lookupProvider) {
    if (this.enchantmentID != null) {
      var echKey = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(this.enchantmentID));
      var ench = lookupProvider.get(echKey);
      return ench.get();
    } else {
      return null;
    }
  }
}
