package nukkitcoders.mobplugin;

import cn.nukkit.Player;
import cn.nukkit.blockentity.BlockEntityID;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.config.ServerPropertiesKeys;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.item.EntityItem;
import cn.nukkit.event.Listener;
import cn.nukkit.level.GameRule;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.registry.EntityRegistry.EntityDefinition;
import nukkitcoders.mobplugin.entities.BaseEntity;
import nukkitcoders.mobplugin.entities.animal.flying.Allay;
import nukkitcoders.mobplugin.entities.animal.flying.Bat;
import nukkitcoders.mobplugin.entities.animal.flying.Bee;
import nukkitcoders.mobplugin.entities.animal.flying.Parrot;
import nukkitcoders.mobplugin.entities.animal.jumping.Frog;
import nukkitcoders.mobplugin.entities.animal.jumping.Rabbit;
import nukkitcoders.mobplugin.entities.animal.swimming.*;
import nukkitcoders.mobplugin.entities.animal.walking.*;
import nukkitcoders.mobplugin.entities.block.BlockEntitySpawner;
import nukkitcoders.mobplugin.entities.monster.flying.*;
import nukkitcoders.mobplugin.entities.monster.jumping.MagmaCube;
import nukkitcoders.mobplugin.entities.monster.jumping.Slime;
import nukkitcoders.mobplugin.entities.monster.swimming.ElderGuardian;
import nukkitcoders.mobplugin.entities.monster.swimming.Guardian;
import nukkitcoders.mobplugin.entities.monster.walking.*;
import nukkitcoders.mobplugin.entities.projectile.*;
import nukkitcoders.mobplugin.utils.Utils;

/**
 * @author <a href="mailto:kniffman@googlemail.com">Michael Gertz (kniffo80)</a>
 */
public class MobPlugin extends PluginBase implements Listener {

    private static MobPlugin INSTANCE;
    public Config config;

    public MobPlugin() {
        INSTANCE = this;
    }

    public static MobPlugin getInstance() {
        return INSTANCE;
    }

    @Override
    public void onLoad() {
        //提前注册，防止核心找不到
        this.registerEntities();
    }

    @Override
    public void onEnable() {
        config = new Config(this);

        if (!config.init(this)) {
            return;
        }

        this.getServer().getPluginManager().registerEvents(new EventListener(), this);

        if (config.spawnDelay > 0) {
            this.getServer().getScheduler().scheduleDelayedRepeatingTask(this, new AutoSpawnTask(this, config.pluginConfig), config.spawnDelay, config.spawnDelay);
            
            if (this.getServer().getProperties().get(ServerPropertiesKeys.SPAWN_ANIMALS, true) || this.getServer().getProperties().get(ServerPropertiesKeys.SPAWN_MOBS, true)){
                this.getServer().getLogger().notice("Disabling mob/animal spawning from server.properties does not disable spawning in MobPlugin");
            }
        } else {
            this.getServer().getLogger().notice("Mob spawning is disabled (autospawn-ticks <= 0)");
        }
    }

