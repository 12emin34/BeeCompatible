package io.github.cottonmc.beecompatible.mixin;

import io.github.cottonmc.beecompatible.api.BeeTags;
import io.github.cottonmc.beecompatible.api.BeeTimeCheckCallback;
import io.github.cottonmc.beecompatible.api.BeeWeatherCheckCallback;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.item.Item;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.Tag;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

@Mixin(BeeEntity.class)
public abstract class MixinBeeEntity extends LivingEntity {
    protected MixinBeeEntity(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }

    @Redirect(method = "canEnterHive", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isNight()Z"))
    private boolean timeEvent(World world) {
        TriState result = BeeTimeCheckCallback.EVENT.invoker().checkTime(world, (BeeEntity) (Object) this);
        if (result != TriState.DEFAULT) return result.get();
        else return world.isNight();
    }

    @Redirect(method = "canEnterHive", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isRaining()Z"))
    private boolean weatherEvent(World world) {
        TriState result = BeeWeatherCheckCallback.EVENT.invoker().checkWeather(world, (BeeEntity) (Object) this);
        if (result != TriState.DEFAULT) return result.get();
        else return world.isRaining();
    }

    @ModifyArg(method = "initGoals", at = @At(value = "INVOKE", target = "Lnet/minecraft/recipe/Ingredient;fromTag(Lnet/minecraft/tag/Tag;)Lnet/minecraft/recipe/Ingredient;"))
    private Tag<Item> modTemptTag(Tag<Item> original) {
        return BeeTags.BEE_TEMPTING;
    }

    @ModifyArg(method = "isFlowers", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;isIn(Lnet/minecraft/tag/Tag;)Z"))
    private Tag<Block> modFeedTag(Tag<Block> original) {
        return BeeTags.BEE_FEEDING;
    }

    @Mixin(targets = "net.minecraft.entity.passive.BeeEntity$PollinateGoal")
    public static abstract class MixinPollinateGoal {
        @Shadow
        @Final
        BeeEntity field_20377;

        @Shadow
        @Final
        @Mutable
        private Predicate<BlockState> flowerPredicate = (blockState) -> {
            if (blockState.isIn(BlockTags.TALL_FLOWERS)) {
                if (blockState.isOf(Blocks.SUNFLOWER)) {
                    return blockState.get(TallPlantBlock.HALF) == DoubleBlockHalf.UPPER;
                } else {
                    return true;
                }
            } else {
                return blockState.isIn(BeeTags.BEE_FEEDING);
            }
        };

        @Redirect(method = "canBeeStart", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isRaining()Z"))
        private boolean weatherEventStart(World world) {
            TriState result = BeeWeatherCheckCallback.EVENT.invoker().checkWeather(world, field_20377);
            if (result != TriState.DEFAULT) return !result.get(); //a negative here allows bees to start pollinating
            else return world.isRaining();
        }

        @Redirect(method = "canBeeContinue", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isRaining()Z"))
        private boolean weatherEventContinue(World world) {
            TriState result = BeeWeatherCheckCallback.EVENT.invoker().checkWeather(world, field_20377);
            if (result != TriState.DEFAULT) return !result.get(); //a negative here allows bees to continue pollinating
            else return world.isRaining();
        }
    }
}
