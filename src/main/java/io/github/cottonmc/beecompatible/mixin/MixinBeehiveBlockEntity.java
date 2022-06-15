package io.github.cottonmc.beecompatible.mixin;

import io.github.cottonmc.beecompatible.api.BeeTimeCheckCallback;
import io.github.cottonmc.beecompatible.api.BeeWeatherCheckCallback;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.EntityTypeTags;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(value = BeehiveBlockEntity.class, priority = 900)
public abstract class MixinBeehiveBlockEntity extends BlockEntity implements Tickable {

    @Shadow
    private BlockPos flowerPos;

    public MixinBeehiveBlockEntity(BlockEntityType<?> type) {
        super(type);
    }

    @Shadow
    public static int getHoneyLevel(BlockState state) {
        return 0;
    }

    @Shadow
    protected abstract boolean hasFlowerPos();

    @Shadow
    protected abstract void ageBee(int ticks, BeeEntity bee);

    /**
     * @author 12emin34
     * @reason overwriting this until I find a better way
     */
    @Overwrite
    private boolean releaseBee(BlockState state, BeehiveBlockEntity.Bee bee, @Nullable List<Entity> list, BeehiveBlockEntity.BeeState beeState) {
        boolean timeEvent = world.isNight();
        CompoundTag entityData = bee.entityData;
        Entity entity1 = EntityType.loadEntityWithPassengers(entityData, world, e -> e);
        if (entity1 instanceof BeeEntity) {
            BeeEntity beeEntity = (BeeEntity) entity1;
            TriState result = BeeTimeCheckCallback.EVENT.invoker().checkTime(world, beeEntity);
            if (result != TriState.DEFAULT) timeEvent = !result.get(); //a negative here allows bees to exit
        }

        boolean weatherEvent = world.isRaining();
        if (entity1 instanceof BeeEntity) {
            BeeEntity beeEntity = (BeeEntity) entity1;
            TriState result = BeeWeatherCheckCallback.EVENT.invoker().checkWeather(world, beeEntity);
            if (result != TriState.DEFAULT) weatherEvent = !result.get(); //a negative here allows bees to exit
        }

        if ((timeEvent || weatherEvent) && beeState != BeehiveBlockEntity.BeeState.EMERGENCY) {
            return false;
        } else {
            BlockPos blockPos = this.getPos();
            CompoundTag compoundTag = bee.entityData;
            compoundTag.remove("Passengers");
            compoundTag.remove("Leash");
            compoundTag.remove("UUID");
            Direction direction = state.get(BeehiveBlock.FACING);
            BlockPos blockPos2 = blockPos.offset(direction);
            boolean bl = !this.world.getBlockState(blockPos2).getCollisionShape(this.world, blockPos2).isEmpty();
            if (bl && beeState != BeehiveBlockEntity.BeeState.EMERGENCY) {
                return false;
            } else {
                Entity entity = EntityType.loadEntityWithPassengers(compoundTag, this.world, (entityx) -> {
                    return entityx;
                });
                if (entity != null) {
                    if (!entity.getType().isIn(EntityTypeTags.BEEHIVE_INHABITORS)) {
                        return false;
                    } else {
                        if (entity instanceof BeeEntity) {
                            BeeEntity beeEntity = (BeeEntity) entity;
                            if (this.hasFlowerPos() && !beeEntity.hasFlower() && this.world.random.nextFloat() < 0.9F) {
                                beeEntity.setFlowerPos(this.flowerPos);
                            }

                            if (beeState == BeehiveBlockEntity.BeeState.HONEY_DELIVERED) {
                                beeEntity.onHoneyDelivered();
                                if (state.getBlock().isIn(BlockTags.BEEHIVES)) {
                                    int i = getHoneyLevel(state);
                                    if (i < 5) {
                                        int j = this.world.random.nextInt(100) == 0 ? 2 : 1;
                                        if (i + j > 5) {
                                            --j;
                                        }

                                        this.world.setBlockState(this.getPos(), state.with(BeehiveBlock.HONEY_LEVEL, i + j));
                                    }
                                }
                            }

                            this.ageBee(bee.ticksInHive, beeEntity);
                            if (list != null) {
                                list.add(beeEntity);
                            }

                            float f = entity.getWidth();
                            double d = bl ? 0.0 : 0.55 + (double) (f / 2.0F);
                            double e = (double) blockPos.getX() + 0.5 + d * (double) direction.getOffsetX();
                            double g = (double) blockPos.getY() + 0.5 - (double) (entity.getHeight() / 2.0F);
                            double h = (double) blockPos.getZ() + 0.5 + d * (double) direction.getOffsetZ();
                            entity.refreshPositionAndAngles(e, g, h, entity.yaw, entity.pitch);
                        }

                        this.world.playSound(null, blockPos, SoundEvents.BLOCK_BEEHIVE_EXIT, SoundCategory.BLOCKS, 1.0F, 1.0F);
                        return this.world.spawnEntity(entity);
                    }
                } else {
                    return false;
                }
            }
        }
    }

}
