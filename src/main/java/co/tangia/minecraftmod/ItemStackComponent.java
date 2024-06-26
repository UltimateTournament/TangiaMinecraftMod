package co.tangia.minecraftmod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

public class ItemStackComponent {
  public static class Data {
    public String itemID;
    public int stackSize;
    public EnchantmentComponent[] enchantments;
    public String hoverName;
    public Boolean drop;
    public int weight;
    public Map<String, Boolean> nbtBools;
    public Map<String, String> nbtStrings;
    public Map<String, Integer> nbtInts;
  }
  public String itemID;
  public int stackSize;
  public EnchantmentComponent[] enchantments;
  public String hoverName;
  public Boolean drop;
  public int weight;
  public Map<String, Boolean> nbtBools;
  public Map<String, String> nbtStrings;
  public Map<String, Integer> nbtInts;

  public ItemStackComponent(Data data) {
    this.itemID = data.itemID;
    this.stackSize = data.stackSize;
    this.enchantments = data.enchantments;
    this.hoverName = data.hoverName;
    this.drop = data.drop;
    this.weight = data.weight;
    this.nbtBools = data.nbtBools;
    this.nbtStrings = data.nbtStrings;
    this.nbtInts = data.nbtInts;
  }

  public ItemStack getItemStack(String displayName) {
    ItemStack is = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(this.itemID)), this.stackSize);
    if (this.enchantments != null) {
      for (EnchantmentComponent enchantment : this.enchantments) {
        Enchantment ench = enchantment.getEnchantment();
        if (ench != null) {
          is.enchant(ench, enchantment.level);
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
      is.setHoverName(MutableComponent.create(new LiteralContents(this.hoverName.replaceAll("\\$DISPLAYNAME", displayName))));
    } else if (this.hoverName != null) {
      is.setHoverName(MutableComponent.create(new LiteralContents(this.hoverName)));
    }
    return is;
  }
}
