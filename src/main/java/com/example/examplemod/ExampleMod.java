package com.example.examplemod;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.InteractWith;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent.KeyboardKeyPressedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PlayMessages.SpawnEntity;

import org.slf4j.Logger;

import java.time.chrono.MinguoEra;
import java.util.stream.Collectors;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("tangia")
public class ExampleMod
{
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public ExampleMod()
    {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the enqueueIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        // Register the processIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        // some preinit code
        LOGGER.info("HELLO FROM PREINIT");
        LOGGER.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }

    private void enqueueIMC(final InterModEnqueueEvent event)
    {
        // Some example code to dispatch IMC to another mod
        InterModComms.sendTo("examplemod", "helloworld", () -> { LOGGER.info("Hello world from the MDK"); return "Hello world";});
    }

    private void processIMC(final InterModProcessEvent event)
    {
        // Some example code to receive and process InterModComms from other mods
        LOGGER.info("Got IMC {}", event.getIMCStream().
                map(m->m.messageSupplier().get()).
                collect(Collectors.toList()));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents
    {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent)
        {
            // Register a new block here
            LOGGER.info("HELLO from Register Block");
        }
    }


    @Mod.EventBusSubscriber(modid = "tangia", bus = Bus.FORGE, value = Dist.CLIENT)
    public class MyStaticClientOnlyEventHandler {
        @SubscribeEvent
        public static void onChatEvent(ClientChatEvent event) {
            LOGGER.info("Got chat message '{}'", event.getMessage().toString());
        }

        // @SubscribeEvent
        // public static void onKeyPressEvent(InputEvent.KeyInputEvent event) {
        //     LOGGER.info("pressed '{} - {}'", event.getKey(), event.getAction());
        //     if (event.getKey() == 71 && event.getAction() == 0) {
        //         // pressed g
        //         var instance = Minecraft.getInstance();
        //         var x = instance.player.getX();
        //         var y = instance.player.getY();
        //         var z = instance.player.getZ();
        //         Creeper creeper = new Creeper(EntityType.CREEPER, instance.level);
                
        //         LOGGER.info("spawning creeper at {}, {}, {}", x+1, y+1, z+1);

        //         // var creeper = ;
        //         // SpawnEntity()
        //     }
        // }
    }
    
    @Mod.EventBusSubscriber(modid = "tangia", bus = Bus.FORGE)
    public class MyStaticServerOnlyEventHandler {
        @SubscribeEvent
        public static void onInteractEvent(PlayerInteractEvent.RightClickItem event) {
            LOGGER.info("Got interaction event '{}'", event.toString());
            LOGGER.info("Got interaction item '{}'", event.getItemStack().toString());
            Creeper creeper = new Creeper(EntityType.CREEPER, event.getWorld());
            creeper.setPos(event.getPlayer().getX(), event.getPlayer().getY(), event.getPlayer().getZ());
            CompoundTag nbt = creeper.serializeNBT();
            Level world = event.getWorld();
            nbt.putBoolean("powered", true);
            nbt.putString("CustomName", "edrique");
            creeper.deserializeNBT(nbt);
            creeper.setCustomNameVisible(true);
            // creeper.setCustomName(Component.Serializer.fromJson("edriquer"));
            // LightningBolt lb = new LightningBolt(EntityType.LIGHTNING_BOLT, world);
            // lb.setPos(event.getPlayer().getX(), event.getPlayer().getY(), event.getPlayer().getZ());
            world.addFreshEntity(creeper);
            // world.addFreshEntity(lb);
        }
    }

    // Set the chest block, grab its block entity, fill its inventory
    // Listen to the tick event, count the amount of ticks up to 10 minutes, when it reaches the amount, do what you want - how to get server side events without player interaction
}
