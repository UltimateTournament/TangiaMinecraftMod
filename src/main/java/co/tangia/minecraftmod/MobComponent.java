package co.tangia.minecraftmod;

import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public class MobComponent {
  public static class Data {
    public String entityID;
    public Map<String, Boolean> nbtBools;
    public Map<String, String> nbtStrings;
    public Map<String, Integer> nbtInts;
    public String customName;
    public Boolean noAI;
  }
  public String entityID;
  public Map<String, Boolean> nbtBools;
  public Map<String, String> nbtStrings;
  public Map<String, Integer> nbtInts;
  public String customName;
  public Boolean noAI;

  public MobComponent(Data data) {
    this.entityID = data.entityID;
    this.nbtBools = data.nbtBools;
    this.nbtStrings = data.nbtStrings;
    this.nbtInts = data.nbtInts;
    this.customName = data.customName;
    this.noAI = data.noAI;
  }

  public Mob getMob(Level level, String displayName) {
    var lookupProvider = level.registryAccess();
    var entityType = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(this.entityID));
    Entity entity = entityType.create(level, EntitySpawnReason.MOB_SUMMONED);
    if (entity instanceof Mob mob) {
      // Apply nbts
      if (this.nbtStrings != null) {
        for(Map.Entry<String,String> entry: this.nbtStrings.entrySet()) {
          CompoundTag nbt = mob.serializeNBT(lookupProvider);
          nbt.putString(entry.getKey(), entry.getValue());
          mob.deserializeNBT(lookupProvider, nbt);
        }
      }
      if (this.nbtBools != null) {
        for(Map.Entry<String,Boolean> entry: this.nbtBools.entrySet()) {
          CompoundTag nbt = mob.serializeNBT(lookupProvider);
          nbt.putBoolean(entry.getKey(), entry.getValue());
          mob.deserializeNBT(lookupProvider, nbt);
        }
      }
      if (this.nbtInts != null) {
        for(Map.Entry<String, Integer> entry: this.nbtInts.entrySet()) {
          CompoundTag nbt = mob.serializeNBT(lookupProvider);
          nbt.putInt(entry.getKey(), entry.getValue());
          mob.deserializeNBT(lookupProvider, nbt);
        }
      }
  
      if (this.customName != null && !this.customName.isEmpty()) {
        mob.setCustomNameVisible(true);
        if (displayName != null) {
          mob.setCustomName(MutableComponent.create(new PlainTextContents.LiteralContents(this.customName.replaceAll("\\$DISPLAYNAME", displayName))));
        } else {
          mob.setCustomName(MutableComponent.create(new PlainTextContents.LiteralContents(this.customName)));
        }
      }
  
      if (this.noAI != null && this.noAI) {
        mob.setNoAi(true);
      }
  
      return mob;
    }
    throw new Error("entity not a mob!");

  }
}
