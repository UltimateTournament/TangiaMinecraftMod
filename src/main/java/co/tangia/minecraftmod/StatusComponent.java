package co.tangia.minecraftmod;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
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
    MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.parse(this.statusID));
    return new MobEffectInstance(Holder.direct(effect), this.tickDuration);
  }
}
