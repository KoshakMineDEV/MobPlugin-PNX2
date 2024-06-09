package nukkitcoders.mobplugin;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.projectile.EntityEnderPearl;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.ProjectileHitEvent;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemSpawnEgg;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.level.Position;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.SimpleAxisAlignedBB;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.EntityEventPacket;
import cn.nukkit.network.protocol.PlayerInputPacket;
import nukkitcoders.mobplugin.entities.HorseBase;
import nukkitcoders.mobplugin.entities.animal.walking.Llama;
import nukkitcoders.mobplugin.entities.animal.walking.Strider;
import nukkitcoders.mobplugin.entities.block.BlockEntitySpawner;
import nukkitcoders.mobplugin.entities.monster.WalkingMonster;
import nukkitcoders.mobplugin.entities.monster.flying.Wither;
import nukkitcoders.mobplugin.entities.monster.walking.Enderman;
import nukkitcoders.mobplugin.entities.monster.walking.IronGolem;
import nukkitcoders.mobplugin.entities.monster.walking.Silverfish;
import nukkitcoders.mobplugin.entities.monster.walking.SnowGolem;
import nukkitcoders.mobplugin.event.entity.SpawnGolemEvent;
import nukkitcoders.mobplugin.event.entity.SpawnWitherEvent;
import nukkitcoders.mobplugin.event.spawner.SpawnerChangeTypeEvent;
import nukkitcoders.mobplugin.event.spawner.SpawnerCreateEvent;
import nukkitcoders.mobplugin.utils.FastMathLite;
import nukkitcoders.mobplugin.utils.Utils;

import static nukkitcoders.mobplugin.entities.block.BlockEntitySpawner.*;

