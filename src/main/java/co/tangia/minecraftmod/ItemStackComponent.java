package co.tangia.minecraftmod;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
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

  public ItemStack getItemStack(String displayName, HolderLookup.Provider lookupProvider) {
    var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(this.itemID));
    var itemTag = new CompoundTag();
    var itemStack = new ItemStack(item, this.stackSize);
    if (this.enchantments != null) {
      for (EnchantmentComponent enchantment : this.enchantments) {
        var ench = enchantment.getEnchantment(lookupProvider);
        if (ench != null) {
          itemStack.enchant(ench, enchantment.level);
        }
      }
    }
    // Apply nbts
    if (this.nbtStrings != null) {
      for (Map.Entry<String, String> entry : this.nbtStrings.entrySet()) {
        itemTag.putString(entry.getKey(), entry.getValue());
      }
    }
    if (this.nbtBools != null) {
      for (Map.Entry<String, Boolean> entry : this.nbtBools.entrySet()) {
        itemTag.putBoolean(entry.getKey(), entry.getValue());
      }
    }
    if (this.nbtInts != null) {
      for (Map.Entry<String, Integer> entry : this.nbtInts.entrySet()) {
        itemTag.putInt(entry.getKey(), entry.getValue());
      }
    }
    // could also be ENTITY_DATA
    itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(itemTag));
    if (this.hoverName != null) {
      var name = this.hoverName;
      if (displayName != null) {
        name = name.replaceAll("\\$DISPLAYNAME", displayName);
      }
      itemStack.set(DataComponents.CUSTOM_NAME, MutableComponent.create(new PlainTextContents.LiteralContents(name)));
    }
    return itemStack;
  }

}
