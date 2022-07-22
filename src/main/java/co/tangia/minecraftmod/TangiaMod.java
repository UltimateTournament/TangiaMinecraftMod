
package co.tangia.minecraftmod;

import com.google.gson.Gson;
import co.tangia.minecraftmod.chatcommands.LoginCommand;
import co.tangia.minecraftmod.chatcommands.LogoutCommand;
import co.tangia.sdk.EventResult;
import co.tangia.sdk.InvalidLoginException;
import co.tangia.sdk.TangiaSDK;
import com.mojang.logging.LogUtils;
import dev.failsafe.RetryPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.command.ConfigCommand;
import org.slf4j.Logger;
import retrofit2.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("tangia")
public class TangiaMod {
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<Integer, TangiaSDK> playerSDKs = new HashMap<>();

    public TangiaMod() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the enqueueIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        // Register the processIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        RetryPolicy<Response<Object>> retryPolicy = RetryPolicy.ofDefaults();
    }

    private void setup(final FMLCommonSetupEvent event) {
        // some preinit code
        LOGGER.info("HELLO FROM PREINIT");
        LOGGER.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {
        // Some example code to dispatch IMC to another mod
        InterModComms.sendTo("examplemod", "helloworld", () -> {
            LOGGER.info("Hello world from the MDK");
            return "Hello world";
        });
    }

    private void processIMC(final InterModProcessEvent event) {
        // Some example code to receive and process InterModComms from other mods
        LOGGER.info("Got IMC {}", event.getIMCStream().
            map(m -> m.messageSupplier().get()).
            collect(Collectors.toList()));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    @SubscribeEvent
    public void onCommandsRegister(RegisterCommandsEvent event) {
        new LoginCommand(this).register(event.getDispatcher());
        new LogoutCommand(this).register(event.getDispatcher());
        ConfigCommand.register(event.getDispatcher());
    }

    public void login(Player player, String code) throws InvalidLoginException, IOException {
        var gameId = System.getenv("TANGIA_GAME_ID");
        if (gameId == null) {
            LOGGER.warn("TANGIA_GAME_ID not set");
            throw new InvalidLoginException();
        }
        var id = player.getId();
        var sdk = new TangiaSDK(gameId, "0.0.1", "STAGING".equals(System.getenv("TANGIA_ENV")) ? TangiaSDK.STAGING_URL : TangiaSDK.PROD_URL);
        sdk.login(code);
        synchronized (playerSDKs) {
            if (playerSDKs.get(id) != null)
                playerSDKs.get(id).stopEventPolling();
            playerSDKs.put(id, sdk);
        }
        sdk.startEventPolling();
    }

    public void logout(Player player) {
        synchronized (playerSDKs) {
            var id = player.getId();
            var sdk = playerSDKs.get(id);
            if (sdk != null) {
                sdk.stopEventPolling();
                playerSDKs.remove(id);
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.WorldTickEvent event) {
        if (event.side != LogicalSide.SERVER)
            return;
        for (var sdkEntry : playerSDKs.entrySet()) {
            var sdk = sdkEntry.getValue();
            if (sdk == null)
                continue;
            var interaction = sdk.popEventQueue();
            if (interaction == null)
                continue;
            LOGGER.info("Got event '{}' for '{}'", interaction.InteractionID, sdkEntry.getKey());

            Gson gson = new Gson();
            InspectMetadata inspect = gson.fromJson(interaction.Metadata, InspectMetadata.class);
            
            if (inspect.items != null) {
                for (var item: inspect.items) {
                    for (var player: event.world.players()) {
                        if (player.getId() == sdkEntry.getKey()) {
                            ItemStack is = item.getItemStack(interaction.BuyerName);
                            // Check if dropping or adding to inventory
                            if (item.drop != null && item.drop) {
                                ItemEntity itement = new ItemEntity(event.world, player.getX(), player.getY(), player.getZ(), is);
                                event.world.addFreshEntity(itement);
                            } else {
                                player.getInventory().add(is);
                            }
                        }
                    }
                }
            }
            if (inspect.commands != null) {
                for (var command: inspect.commands) {
                    for (var player: event.world.players()) {
                        if (player.getId() == sdkEntry.getKey()) {
                            // Run the command
                            event.world.getServer().getCommands().performCommand(player.createCommandSourceStack().withSuppressedOutput().withPermission(4), command.getMessage(player.getName().getContents(), interaction.BuyerName));
                        }
                    }
                }
            }
            if (inspect.chests != null) {
                for (var chest: inspect.chests) {
                    for (var player: event.world.players()) {
                        if (player.getId() == sdkEntry.getKey()) {
                            // Spawn the chest at the player
                            chest.setBlockEntity(event.world, player.getX(), player.getY(), player.getZ(), interaction.BuyerName);
                        }
                    }
                }
            }
            if (inspect.messages != null) {
                for (var message: inspect.messages) {
                    if (message.toAllPlayers != null && message.toAllPlayers) {
                        for (var player: event.world.players()) {
                            player.sendMessage(new TextComponent(message.message), UUID.randomUUID());
                        }
    
                    }
                    for (var player: event.world.players()) {
                        if (player.getId() == sdkEntry.getKey()) {
                            // Spawn the chest at the player
                            player.sendMessage(new TextComponent(message.message), UUID.randomUUID());
                        }
                    }
                }
            }
            if (inspect.mobs != null) {
                for (var mobComponent: inspect.mobs) {
                    for (var player: event.world.players()) {
                        if (player.getId() == sdkEntry.getKey()) {
                            Mob mob = mobComponent.getMob(event.world, interaction.BuyerName);
                            mob.setPos(player.getX(), player.getY(), player.getZ());
                            event.world.addFreshEntity(mob);
                        }
                    }
                }
            }
            if (inspect.sounds != null) {
                for (var soundComponent: inspect.sounds) {
                    for (var player: event.world.players()) {
                        if (player.getId() == sdkEntry.getKey()) {
                            BlockPos bp = new BlockPos(player.getX(), player.getY(), player.getZ());
                            if (soundComponent.delaySeconds != null && soundComponent.delaySeconds > 0) {
                                ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
                                exec.schedule(new Runnable() {
                                public void run() {
                                    event.world.playSound(player, bp, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(soundComponent.soundID)), SoundSource.PLAYERS, 1f, 1f);
                                }  
                                }, 1, TimeUnit.SECONDS);
                            } else {
                                event.world.playSound(player, bp, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(soundComponent.soundID)), SoundSource.PLAYERS, 1f, 1f);
                            }
                        }
                    }
                }
            }
            if (inspect.statuses != null) {
                for (var statusComponent: inspect.statuses) {
                    for (var player: event.world.players()) {
                        if (player.getId() == sdkEntry.getKey()) {
                            MobEffectInstance mei = new MobEffectInstance(ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(statusComponent.statusID)), statusComponent.tickDuration);
                            player.addEffect(mei);
                        }
                    }
                }
            }
            
            // Ack the event
            sdk.ackEventAsync(new EventResult(interaction.EventID, true, null));
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        // TODO figure out if we get a stable identity for players so we can keep their session when they come back
        logout(event.getPlayer());
    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
            // Register a new block here
            LOGGER.info("HELLO from Register Block");
        }
    }


    @Mod.EventBusSubscriber(modid = "tangia", bus = Bus.FORGE, value = Dist.CLIENT)
    public static class MyStaticClientOnlyEventHandler {
        @SubscribeEvent
        public static void onChatEvent(ClientChatEvent event) {
            LOGGER.info("Got chat message '{}'", event.getMessage());
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
    public static class MyStaticServerOnlyEventHandler {
        @SubscribeEvent
        public static void onInteractEvent(PlayerInteractEvent.RightClickItem event) {
            LOGGER.info("Got interaction event '{}'", event.toString());
            LOGGER.info("Got interaction item '{}'", event.getItemStack());
            var gameId = System.getenv("DEBUG");
            if (gameId == null) {
                return;
            }
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

            BlockPos bp = new BlockPos(event.getPlayer().getX(), event.getPlayer().getY(), event.getPlayer().getZ() + 1);
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
            // Ghast entityToSpawn = EntityType.GHAST.create(world);
            Entity entityToSpawn = ForgeRegistries.ENTITIES.getValue(new ResourceLocation("donkey")).create(world);
            entityToSpawn.moveTo(Vec3.atBottomCenterOf(new BlockPos(
                event.getPlayer().level.clip(new ClipContext(event.getPlayer().getEyePosition(1f), event.getPlayer().getEyePosition(1f).add(event.getPlayer().getViewVector(1f).scale(100)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, event.getPlayer())).getBlockPos().getX(),
                event.getPlayer().level.clip(new ClipContext(event.getPlayer().getEyePosition(1f), event.getPlayer().getEyePosition(1f).add(event.getPlayer().getViewVector(1f).scale(100)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, event.getPlayer())).getBlockPos().getY(),
                event.getPlayer().level.clip(new ClipContext(event.getPlayer().getEyePosition(1f), event.getPlayer().getEyePosition(1f).add(event.getPlayer().getViewVector(1f).scale(100)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, event.getPlayer())).getBlockPos().getZ())));
            if (entityToSpawn instanceof Mob mob) {
                mob.setNoAi(true);
                world.addFreshEntity(mob);
            }


            // apply a status effect
            MobEffectInstance mei = new MobEffectInstance(ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("strength")), 200);
            // MobEffectInstance mei = new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100);
            event.getPlayer().addEffect(mei);
            // event.getPlayer().getName().getContents();

            event.getPlayer().sendMessage(new TextComponent("personal message"), UUID.randomUUID());

            // drop an item
            ItemStack mossyblock = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft:mossy_stone_brick_stairs")), 1);
            mossyblock.enchant(ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation("protection")), 3);
            ItemEntity itement = new ItemEntity(world, event.getPlayer().getX(), event.getPlayer().getY(), event.getPlayer().getZ(), mossyblock);
            world.addFreshEntity(itement);

            String testobj = "{\"chestName\":\"hello\",\"items\":[{\"itemID\":\"hey\"}]}";

            Gson gson = new Gson();
            ChestComponent actualObj = gson.fromJson(testobj, ChestComponent.class);
            LOGGER.info("actual obj - {}", gson.toJson(actualObj));

            // play a sound
            world.playSound(event.getPlayer(), event.getPos(), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.lightning_bolt.thunder")), SoundSource.AMBIENT, 1f, 1f);
            world.playSound(event.getPlayer(), event.getPos(), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.lightning_bolt.impact")), SoundSource.AMBIENT, 1f, 1f);

            // Play a delayed sound
            ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
            exec.schedule(new Runnable() {
              public void run() {
                  world.playSound(event.getPlayer(), event.getPos(), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.creeper.primed")), SoundSource.AMBIENT, 1f, 1f);
                  world.playSound(event.getPlayer(), event.getPos(), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.creeper.primed")), SoundSource.AMBIENT, 1f, 1f);
              }  
            }, 1, TimeUnit.SECONDS);
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
            if (b instanceof ChestBlockEntity cbe) {
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
                            world.setBlock(bp, Blocks.AIR.defaultBlockState(), Block.getId(Blocks.AIR.defaultBlockState())); // breaks the chest

                            // Spawn creeper
                            Creeper creeper = new Creeper(EntityType.CREEPER, event.getWorld());
                            creeper.setPos(event.getPos().getX(), event.getPos().getY() + 2, event.getPos().getZ());
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

}
