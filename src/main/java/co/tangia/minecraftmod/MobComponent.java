package co.tangia.minecraftmod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public class MobComponent {
  public String entityID;

  public Entity getEntity(Level level) {
    return ForgeRegistries.ENTITIES.getValue(new ResourceLocation(this.entityID)).create(level);
  }
}
