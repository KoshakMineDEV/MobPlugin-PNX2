package nukkitcoders.mobplugin.entities.spawners;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.EntityID;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import nukkitcoders.mobplugin.AutoSpawnTask;
import nukkitcoders.mobplugin.MobPlugin;
import nukkitcoders.mobplugin.entities.animal.swimming.Dolphin;
import nukkitcoders.mobplugin.entities.autospawn.AbstractEntitySpawner;
import nukkitcoders.mobplugin.utils.Utils;

public class DolphinSpawner extends AbstractEntitySpawner {

    public DolphinSpawner(AutoSpawnTask spawnTask) {
        super(spawnTask);
    }

    public void spawn(Player player, Position pos, Level level) {
        if (Utils.rand(1, 3) != 1) {
            return;
        }
        final int biomeId = level.getBiomeId((int) pos.x, (int) pos.y, (int) pos.z);
        final String blockId = level.getBlockIdAt((int) pos.x, (int) pos.y, (int) pos.z);
        if ((blockId == Block.WATER || blockId == Block.FLOWING_WATER) && (biomeId == 0 || biomeId == 24)) {
            if (MobPlugin.isAnimalSpawningAllowedByTime(level)) {
                final String b = level.getBlockIdAt((int) pos.x, (int) (pos.y -1), (int) pos.z);
                if (b == Block.WATER || b == Block.FLOWING_WATER) {
                    for (int i = 0; i < Utils.rand(1, 3); i++) {
                        var entity = this.spawnTask.createEntity(EntityID.DOLPHIN, pos.add(0, -1, 0));
                        if (entity == null) return;
                        if (Utils.rand(1, 10) == 1) {
                            entity.setBaby(true);
                        }
                    }
                }
            }
        }
    }

    @Override
    public final int getEntityNetworkId() {
        return Dolphin.NETWORK_ID;
    }

    @Override
    public boolean isWaterMob() {
        return true;
    }
}