    @Override
    public void onDisable() {
        RouteFinderThreadPool.shutDownNow();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("mobplugin.command")) return false;
        /*if (cmd.getName().equals("summon")) {
            if (args.length == 0 || (args.length == 1 && !(sender instanceof Player))) {
                return false;
            }

            String mob = Character.toUpperCase(args[0].charAt(0)) + args[0].substring(1);
            int max = mob.length() - 1;
            for (int x = 2; x < max; x++) {
                if (mob.charAt(x) == '_') {
                    mob = mob.substring(0, x) + Character.toUpperCase(mob.charAt(x + 1)) + mob.substring(x + 2);
                }
            }

            Player playerThatSpawns;

            if (args.length == 2) {
                playerThatSpawns = getServer().getPlayerExact(args[1].replace("@s", sender.getName()));
            } else {
                playerThatSpawns = (Player) sender;
            }

            if (playerThatSpawns != null) {
                Position pos = playerThatSpawns.getPosition();
                Entity ent;
                if ((ent = Entity.createEntity(mob, pos)) != null) {
                    ent.spawnToAll();
                    sender.sendMessage("\u00A76Spawned " + mob + " to " + playerThatSpawns.getName());
                } else {
                    sender.sendMessage("\u00A7cUnable to spawn " + mob);
                }
            } else {
                sender.sendMessage("\u00A7cUnknown player " + (args.length == 2 ? args[1] : sender.getName()));
            }
        } else */if (cmd.getName().equals("mob")) {
            if (args.length == 0) {
                sender.sendMessage("-- MobPlugin " + this.getDescription().getVersion() + " --");
                sender.sendMessage("/mob spawn <entity> <opt:player> - Summon entity");
                sender.sendMessage("/mob removeall - Remove all living mobs");
                sender.sendMessage("/mob removeitems - Remove all items from ground");
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "spawn":
                    if (args.length == 1 || (args.length == 2 && !(sender instanceof Player))) {
                        sender.sendMessage("Usage: /mob spawn <entity> <opt:player>");
                        break;
                    }

                    String mob = args[1];
                    Player playerThatSpawns;

                    if (args.length == 3) {
                        playerThatSpawns = this.getServer().getPlayerExact(args[2]);
                    } else {
                        playerThatSpawns = (Player) sender;
                    }

                    if (playerThatSpawns != null) {
                        Position pos = playerThatSpawns.getPosition();

                        Entity ent;
                        if ((ent = Entity.createEntity(mob, pos)) != null) {
                            ent.spawnToAll();
                            sender.sendMessage("Spawned " + mob + " to " + playerThatSpawns.getName());
                        } else {
                            sender.sendMessage("Unable to spawn " + mob);
                        }
                    } else {
                        sender.sendMessage("Unknown player " + (args.length == 3 ? args[2] : sender.getName()));
                    }
                    break;
                case "removeall":
                    int count = 0;
                    for (Level level : getServer().getLevels().values()) {
                        for (Entity entity : level.getEntities()) {
                            if (entity instanceof BaseEntity) {
                                entity.close();
                                ++count;
                            }
                        }
                    }
                    sender.sendMessage("Removed " + count + " entities from all levels.");
                    break;
                case "removeitems":
                    count = 0;
                    for (Level level : getServer().getLevels().values()) {
                        for (Entity entity : level.getEntities()) {
                            if (entity instanceof EntityItem && entity.isOnGround()) {
                                entity.close();
                                ++count;
                            }
                        }
                    }
                    sender.sendMessage("Removed " + count + " items on ground from all levels.");
                    break;
                default:
                    sender.sendMessage("Unknown command.");
                    break;
            }
        }

