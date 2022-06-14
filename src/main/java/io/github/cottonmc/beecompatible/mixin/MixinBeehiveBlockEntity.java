package io.github.cottonmc.beecompatible.mixin;

import io.github.cottonmc.beecompatible.api.BeeTimeCheckCallback;
import io.github.cottonmc.beecompatible.api.BeeWeatherCheckCallback;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tickable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(BeehiveBlockEntity.class)
public abstract class MixinBeehiveBlockEntity extends BlockEntity implements Tickable {

    public MixinBeehiveBlockEntity(BlockEntityType<?> type) {
        super(type);
    }

    @Inject(method = "releaseBee", at = @At(value = "TAIL"), cancellable = true, locals = LocalCapture.CAPTURE_FAILSOFT)
    private void timeEvent(BlockState state, BeehiveBlockEntity.Bee bee, List<Entity> list, BeehiveBlockEntity.BeeState beeState, CallbackInfoReturnable<Boolean> cir) {
        CompoundTag compoundTag = bee.entityData;
        Entity entity = EntityType.loadEntityWithPassengers(compoundTag, world, e -> e);
        if (entity instanceof BeeEntity) {
            BeeEntity beeEntity = (BeeEntity) entity;
            TriState result = BeeTimeCheckCallback.EVENT.invoker().checkTime(world, beeEntity);
            if (result != TriState.DEFAULT) cir.setReturnValue(!result.get()); //a negative here allows bees to exit
        }
        cir.setReturnValue(world.isNight());
    }

    @Inject(method = "releaseBee", at = @At(value = "TAIL"), cancellable = true, locals = LocalCapture.CAPTURE_FAILSOFT)
    private void weatherEvent(BlockState state, BeehiveBlockEntity.Bee bee, List<Entity> list, BeehiveBlockEntity.BeeState beeState, CallbackInfoReturnable<Boolean> cir) {
        CompoundTag compoundTag = bee.entityData;
        Entity entity = EntityType.loadEntityWithPassengers(compoundTag, world, e -> e);
        if (entity instanceof BeeEntity) {
            BeeEntity beeEntity = (BeeEntity) entity;
            TriState result = BeeWeatherCheckCallback.EVENT.invoker().checkWeather(world, beeEntity);
            if (result != TriState.DEFAULT) cir.setReturnValue(!result.get()); //a negative here allows bees to exit
        }
        cir.setReturnValue(world.isRaining());
    }
}
