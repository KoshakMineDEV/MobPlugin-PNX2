package nukkitcoders.mobplugin.entities.projectile;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.projectile.EntityProjectile;
import cn.nukkit.level.format.IChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import org.jetbrains.annotations.NotNull;

public class EntityLlamaSpit extends EntityProjectile {

    public static final int NETWORK_ID = 102;

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public float getWidth() {
        return 0.3f;
    }

    @Override
    public float getHeight() {
        return 0.3f;
    }

    @Override
    public float getGravity() {
        return 0.001f;
    }

    @Override
    public float getDrag() {
        return 0.01f;
    }

    @Override
    protected double getBaseDamage() {
        return 1;
    }

    public EntityLlamaSpit(IChunk chunk, CompoundTag nbt) {
        this(chunk, nbt, null);
    }

    @Override
    public @NotNull String getIdentifier() {
        return LLAMA_SPIT;
    }

    public EntityLlamaSpit(IChunk chunk, CompoundTag nbt, Entity shootingEntity) {
        super(chunk, nbt, shootingEntity);
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        if (this.age > 100 || this.isCollided || this.hadCollision) {
            this.close();
            return false;
        }

        super.onUpdate(currentTick);
        return !this.closed;
    }
}
