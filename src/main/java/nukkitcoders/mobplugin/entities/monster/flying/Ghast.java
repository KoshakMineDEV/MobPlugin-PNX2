package nukkitcoders.mobplugin.entities.monster.flying;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.data.EntityFlag;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.ProjectileLaunchEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Location;
import cn.nukkit.level.format.IChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.LevelEventPacket;
import nukkitcoders.mobplugin.entities.monster.FlyingMonster;
import nukkitcoders.mobplugin.entities.projectile.EntityGhastFireBall;
import nukkitcoders.mobplugin.utils.FastMathLite;
import nukkitcoders.mobplugin.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Ghast extends FlyingMonster {

    public static final int NETWORK_ID = 41;

    private boolean attacked;

    public Ghast(IChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public @NotNull String getIdentifier() {
        return GHAST;
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public float getWidth() {
        return 4;
    }

    @Override
    public float getHeight() {
        return 4;
    }

    @Override
    public double getSpeed() {
        return 1.2;
    }

    @Override
    public void initEntity() {
        super.initEntity();

        this.fireProof = true;
        this.setMaxHealth(10);
        this.setDataFlag(EntityFlag.FIRE_IMMUNE, true);
    }

    @Override
    public boolean targetOption(EntityCreature creature, double distance) {
        if (creature instanceof Player) {
            Player player = (Player) creature;
            return !player.closed && player.spawned && player.isAlive() && (player.isSurvival() || player.isAdventure()) && distance <= 4096;
        }
        return false;
    }

    @Override
    public void attackEntity(Entity player) {
        if (this.distanceSquared(player) <= (this.attacked ? 4096 : 784)) { // 28 blocks or 64 blocks if attacked)
            if (Utils.rand()) {
                this.attackDelay--;
                return;
            }
            if (this.attackDelay == 50) {
                this.level.addLevelEvent(this, LevelEventPacket.EVENT_SOUND_GHAST_WARNING);
            }
            if (this.attackDelay > 60) {
                this.attackDelay = 0;

                double f = 1.01;
                double yaw = this.yaw + Utils.rand(-4.0, 4.0);
                double pitch = this.pitch + Utils.rand(-4.0, 4.0);
                Location pos = new Location(this.x - Math.sin(FastMathLite.toRadians(yaw)) * Math.cos(FastMathLite.toRadians(pitch)) * 0.5, this.y + this.getEyeHeight() - 1, // below eyes
                        this.z + Math.cos(FastMathLite.toRadians(yaw)) * Math.cos(FastMathLite.toRadians(pitch)) * 0.5, yaw, pitch, this.level);

                if (this.getLevel().getBlockIdAt(pos.getFloorX(), pos.getFloorY(), pos.getFloorZ()) != Block.AIR) {
                    return;
                }

                EntityGhastFireBall fireball = (EntityGhastFireBall) Entity.createEntity("GhastFireBall", pos, this);
                fireball.setExplode(true);
                fireball.setMotion(new Vector3(-Math.sin(FastMathLite.toRadians(yaw)) * Math.cos(FastMathLite.toRadians(pitch)) * f * f, -Math.sin(FastMathLite.toRadians(pitch)) * f * f,
                        Math.cos(FastMathLite.toRadians(yaw)) * Math.cos(FastMathLite.toRadians(pitch)) * f * f));

                ProjectileLaunchEvent launch = new ProjectileLaunchEvent(fireball, this);
                this.server.getPluginManager().callEvent(launch);
                if (launch.isCancelled()) {
                    fireball.close();
                } else {
                    fireball.spawnToAll();
                    this.level.addLevelEvent(this, LevelEventPacket.EVENT_SOUND_GHAST_FIREBALL);
                }
            }
        }
    }

    @Override
    public boolean attack(EntityDamageEvent ev) {
        boolean result = super.attack(ev);

        if (!ev.isCancelled() && ev instanceof EntityDamageByEntityEvent) {
            if (((EntityDamageByEntityEvent) ev).getDamager() instanceof Player) {
                this.attacked = true;
            }
        }

        return result;
    }

    @Override
    public Item[] getDrops() {
        List<Item> drops = new ArrayList<>();

        drops.add(Item.get(Item.GUNPOWDER, 0, Utils.rand(0, 2)));
        drops.add(Item.get(Item.GHAST_TEAR, 0, Utils.rand(0, 1)));

        return drops.toArray(new Item[0]);
    }

    @Override
    public int getKillExperience() {
        return 5;
    }

    @Override
    public int nearbyDistanceMultiplier() {
        return 1000; // don't follow
    }
}
