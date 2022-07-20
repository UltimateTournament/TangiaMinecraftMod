package com.example.examplemod;

import com.ibm.icu.text.IDNA.Info;
import com.mojang.logging.LogUtils;

import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.slf4j.Logger;

import java.util.Random;
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
            Level world = event.getWorld();
            CompoundTag nbt = creeper.serializeNBT();
            nbt.putBoolean("powered", true);
            creeper.deserializeNBT(nbt);
            creeper.setNoAi(true);
            creeper.setCustomNameVisible(true);
            TextComponent name = new TextComponent("edrique");
            creeper.setCustomName(name);
            creeper.addTag("test tag");
            // LightningBolt lb = new LightningBolt(EntityType.LIGHTNING_BOLT, world);
            // lb.setPos(event.getPlayer().getX(), event.getPlayer().getY(), event.getPlayer().getZ());
            world.addFreshEntity(creeper);

            // add item to inventory
            Item item = Items.TOTEM_OF_UNDYING;
            ItemStack totem = new ItemStack(item, 1);
            totem.setHoverName(name);

            ItemStack sword = new ItemStack(Items.NETHERITE_HOE, 1);
            sword.enchant(Enchantments.SHARPNESS, 5);
            sword.setHoverName(name);
            event.getPlayer().getInventory().add(totem);
            event.getPlayer().getInventory().add(sword);

            BlockPos bp = new BlockPos(event.getPlayer().getX(), event.getPlayer().getY(), event.getPlayer().getZ()+1);
            world.setBlockAndUpdate(bp, Blocks.CHEST.defaultBlockState());
            ChestBlockEntity cbe = new ChestBlockEntity(bp, Blocks.CHEST.defaultBlockState());
            cbe.setCustomName(new TextComponent("pepechest"));
            for (int slot = 0; slot < 5; slot++) {
                Item iteme = Items.TOTEM_OF_UNDYING;
                ItemStack toteme = new ItemStack(iteme, 1);
                toteme.setHoverName(new TextComponent("hehe"));
                cbe.setItem(slot, toteme);
            }
            world.setBlockEntity(cbe);
            // world.addFreshEntity(lb);

            // spawn lightning where player is looking
            // LightningBolt entityToSpawn = EntityType.LIGHTNING_BOLT.create(world);
			// entityToSpawn.moveTo(Vec3.atBottomCenterOf(new BlockPos(
			// 		event.getPlayer().level.clip(new ClipContext(event.getPlayer().getEyePosition(1f), event.getPlayer().getEyePosition(1f).add(event.getPlayer().getViewVector(1f).scale(100)),
			// 				ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, event.getPlayer())).getBlockPos().getX(),
			// 		event.getPlayer().level.clip(new ClipContext(event.getPlayer().getEyePosition(1f), event.getPlayer().getEyePosition(1f).add(event.getPlayer().getViewVector(1f).scale(100)),
			// 				ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, event.getPlayer())).getBlockPos().getY(),
			// 		event.getPlayer().level.clip(new ClipContext(event.getPlayer().getEyePosition(1f), event.getPlayer().getEyePosition(1f).add(event.getPlayer().getViewVector(1f).scale(100)),
			// 				ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, event.getPlayer())).getBlockPos().getZ())));
			// entityToSpawn.setVisualOnly(false);
			// world.addFreshEntity(entityToSpawn);
            
            // spawn ghast where player is looking
            Ghast entityToSpawn = EntityType.GHAST.create(world);
			entityToSpawn.moveTo(Vec3.atBottomCenterOf(new BlockPos(
					event.getPlayer().level.clip(new ClipContext(event.getPlayer().getEyePosition(1f), event.getPlayer().getEyePosition(1f).add(event.getPlayer().getViewVector(1f).scale(100)),
							ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, event.getPlayer())).getBlockPos().getX(),
					event.getPlayer().level.clip(new ClipContext(event.getPlayer().getEyePosition(1f), event.getPlayer().getEyePosition(1f).add(event.getPlayer().getViewVector(1f).scale(100)),
							ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, event.getPlayer())).getBlockPos().getY(),
					event.getPlayer().level.clip(new ClipContext(event.getPlayer().getEyePosition(1f), event.getPlayer().getEyePosition(1f).add(event.getPlayer().getViewVector(1f).scale(100)),
							ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, event.getPlayer())).getBlockPos().getZ())));
			world.addFreshEntity(entityToSpawn);
        }

        @SubscribeEvent
        public static void onMobDrops(LivingDropsEvent event) {
            LOGGER.info("------ GOT LIVING DROP EVENT");
            Entity entity = event.getEntity();
            LOGGER.info("GOT TAGS {}", entity.getTags());
        }

        @SubscribeEvent
        public static void onLeverEvent(PlayerInteractEvent.RightClickBlock event) {
            BlockPos bp = event.getPos();
            BlockEntity b = event.getWorld().getBlockEntity(bp);
            if (b instanceof ChestBlockEntity) {
                ChestBlockEntity cbe = (ChestBlockEntity) b;
                var textcomp = cbe.getCustomName();
                if (textcomp != null) {
                    String thestring = textcomp.getString();
                    LOGGER.info("INTERACTED WITH CHEST BLOCK - '{}'", thestring);
                    if (thestring.equals("pepechest")) {
                        LOGGER.info("interactied with pepechest");
                        Random rand = new Random();
                        int randomInt = rand.nextInt(2);
                        LOGGER.info("GOT NUMBER {}", randomInt);
                        if (randomInt == 1) {
                            // Spawn creeper
                            LOGGER.info("SPAWNING CREEPER HAHA!");
                            Level world = event.getWorld();
                            // Remove chest from position
                            // world.removeBlockEntity(bp); // makes an empty default chest for some reason
                            world.setBlock(bp, Blocks.AIR.defaultBlockState(), Blocks.AIR.getId(Blocks.AIR.defaultBlockState())); // breaks the chest

                            // Spawn creeper
                            Creeper creeper = new Creeper(EntityType.CREEPER, event.getWorld());
                            creeper.setPos(event.getPos().getX(), event.getPos().getY()+2, event.getPos().getZ());
                            // CompoundTag nbt = creeper.serializeNBT();
                            // nbt.putBoolean("powered", true);
                            // creeper.deserializeNBT(nbt);
                            creeper.setCustomNameVisible(true);
                            TextComponent name = new TextComponent("Get naenae'd");
                            creeper.setCustomName(name);
                            world.addFreshEntity(creeper);
                        }
                    }
                    // see if we spawn a creeper instead
                }
            }
        }
    }

    // Set the chest block, grab its block entity, fill its inventory
    // Listen to the tick event, count the amount of ticks up to 10 minutes, when it reaches the amount, do what you want - how to get server side events without player interaction
}
