package nukkitcoders.mobplugin.entities.autospawn;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockLava;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import nukkitcoders.mobplugin.AutoSpawnTask;
import nukkitcoders.mobplugin.MobPlugin;
import nukkitcoders.mobplugin.entities.animal.walking.Strider;
import nukkitcoders.mobplugin.utils.Utils;

/**
 * @author <a href="mailto:kniffman@googlemail.com">Michael Gertz</a>
 */
public abstract class AbstractEntitySpawner implements IEntitySpawner {

    protected AutoSpawnTask spawnTask;

    public AbstractEntitySpawner(AutoSpawnTask spawnTask) {
        this.spawnTask = spawnTask;
    }

    @Override
    public void spawn() {
        for (Player player : Server.getInstance().getOnlinePlayers().values()) {
            if (isSpawningAllowed(player)) {
                spawnTo(player);
            }
        }
    }

    private void spawnTo(Player player) {
        Level level = player.getLevel();
        Position pos = new Position(player.getFloorX(), player.getFloorY(), player.getFloorZ(), level);

        if (this.spawnTask.entitySpawnAllowed(level, this.getEntityNetworkId(), player)) {
            pos.x += this.spawnTask.getRandomSafeXZCoord(Utils.rand(48, 52), Utils.rand(24, 28), Utils.rand(4, 8));
            pos.z += this.spawnTask.getRandomSafeXZCoord(Utils.rand(48, 52), Utils.rand(24, 28), Utils.rand(4, 8));

            if (!level.isChunkLoaded((int) pos.x >> 4, (int) pos.z >> 4)) {
                return;
            }

            if (MobPlugin.getInstance().config.spawnNoSpawningArea > 0 && level.getSpawnLocation().distance(pos) < MobPlugin.getInstance().config.spawnNoSpawningArea) {
                return;
            }

            if (Utils.monstersList.contains(this.getEntityNetworkId())) {
                int biome = level.getBiomeId((int) pos.x, (int) pos.y, (int) pos.z);
                if (biome == 14 || biome == 15) {
                    return; // Hostile mobs don't spawn on mushroom island
                }
            }

            pos.y = this.spawnTask.getSafeYCoord(level, pos);

            if (this.isWaterMob()) {
                pos.y--;
            }

            if (pos.y < 1 || pos.y > 255 || level.getDimension() == Level.DIMENSION_NETHER && pos.y > 125) {
                return;
            }

            if (isTooNearOfPlayer(pos)) {
                return;
            }

            Block block = level.getBlock(pos, false);
            if (this.getEntityNetworkId() == Strider.NETWORK_ID) {
                if (!(block instanceof BlockLava)) {
                    return;
                }
            } else {
                if (block.getId().equals(Block.BROWN_MUSHROOM_BLOCK) || block.getId().equals(Block.RED_MUSHROOM_BLOCK)) { // Mushrooms aren't transparent but shouldn't have mobs spawned on them
                    return;
                }

                if (block.isTransparent() && !block.getId().equals(Block.SNOW_LAYER)) { // Snow layer is an exception
                    if ((!block.getId().equals(Block.WATER) && !block.getId().equals(Block.FLOWING_WATER)) || !this.isWaterMob()) { // Water mobs can spawn in water
                        return;
                    }
                }
            }

            spawn(player, pos, level);
        }
    }

    private boolean isSpawningAllowed(Player player) {
        if (player.isSpectator()) {
            return false;
        }
        if (Utils.rand(1, 4) != 1 && MobPlugin.isSpawningAllowedByLevel(player.getLevel())) {
            if (Server.getInstance().getDifficulty() == 0) {
                return !Utils.monstersList.contains(this.getEntityNetworkId());
            }
            return true;
        }
        return false;
    }

    private static boolean isTooNearOfPlayer(Position pos) {
        for (Player p : pos.getLevel().getPlayers().values()) {
            if (p.distanceSquared(pos) < 196) { // 14 blocks
                return true;
            }
        }
        return false;
    }
}
