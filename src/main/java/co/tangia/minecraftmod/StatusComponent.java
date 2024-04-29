package co.tangia.minecraftmod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.ForgeRegistries;

public class StatusComponent {
  public static class Data {
    public String statusID;
    public int tickDuration;
  }
  public String statusID;
  public int tickDuration;

  public MobEffectInstance getMobEffectInstance() {
    return new MobEffectInstance(ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(this.statusID)), this.tickDuration);
  }
}
