package nukkitcoders.mobplugin.entities.spawners;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.EntityID;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import nukkitcoders.mobplugin.AutoSpawnTask;
import nukkitcoders.mobplugin.entities.autospawn.AbstractEntitySpawner;
import nukkitcoders.mobplugin.utils.Utils;

public class TropicalFishSpawner extends AbstractEntitySpawner {

    public TropicalFishSpawner(AutoSpawnTask spawnTask) {
        super(spawnTask);
    }

    public void spawn(Player player, Position pos, Level level) {
        if (Utils.rand(1, 3) != 1) {
            return;
        }
        final String blockId = level.getBlockIdAt((int) pos.x, (int) pos.y, (int) pos.z);
        if (blockId.equals(Block.WATER) || blockId.equals(Block.FLOWING_WATER)) {
            final int biomeId = level.getBiomeId((int) pos.x, (int) pos.y, (int) pos.z);
            if (biomeId == 0) {
                final String b = level.getBlockIdAt((int) pos.x, (int) (pos.y -1), (int) pos.z);
                if (b.equals(Block.WATER) || b.equals(Block.FLOWING_WATER)) {
                    for (int i = 0; i < Utils.rand(1, 3); i++) {
                        this.spawnTask.createEntity(EntityID.TROPICALFISH, pos.add(0, -1, 0));
                    }
                }
            }
        }
    }

    @Override
    public final int getEntityNetworkId() {
        return 108;
    }

    @Override
    public boolean isWaterMob() {
        return true;
    }
}