public class EventListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void PlayerDeathEvent(PlayerDeathEvent ev) {
        this.handleAttackedEntityAngry(ev.getEntity());
    }

    private void handleAttackedEntityAngry(Entity entity) {
        if (!(entity.getLastDamageCause() instanceof EntityDamageByEntityEvent)) return;

        Entity damager = ((EntityDamageByEntityEvent) entity.getLastDamageCause()).getDamager();
        if (damager instanceof IronGolem || damager instanceof SnowGolem) {
            ((WalkingMonster) damager).isAngryTo = -1L;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void PlayerInteractEvent(PlayerInteractEvent ev) {
        if (ev.getFace() == null || ev.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;

        Item item = ev.getItem();
        Block block = ev.getBlock();
        if (!item.getId().endsWith("_spawn_egg") || !block.getId().equals(Block.MOB_SPAWNER)) return;

        Player player = ev.getPlayer();
        ItemSpawnEgg spawnEgg = (ItemSpawnEgg) item;
        if (player.isAdventure()) return;

        BlockEntity blockEntity = block.getLevel().getBlockEntity(block);
        if (blockEntity instanceof BlockEntitySpawner) {
            SpawnerChangeTypeEvent event = new SpawnerChangeTypeEvent((BlockEntitySpawner) blockEntity, ev.getBlock(), ev.getPlayer(), ((BlockEntitySpawner) blockEntity).getSpawnEntityType(), spawnEgg.getEntityNetworkId());
            Server.getInstance().getPluginManager().callEvent(event);
            if (((BlockEntitySpawner) blockEntity).getSpawnEntityType() == spawnEgg.getEntityNetworkId()) {
                if (MobPlugin.getInstance().config.noSpawnEggWasting) {
                    event.setCancelled(true);
                    return;
                }
            }

            if (event.isCancelled()) return;
            ((BlockEntitySpawner) blockEntity).setSpawnEntityType(spawnEgg.getEntityNetworkId());
            ev.setCancelled(true);

            if (!player.isCreative()) {
                player.getInventory().decreaseCount(player.getInventory().getHeldItemIndex());
            }
        } else {
            SpawnerCreateEvent event = new SpawnerCreateEvent(ev.getPlayer(), ev.getBlock(), spawnEgg.getEntityNetworkId());
            Server.getInstance().getPluginManager().callEvent(event);
            if (event.isCancelled()) return;
            ev.setCancelled(true);
            if (blockEntity != null) {
                blockEntity.close();
            }
            CompoundTag nbt = new CompoundTag()
                    .putString(TAG_ID, BlockEntity.MOB_SPAWNER)
                    .putInt(TAG_ENTITY_ID, spawnEgg.getEntityNetworkId())
                    .putInt(TAG_X, (int) block.x)
                    .putInt(TAG_Y, (int) block.y)
                    .putInt(TAG_Z, (int) block.z);

            BlockEntitySpawner entitySpawner = new BlockEntitySpawner(block.getLevel().getChunk((int) block.x >> 4, (int) block.z >> 4), nbt);
            entitySpawner.spawnToAll();

            if (!player.isCreative()) {
                player.getInventory().decreaseCount(player.getInventory().getHeldItemIndex());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void BlockPlaceEvent(BlockPlaceEvent ev) {
        Block block = ev.getBlock();
        if (!MobPlugin.isEntityCreationAllowed(block.getLevel())) {
            return;
        }
        Player player = ev.getPlayer();
        Item item = ev.getItem();
        if (block.getId() == Block.LIT_PUMPKIN || block.getId() == Block.PUMPKIN) {
            if (block.getSide(BlockFace.DOWN).getId() == BlockID.SNOW && block.getSide(BlockFace.DOWN, 2).getId() == BlockID.SNOW) {

                Position pos = block.add(0.5, -1, 0.5);
                SpawnGolemEvent event = new SpawnGolemEvent(player, pos, SpawnGolemEvent.GolemType.SNOW_GOLEM);
                Server.getInstance().getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    return;
                }

                block.level.setBlock(block.add(0, -1, 0), Block.get(Block.AIR));
                block.level.setBlock(block.add(0, -2, 0), Block.get(Block.AIR));

                Entity.createEntity("SnowGolem", pos).spawnToAll();
                ev.setCancelled(true);
                if (player.isSurvival()) player.getInventory().removeItem(Item.get(block.getId()));

            } else if (block.getSide(BlockFace.DOWN).getId() == BlockID.IRON_BLOCK && block.getSide(BlockFace.DOWN, 2).getId() == BlockID.IRON_BLOCK) {
                String removeId = block.getId();
                block = block.getSide(BlockFace.DOWN);

                Block first = null, second = null;
                if (block.getSide(BlockFace.EAST).getId() == BlockID.IRON_BLOCK && block.getSide(BlockFace.WEST).getId() == BlockID.IRON_BLOCK) {
                    first = block.getSide(BlockFace.EAST);
                    second = block.getSide(BlockFace.WEST);
                } else if (block.getSide(BlockFace.NORTH).getId() == BlockID.IRON_BLOCK && block.getSide(BlockFace.SOUTH).getId() == BlockID.IRON_BLOCK) {
                    first = block.getSide(BlockFace.NORTH);
                    second = block.getSide(BlockFace.SOUTH);
                }

                if (second == null || first == null) return;

                Position pos = block.add(0.5, -1, 0.5);
                SpawnGolemEvent event = new SpawnGolemEvent(player, pos, SpawnGolemEvent.GolemType.IRON_GOLEM);
                Server.getInstance().getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    return;
                }

                block.level.setBlock(first, Block.get(Block.AIR));
                block.level.setBlock(second, Block.get(Block.AIR));
                block.level.setBlock(block, Block.get(Block.AIR));
                block.level.setBlock(block.add(0, -1, 0), Block.get(Block.AIR));

                Entity.createEntity("IronGolem", pos).spawnToAll();
                ev.setCancelled(true);
                if (player.isSurvival()) player.getInventory().removeItem(Item.get(removeId));
            }
        } else if (item.getId() == "minecraft:skull" && item.getDamage() == 1) {
            if (block.getSide(BlockFace.DOWN).getId() == BlockID.SOUL_SAND && block.getSide(BlockFace.DOWN, 2).getId() == BlockID.SOUL_SAND) {
                Block first, second;

                if (!(((first = block.getSide(BlockFace.EAST)).getId() == BlockID.SKULL && first.toItem().getDamage() == 1) && ((second = block.getSide(BlockFace.WEST)).getId() == BlockID.SKULL && second.toItem().getDamage() == 1) || ((first = block.getSide(BlockFace.NORTH)).getId() == BlockID.SKULL && first.toItem().getDamage() == 1) && ((second = block.getSide(BlockFace.SOUTH)).getId() == BlockID.SKULL && second.toItem().getDamage() == 1))) {
                    return;
                }

                block = block.getSide(BlockFace.DOWN);

                Block first2, second2;

                if (!((first2 = block.getSide(BlockFace.EAST)).getId() == BlockID.SOUL_SAND && (second2 = block.getSide(BlockFace.WEST)).getId() == BlockID.SOUL_SAND || (first2 = block.getSide(BlockFace.NORTH)).getId() == BlockID.SOUL_SAND && (second2 = block.getSide(BlockFace.SOUTH)).getId() == BlockID.SOUL_SAND)) {
                    return;
                }

                Position pos = block.add(0.5, -1, 0.5);
                SpawnWitherEvent event = new SpawnWitherEvent(player, pos);
                Server.getInstance().getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    return;
                }

                block.getLevel().setBlock(first, Block.get(Block.AIR));
                block.getLevel().setBlock(second, Block.get(Block.AIR));
                block.getLevel().setBlock(first2, Block.get(Block.AIR));
                block.getLevel().setBlock(second2, Block.get(Block.AIR));
                block.getLevel().setBlock(block, Block.get(Block.AIR));
                block.getLevel().setBlock(block.add(0, -1, 0), Block.get(Block.AIR));

                if (!player.isCreative()) {
                    item.setCount(item.getCount() - 1);
                    player.getInventory().setItemInHand(item);
                }

                Wither wither = (Wither) Entity.createEntity("Wither", pos);
                wither.stayTime = 220;
                wither.spawnToAll();
                block.getLevel().addSound(block, cn.nukkit.level.Sound.MOB_WITHER_SPAWN);
                ev.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void BlockBreakEvent(BlockBreakEvent ev) {
        Block block = ev.getBlock();
        if ((block.getId() == Block.MONSTER_EGG) && Utils.rand(1, 5) == 1 && !ev.getItem().hasEnchantment(Enchantment.ID_SILK_TOUCH) && block.level.getBlockLightAt((int) block.x, (int) block.y, (int) block.z) < 12) {
            Silverfish entity = (Silverfish) Entity.createEntity("Silverfish", block.add(0.5, 0, 0.5));
            if (entity == null) return;
            entity.spawnToAll();
            EntityEventPacket pk = new EntityEventPacket();
            pk.eid = entity.getId();
            pk.event = 27;
            entity.level.addChunkPacket(entity.getChunkX(), entity.getChunkZ(), pk);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void ProjectileHitEvent(ProjectileHitEvent ev) {
        /*if (ev.getEntity() instanceof EntityEgg) {
            if (Utils.rand(1, 20) == 5) {
                Chicken entity = (Chicken) Entity.createEntity("Chicken", ev.getEntity().add(0.5, 1, 0.5));
                if (entity != null) {
                    entity.spawnToAll();
                    entity.setBaby(true);
                }
            }
        }*/

        if (ev.getEntity() instanceof EntityEnderPearl) {
            if (Utils.rand(1, 20) == 5) {
                Entity entity = Entity.createEntity("Endermite", ev.getEntity().add(0.5, 1, 0.5));
                if (entity != null) {
                    entity.spawnToAll();
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void DataPacketReceiveEvent(DataPacketReceiveEvent ev) {
        //PNX没有使用PlayerAuthInputPacket
        /*if (ev.getPacket() instanceof PlayerAuthInputPacket) {
            Player p = ev.getPlayer();
            if (!p.locallyInitialized) {
                return;
            }
            PlayerAuthInputPacket pk = (PlayerAuthInputPacket) ev.getPacket();
            double inputX = pk.getMotion().getX();
            double inputY = pk.getMotion().getY();
            if (inputX >= -1.0 && inputX <= 1.0 && inputY >= -1.0 && inputY <= 1.0) {
                if (p.riding instanceof HorseBase && !(p.riding instanceof Llama)) {
                    ((HorseBase) p.riding).onPlayerInput(p, inputX, inputY);
                } else if (p.riding instanceof Pig) {
                    ((Pig) p.riding).onPlayerInput(p, inputX, inputY);
                } else if (p.riding instanceof Strider) {
                    ((Strider) p.riding).onPlayerInput(p, inputX, inputY);
                }
            }
        }*/
        if (ev.getPacket() instanceof PlayerInputPacket) {
            PlayerInputPacket ipk = (PlayerInputPacket) ev.getPacket();
            Player p = ev.getPlayer();
            if (p.riding instanceof HorseBase && !(p.riding instanceof Llama)) {
                ((HorseBase) p.riding).onPlayerInput(p, ipk.motionX, ipk.motionY);
            }/* else if (p.riding instanceof Pig) {
                ((Pig) p.riding).onPlayerInput(p, ipk.motionX, ipk.motionY);
            } */else if (p.riding instanceof Strider) {
                ((Strider) p.riding).onPlayerInput(p, ipk.motionX, ipk.motionY);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void PlayerMoveEvent(PlayerMoveEvent ev) {
        Player player = ev.getPlayer();
        if (player.ticksLived % 20 == 0) {
            AxisAlignedBB aab = new SimpleAxisAlignedBB(
                    player.getX() - 0.6f,
                    player.getY() + 1.45f,
                    player.getZ() - 0.6f,
                    player.getX() + 0.6f,
                    player.getY() + 2.9f,
                    player.getZ() + 0.6f
            );

            for (int i = 0; i < 8; i++) {
                aab.offset(-FastMathLite.sin(player.getYaw() * Math.PI / 180) * i, i * (Math.tan(player.getPitch() * -3.141592653589793 / 180)), FastMathLite.cos(player.getYaw() * Math.PI / 180) * i);
                Entity[] entities = player.getLevel().getCollidingEntities(aab);
                for (Entity e : entities) {
                    if (e instanceof Enderman) {
                        ((Enderman) e).stareToAngry();
                    }
                }
            }
        }
    }
}