        return true;
    }

    private void registerEntities() {
        PluginEntityRegistry registry = new PluginEntityRegistry();

        registry.registerBlockEntity(BlockEntityID.MOB_SPAWNER, BlockEntitySpawner.class);

        registry.registerEntity(new EntityDefinition(Entity.MAGMA_CUBE, "", 42, true, true), MagmaCube.class);
        registry.registerEntity(new EntityDefinition(Entity.WANDERING_TRADER, "", 118, true, true), WanderingTrader.class);
        registry.registerEntity(new EntityDefinition(Entity.SKELETON, "", 34, true, true), Skeleton.class);
        registry.registerEntity(new EntityDefinition(Entity.ZOGLIN, "", 126, true, true), Zoglin.class);
        registry.registerEntity(new EntityDefinition(Entity.EVOCATION_ILLAGER, "", 104, true, true), Evoker.class);
        registry.registerEntity(new EntityDefinition(Entity.HOGLIN, "", 124, true, true), Hoglin.class);
        registry.registerEntity(new EntityDefinition(Entity.DOLPHIN, "", 31, true, true), Dolphin.class);
        registry.registerEntity(new EntityDefinition(Entity.PARROT, "", 30, true, true), Parrot.class);
        registry.registerEntity(new EntityDefinition(Entity.TADPOLE, "", 133, true, true), Tadpole.class);
        registry.registerEntity(new EntityDefinition(Entity.RAVAGER, "", 59, true, true), Ravager.class);
        registry.registerEntity(new EntityDefinition(Entity.ELDER_GUARDIAN, "", 50, true, true), ElderGuardian.class);
        registry.registerEntity(new EntityDefinition(Entity.PIGLIN_BRUTE, "", 127, true, true), PiglinBrute.class);
        registry.registerEntity(new EntityDefinition(Entity.ZOMBIE_HORSE, "", 27, true, true), ZombieHorse.class);
        registry.registerEntity(new EntityDefinition(Entity.FOX, "", 121, true, true), Fox.class);
        registry.registerEntity(new EntityDefinition(Entity.SILVERFISH, "", 39, true, true), Silverfish.class);
        registry.registerEntity(new EntityDefinition(Entity.GOAT, "", 128, true, true), Goat.class);
        registry.registerEntity(new EntityDefinition(Entity.MULE, "", 25, true, true), Mule.class);
        registry.registerEntity(new EntityDefinition(Entity.FROG, "", 132, true, true), Frog.class);
        registry.registerEntity(new EntityDefinition(Entity.BEE, "", 122, true, true), Bee.class);
        registry.registerEntity(new EntityDefinition(Entity.GLOW_SQUID, "", 129, true, true), GlowSquid.class);
        registry.registerEntity(new EntityDefinition(Entity.RABBIT, "", 18, true, true), Rabbit.class);
        registry.registerEntity(new EntityDefinition(Entity.ENDERMITE, "", 55, true, true), Endermite.class);
        registry.registerEntity(new EntityDefinition(Entity.STRIDER, "", 125, true, true), Strider.class);
        registry.registerEntity(new EntityDefinition(Entity.HUSK, "", 47, true, true), Husk.class);
        registry.registerEntity(new EntityDefinition(Entity.ZOMBIE_PIGMAN, "", 36, true, true), ZombiePigman.class);
        registry.registerEntity(new EntityDefinition(Entity.VINDICATOR, "", 57, true, true), Vindicator.class);
        registry.registerEntity(new EntityDefinition(Entity.WITCH, "", 45, true, true), Witch.class);
        registry.registerEntity(new EntityDefinition(Entity.DONKEY, "", 24, true, true), Donkey.class);
        registry.registerEntity(new EntityDefinition(Entity.OCELOT, "", 22, true, true), Ocelot.class);
        registry.registerEntity(new EntityDefinition(Entity.ENDERMAN, "", 38, true, true), Enderman.class);
        registry.registerEntity(new EntityDefinition(Entity.LLAMA, "", 29, true, true), Llama.class);
        registry.registerEntity(new EntityDefinition(Entity.DROWNED, "", 110, true, true), Drowned.class);
        registry.registerEntity(new EntityDefinition(Entity.WITHER_SKELETON, "", 48, true, true), WitherSkeleton.class);
        registry.registerEntity(new EntityDefinition(Entity.SKELETON_HORSE, "", 26, true, true), SkeletonHorse.class);
        registry.registerEntity(new EntityDefinition(Entity.VEX, "", 105, true, true), Vex.class);
        registry.registerEntity(new EntityDefinition(Entity.ZOMBIE_VILLAGER, "", 44, true, true), ZombieVillager.class);
        registry.registerEntity(new EntityDefinition(Entity.PILLAGER, "", 114, true, true), Pillager.class);
        registry.registerEntity(new EntityDefinition(Entity.PUFFERFISH, "", 108, true, true), Pufferfish.class);
        registry.registerEntity(new EntityDefinition(Entity.SNOW_GOLEM, "", 21, true, true), SnowGolem.class);
        registry.registerEntity(new EntityDefinition(Entity.STRAY, "", 46, true, true), Stray.class);
        registry.registerEntity(new EntityDefinition(Entity.BAT, "", 19, true, true), Bat.class);
        registry.registerEntity(new EntityDefinition(Entity.PANDA, "", 113, true, true), Panda.class);
        registry.registerEntity(new EntityDefinition(Entity.IRON_GOLEM, "", 20, true, true), IronGolem.class);
        registry.registerEntity(new EntityDefinition(Entity.SPIDER, "", 35, true, true), Spider.class);
        registry.registerEntity(new EntityDefinition(Entity.PIGLIN, "", 123, true, true), Piglin.class);
        registry.registerEntity(new EntityDefinition(Entity.GHAST, "", 41, true, true), Ghast.class);
        registry.registerEntity(new EntityDefinition(Entity.AXOLOTL, "", 130, true, true), Axolotl.class);
        registry.registerEntity(new EntityDefinition(Entity.ALLAY, "", 134, true, true), Allay.class);
        registry.registerEntity(new EntityDefinition(Entity.GUARDIAN, "", 49, true, true), Guardian.class);
        registry.registerEntity(new EntityDefinition(Entity.CAVE_SPIDER, "", 40, true, true), CaveSpider.class);
        registry.registerEntity(new EntityDefinition(Entity.ENDER_DRAGON, "", 53, true, true), EnderDragon.class);
        registry.registerEntity(new EntityDefinition(Entity.TURTLE, "", 74, true, true), Turtle.class);
        registry.registerEntity(new EntityDefinition(Entity.SHULKER, "", 54, true, true), Shulker.class);
        registry.registerEntity(new EntityDefinition(Entity.WITHER, "", 52, true, true), Wither.class);
        registry.registerEntity(new EntityDefinition(Entity.ZOMBIE_VILLAGER_V2, "", 116, true, true), ZombieVillagerV2.class);
        registry.registerEntity(new EntityDefinition(Entity.BLAZE, "", 43, true, true), Blaze.class);
        registry.registerEntity(new EntityDefinition(Entity.POLAR_BEAR, "", 28, true, true), PolarBear.class);
        registry.registerEntity(new EntityDefinition(Entity.SLIME, "", 37, true, true), Slime.class);
        registry.registerEntity(new EntityDefinition(Entity.SQUID, "", 17, true, true), Squid.class);
        registry.registerEntity(new EntityDefinition(Entity.PHANTOM, "", 58, true, true), Phantom.class);
        registry.registerEntity(new EntityDefinition(Entity.CAMEL, "", 138, true, true), Camel.class);

        registry.registerEntity(new EntityDefinition(Entity.SHULKER_BULLET, "", 76, false, false), EntityShulkerBullet.class);
        registry.registerEntity(new EntityDefinition(Entity.FIREBALL, "", 85, false, false), EntityGhastFireBall.class);
        registry.registerEntity(new EntityDefinition(Entity.LLAMA_SPIT, "", 102, false, false), EntityLlamaSpit.class);
        registry.registerEntity(new EntityDefinition(Entity.SMALL_FIREBALL, "", 94, false, false), EntityBlazeFireBall.class);
        registry.registerEntity(new EntityDefinition(Entity.WITHER_SKULL_DANGEROUS, "", 91, false, false), EntityBlueWitherSkull.class);
        registry.registerEntity(new EntityDefinition(Entity.DRAGON_FIREBALL, "", 79, false, false), EntityEnderCharge.class);
        registry.registerEntity(new EntityDefinition(Entity.WITHER_SKULL, "", 89, false, false), EntityWitherSkull.class);
        registry.registerEntity(new EntityDefinition(Entity.THROWN_TRIDENT, "", 73, false, false), DespawnableThrownTrident.class);
    }

    public static boolean isAnimalSpawningAllowedByTime(Level level) {
        int time = level.getTime() % Level.TIME_FULL;
        return time < 13184 || time > 22800;
    }

    public static boolean isMobSpawningAllowedByTime(Level level) {
        int time = level.getTime() % Level.TIME_FULL;
        return time > 13184 && time < 22800;
    }

    public static boolean isSpawningAllowedByLevel(Level level) {
        return !INSTANCE.config.mobSpawningDisabledWorlds.contains(level.getName().toLowerCase()) && level.getGameRules().getBoolean(GameRule.DO_MOB_SPAWNING);
    }

    public static boolean shouldMobBurn(Level level, BaseEntity entity) {
        int time = level.getTime() % Level.TIME_FULL;
        return !entity.isOnFire() && !level.isRaining() && !entity.isBaby() && (time < 12567 || time > 23450) && !Utils.entityInsideWaterFast(entity) && level.canBlockSeeSky(entity);
    }

    public static boolean isEntityCreationAllowed(Level level) {
        return !INSTANCE.config.mobCreationDisabledWorlds.contains(level.getName().toLowerCase());
    }
}
