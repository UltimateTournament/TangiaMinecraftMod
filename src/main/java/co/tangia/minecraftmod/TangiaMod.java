
package co.tangia.minecraftmod;

import co.tangia.minecraftmod.chatcommands.LoginCommand;
import co.tangia.minecraftmod.chatcommands.LogoutCommand;
import co.tangia.minecraftmod.chatcommands.TowerCommand;
import co.tangia.sdk.EventResult;
import co.tangia.sdk.InteractionEvent;
import co.tangia.sdk.InvalidLoginException;
import co.tangia.sdk.TangiaSDK;
import com.google.gson.Gson;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.WhitelistCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
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
    private final Gson gson = new Gson();
    private final Map<UUID, TangiaSDK> playerSDKs = new HashMap<>();
    private final String gameVersion = "0.0.1";
    private final String tangiaUrl = "STAGING".equals(System.getenv("TANGIA_ENV")) ? TangiaSDK.STAGING_URL : TangiaSDK.PROD_URL;
    private final String gameId = System.getenv("TANGIA_GAME_ID");

    public TangiaMod() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the enqueueIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        // Register the processIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
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
        LOGGER.info("Tangia Mod starting");
        ModPersistence.load();
    }

    @SubscribeEvent
    public void onCommandsRegister(RegisterCommandsEvent event) {
        ConfigCommand.register(event.getDispatcher());

        new LoginCommand(this).register(event.getDispatcher());
        new LogoutCommand(this).register(event.getDispatcher());
        String features = System.getenv("TANGIA_MOD_FEATURES");
        if (features == null) {
            features = "";
        }
        if (features.contains("TOWER")) {
            new TowerCommand().register(event.getDispatcher());
        }
    }

    public void login(Player player, String code) throws InvalidLoginException, IOException {
        if (gameId == null) {
            LOGGER.warn("TANGIA_GAME_ID not set");
            throw new InvalidLoginException();
        }
        var id = player.getUUID();
        var sdk = new TangiaSDK(gameId, gameVersion, tangiaUrl);
        sdk.login(code);
        synchronized (playerSDKs) {
            if (playerSDKs.get(id) != null)
                playerSDKs.get(id).stopEventPolling();
            playerSDKs.put(id, sdk);
        }
        sdk.startEventPolling();
        ModPersistence.data.sessions().put(id, new ModPersistenceData.PlayerSession(sdk.getSessionKey()));
        ModPersistence.store();
    }

    public void logout(Player player, boolean removeSession) {
        synchronized (playerSDKs) {
            var id = player.getUUID();
            var sdk = playerSDKs.get(id);
            if (sdk != null) {
                sdk.stopEventPolling();
                playerSDKs.remove(id);
            }
            if (removeSession) {
                ModPersistence.data.sessions().remove(player.getUUID());
                ModPersistence.store();
            }
        }
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        LOGGER.info("Player with UUID {} joined", player.getUUID());
        if (playerSDKs.get(player.getUUID()) != null) {
            return;
        }
        var session = ModPersistence.data.sessions().get(player.getUUID());
        if (session != null) {
            var sdk = new TangiaSDK(gameId, gameVersion, tangiaUrl);
            sdk.setSessionKey(session.sessionToken());
            playerSDKs.put(player.getUUID(), sdk);
            sdk.startEventPolling();
            LOGGER.info("Tangia session restored for Player with UUID {}", player.getUUID());
            player.sendMessage(new TextComponent("We've logged you back into your Tangia account"), UUID.randomUUID());
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        // TODO figure out if we get a stable identity for players so we can keep their session when they come back
        logout(event.getPlayer(), false);
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
            LOGGER.info("Got event '{}' for '{}' with metadata '{}'", interaction.InteractionID, sdkEntry.getKey(), interaction.Metadata);
            var player = event.world.getServer().getPlayerList().getPlayer(sdkEntry.getKey());
            if (player == null) {
                LOGGER.warn("Interaction for unavailable player");
                sdk.ackEventAsync(new EventResult(interaction.EventID, false, "player not in game"));
                continue;
            }
            try {
                handlePlayerInteraction(event, interaction, player);
                sdk.ackEventAsync(new EventResult(interaction.EventID, true, null));
            } catch (Exception e) {
                LOGGER.error("exception in interaction processing", e);
                sdk.ackEventAsync(new EventResult(interaction.EventID, false, "exception"));
            }

        }
    }

    private void handlePlayerInteraction(TickEvent.WorldTickEvent event, InteractionEvent interaction, ServerPlayer player) {
        InspectMetadata inspect = gson.fromJson(interaction.Metadata, InspectMetadata.class);

        if (inspect.items != null) {
            for (var item : inspect.items) {
                ItemStack is = item.getItemStack(interaction.BuyerName);
                // Check if dropping or adding to inventory
                if (item.drop != null && item.drop) {
                    ItemEntity itement = new ItemEntity(event.world, player.getX(), player.getY(), player.getZ(), is);
                    event.world.addFreshEntity(itement);
                } else {
                    player.getInventory().add(is);
                }
                // DEBUG SHULKER
                // ItemStack shulk = new ItemStack(Items.SHULKER_BOX, 1);
                // NonNullList<ItemStack> shulkerItems = NonNullList.withSize(27, ItemStack.EMPTY);
                // CompoundTag shulkNbt = new CompoundTag();
                // CompoundTag shulkItems = new CompoundTag();
                // ItemStack axe = new ItemStack(Items.NETHERITE_AXE, 1);
                // axe.enchant(Enchantments.SHARPNESS, 5);
                // shulkerItems.set(1, axe);
                // ContainerHelper.saveAllItems(shulkItems, shulkerItems);
                // shulkNbt.put("BlockEntityTag", shulkItems);
                // CompoundTag displayTag = new CompoundTag();
                // displayTag.putString("Name", Component.Serializer.toJson(MutableComponent.create(new LiteralContents("yeye"))));
                // shulkNbt.put("display", displayTag);
                // shulk.setTag(shulkNbt);
                // player.getInventory().add(shulk);
            }
        }
        if (inspect.commands != null) {
            for (var command : inspect.commands) {
                // Run the command
                var cmd = new CommandComponent(player.getName().getString(), interaction.BuyerName, player.getUUID(), command.command, event.world.dayTime(), command.delayTicks);
                cmd.init();
            }
        }
        if (inspect.chests != null) {
            for (var chest : inspect.chests) {
                // Spawn the chest at the player
                chest.setBlockEntity(event.world, player.getX(), player.getY(), player.getZ(), interaction.BuyerName);
            }
        }
        if (inspect.kits != null) {
            for (var kit : inspect.kits) {
                var totalWeight = 0;
                for (var item : kit.items) {
                    // Add up the weights
                    totalWeight += item.weight;
                }
                // Keep spawning items
                Random rand = new Random();
                var iter = 0;
                for (int i = 0; i < 10000 && iter < kit.numItems; i++) {
                    // Iterate over items trying to spawn them, try max 1k times
                    int randomInt = rand.nextInt(totalWeight);
                    var currentItem = kit.items[i % kit.items.length];
                    if (randomInt <= currentItem.weight) {
                        // Spawn the item
                        ItemStack itemStack = currentItem.getItemStack(null);
                        ItemEntity itemEntity = new ItemEntity(event.world, player.getX(), player.getY(), player.getZ(), itemStack);
                        event.world.addFreshEntity(itemEntity);
                        iter++;
                    }
                }
            }
        }
        if (inspect.messages != null) {
            for (var message : inspect.messages) {
                if (interaction.BuyerName == null || interaction.BuyerName == "") {
                    message.message = message.message.replaceAll("\\$DISPLAYNAME", "someone");
                } else {
                    message.message = message.message.replaceAll("\\$DISPLAYNAME", interaction.BuyerName);
                }
                message.message = message.message.replaceAll("\\$PLAYERNAME", player.getName().getContents());
                if (message.toAllPlayers != null && message.toAllPlayers) {
                    for (var p : event.world.players()) {
                        p.sendMessage(new TextComponent(message.message), UUID.randomUUID());
                    }
                } else {
                    player.sendMessage(new TextComponent(message.message), UUID.randomUUID());
                }
            }
        }
        if (inspect.primedTNT != null) {
            for (var primedTNT : inspect.primedTNT) {
                LOGGER.info("SPAWNING TNT");
                var tnt = new PrimedTntComponent(event.world.dayTime(), player.getUUID(), primedTNT.xOffset, primedTNT.yOffset, primedTNT.zOffset, primedTNT.primeTicks, primedTNT.delaySeconds);
                tnt.init();
            }
        }
        if (inspect.mobs != null) {
            LOGGER.info("Spawning {} mobs", inspect.mobs.length);
            for (var mobComponent : inspect.mobs) {
                LOGGER.info("SPAWNING mob with id {}", mobComponent.entityID);
                Mob mob = mobComponent.getMob(event.world, interaction.BuyerName);
                mob.setPos(player.getX(), player.getY(), player.getZ());
                event.world.addFreshEntity(mob);
            }
        }
        if (inspect.sounds != null) {
            for (var soundComponent : inspect.sounds) {
                var sc = new SoundComponent(player.getUUID(), soundComponent.soundID, soundComponent.delaySeconds, event.world.dayTime());
                sc.init();
            }
        }
        if (inspect.statuses != null) {
            for (var statusComponent : inspect.statuses) {
                MobEffectInstance mei = new MobEffectInstance(ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(statusComponent.statusID)), statusComponent.tickDuration);
                player.addEffect(mei);
            }
        }
        if (inspect.lightning) {
            // Pick random block in radius around player
            Random rand = new Random();
            var xOffset = -3 + rand.nextInt(4);
            var zOffset = -3 + rand.nextInt(4);
            LightningBolt lb = new LightningBolt(EntityType.LIGHTNING_BOLT, event.world);
            lb.setPos(player.getX() + xOffset, player.getY(), player.getZ() + zOffset);
            event.world.addFreshEntity(lb);
        }
        if (inspect.whitelist) {
            // Whitelist the display name if it exists, and whitelist is enabled
            if (event.world.getServer().isEnforceWhitelist()) {
                var wlc = new WhitelistCommand();
            }
        }
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
        //         Creeper creeper = new Creeper(EntityType.CREEPER, instance.world);

        //         LOGGER.info("spawning creeper at {}, {}, {}", x+1, y+1, z+1);

        //         // var creeper = ;
        //         // SpawnEntity()
        //     }
        // }
    }


    @Mod.EventBusSubscriber(modid = "tangia", bus = Bus.FORGE)
    public static class MyStaticServerOnlyEventHandler {
        @SubscribeEvent
        public static void onInteractBlockEvent(PlayerInteractEvent.RightClickBlock event) {
            LOGGER.info("Got block interaction event '{}'", event.toString());
        }

        @SubscribeEvent
        public static void onInteractEmptyEvent(PlayerInteractEvent.RightClickEmpty event) {
            LOGGER.info("Got empty interaction event '{}'", event.toString());
        }

        @SubscribeEvent
        public static void onInteractEvent(PlayerInteractEvent.RightClickItem event) {
            LOGGER.info("Got interaction event '{}'", event.toString());
            LOGGER.info("Got interaction item '{}'", event.getItemStack());
            var gameId = System.getenv("DEBUG");
            if (gameId == null) {
                return;
            }

            // Spawn live TNT above someone's head
            // Play a delayed sound
            ScheduledThreadPoolExecutor exece = new ScheduledThreadPoolExecutor(1);
            exece.schedule(new Runnable() {
                public void run() {
                    var liveTNT = new PrimedTnt(event.getWorld(), event.getEntity().getX(), event.getEntity().getY() + 5, event.getEntity().getZ(), null);
                    liveTNT.setFuse(30);
                    event.getWorld().addFreshEntity(liveTNT);
                }
            }, 1, TimeUnit.SECONDS);
            exece.schedule(new Runnable() {
                public void run() {
                    var liveTNT = new PrimedTnt(event.getWorld(), event.getEntity().getX(), event.getEntity().getY() + 5, event.getEntity().getZ(), null);
                    liveTNT.setFuse(30);
                    event.getWorld().addFreshEntity(liveTNT);
                }
            }, 2, TimeUnit.SECONDS);
            exece.schedule(new Runnable() {
                public void run() {
                    var liveTNT = new PrimedTnt(event.getWorld(), event.getEntity().getX(), event.getEntity().getY() + 5, event.getEntity().getZ(), null);
                    liveTNT.setFuse(30);
                    event.getWorld().addFreshEntity(liveTNT);
                }
            }, 3, TimeUnit.SECONDS);

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

            var kelp = new ItemStack(Items.DRIED_KELP_BLOCK, 1);
            kelp.setHoverName(new TextComponent("kit - hehe"));
            event.getPlayer().getInventory().add(kelp);

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
            // 		event.getPlayer().world.clip(new ClipContext(event.getPlayer().getEyePosition(1f), event.getPlayer().getEyePosition(1f).add(event.getPlayer().getViewVector(1f).scale(100)),
            // 				ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, event.getPlayer())).getBlockPos().getX(),
            // 		event.getPlayer().world.clip(new ClipContext(event.getPlayer().getEyePosition(1f), event.getPlayer().getEyePosition(1f).add(event.getPlayer().getViewVector(1f).scale(100)),
            // 				ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, event.getPlayer())).getBlockPos().getY(),
            // 		event.getPlayer().world.clip(new ClipContext(event.getPlayer().getEyePosition(1f), event.getPlayer().getEyePosition(1f).add(event.getPlayer().getViewVector(1f).scale(100)),
            // 				ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, event.getPlayer())).getBlockPos().getZ())));
            // entityToSpawn.setVisualOnly(false);
            // world.addFreshEntity(entityToSpawn);

            // spawn ghast where player is looking
            // Ghast entityToSpawn = EntityType.GHAST.create(world);
            Entity entityToSpawn = ForgeRegistries.ENTITIES.getValue(new ResourceLocation("donkey")).create(world);
            entityToSpawn.moveTo(Vec3.atBottomCenterOf(new BlockPos(
                event.getWorld().clip(new ClipContext(event.getPlayer().getEyePosition(1f), event.getPlayer().getEyePosition(1f).add(event.getPlayer().getViewVector(1f).scale(100)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, event.getPlayer())).getBlockPos().getX(),
                event.getWorld().clip(new ClipContext(event.getPlayer().getEyePosition(1f), event.getPlayer().getEyePosition(1f).add(event.getPlayer().getViewVector(1f).scale(100)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, event.getPlayer())).getBlockPos().getY(),
                event.getWorld().clip(new ClipContext(event.getPlayer().getEyePosition(1f), event.getPlayer().getEyePosition(1f).add(event.getPlayer().getViewVector(1f).scale(100)),
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
            var blockState = event.getWorld().getBlockState(bp);
            LOGGER.info("Interacted with block - {}", blockState.getBlock().getRegistryName());
            BlockEntity b = event.getWorld().getBlockEntity(bp);
            if (b instanceof ShulkerBoxBlockEntity sbe) {
                var textcomp = sbe.getCustomName();
                if (textcomp != null) {
                    String thestring = textcomp.getString();
                    LOGGER.info("INTERACTED WITH SHULKER BLOCK - '{}'", thestring);
                }
            }
            if (b instanceof ChestBlockEntity cbe) {
                var textcomp = cbe.getCustomName();
                if (textcomp != null) {
                    String thestring = textcomp.getString();
                    LOGGER.info("INTERACTED WITH CHEST BLOCK - '{}'", thestring);
                    if (thestring.startsWith("decoy")) {
                        Random rand = new Random();
                        int randomInt = rand.nextInt(2);
                        Level world = event.getWorld();
                        // Remove chest from position
                        // world.removeBlockEntity(bp); // makes an empty default chest for some reason
                        world.setBlock(bp, Blocks.AIR.defaultBlockState(), Block.getId(Blocks.AIR.defaultBlockState())); // breaks the chest

                        // Spawn creeper
                        Creeper creeper = new Creeper(EntityType.CREEPER, event.getWorld());
                        creeper.setPos(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());
                        // CompoundTag nbt = creeper.serializeNBT();
                        // nbt.putBoolean("powered", true);
                        // creeper.deserializeNBT(nbt);
                        creeper.setCustomNameVisible(true);
                        TextComponent name = new TextComponent(thestring.substring(5));
                        creeper.setCustomName(name);
                        world.addFreshEntity(creeper);
                    }
                }
            }
        }
    }

}
