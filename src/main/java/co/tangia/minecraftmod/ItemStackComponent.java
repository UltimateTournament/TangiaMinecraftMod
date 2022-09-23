package co.tangia.minecraftmod;

import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemStackComponent {
  public String itemID;
  public int stackSize;
  public EnchantmentComponent[] enchantments;
  public String hoverName;
  public Boolean drop;
  public int weight;
  public Map<String, Boolean> nbtBools;
  public Map<String, String> nbtStrings;
  public Map<String, Integer> nbtInts;

  public ItemStack getItemStack(String displayName) {
    ItemStack is = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(this.itemID)), this.stackSize);
    if (this.enchantments != null) {
      for (int i = 0; i < this.enchantments.length; i++) {
        Enchantment ench = this.enchantments[i].getEnchantment();
        if (ench != null) {
          is.enchant(ench, this.enchantments[i].level);
        }
      }
    }
    // Apply nbts
    if (this.nbtStrings != null) {
      for(Map.Entry<String,String> entry: this.nbtStrings.entrySet()) {
        var tag = is.getTag();
        if (tag == null) {
          tag = new CompoundTag();
        }
        tag.putString(entry.getKey(), entry.getValue());
        is.setTag(tag);
      }
    }
    if (this.nbtBools != null) {
      for(Map.Entry<String,Boolean> entry: this.nbtBools.entrySet()) {
          var tag = is.getTag();
          if (tag == null) {
            tag = new CompoundTag();
          }
          tag.putBoolean(entry.getKey(), entry.getValue());
          is.setTag(tag);
      }
    }
    if (this.nbtInts != null) {
      for(Map.Entry<String, Integer> entry: this.nbtInts.entrySet()) {
          var tag = is.getTag();
          if (tag == null) {
            tag = new CompoundTag();
          }
          tag.putInt(entry.getKey(), entry.getValue());
          is.setTag(tag);
        }
    }
    if (this.hoverName != null && displayName != null) {
      is.setHoverName(new TextComponent(this.hoverName.replaceAll("\\$DISPLAYNAME", displayName)));
    } else if (this.hoverName != null) {
      is.setHoverName(new TextComponent(this.hoverName));
    }
    return is;
  }
}
