package nukkitcoders.mobplugin.entities.animal.walking;

import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.EntitySmite;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.IChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import nukkitcoders.mobplugin.entities.HorseBase;
import nukkitcoders.mobplugin.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:kniffman@googlemail.com">Michael Gertz</a>
 */
public class SkeletonHorse extends HorseBase implements EntitySmite {

    public static final int NETWORK_ID = 26;

    public SkeletonHorse(IChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public float getWidth() {
        if (this.isBaby()) {
            return 0.6982f;
        }
        return 1.3965f;
    }

    @Override
    public float getHeight() {
        if (this.isBaby()) {
            return 0.8f;
        }
        return 1.6f;
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(15);
    }

    @Override
    public Item[] getDrops() {
        List<Item> drops = new ArrayList<>();

        if (!this.isBaby()) {
            drops.add(Item.get(Item.LEATHER, 0, Utils.rand(0, 2)));
            drops.add(Item.get(Item.BONE, 0, Utils.rand(0, 1)));
        }

        if (this.isSaddled()) {
            drops.add(Item.get(Item.SADDLE, 0, 1));
        }

        return drops.toArray(new Item[0]);
    }

    @Override
    public String getName() {
        return this.hasCustomName() ? this.getNameTag() : "Skeleton Horse";
    }

    @Override
    public boolean isFeedItem(Item item) {
        return false;
    }

    @Override
    public boolean targetOption(EntityCreature creature, double distance) {
        return false;
    }
}
