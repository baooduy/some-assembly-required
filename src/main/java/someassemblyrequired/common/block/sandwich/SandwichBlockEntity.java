package someassemblyrequired.common.block.sandwich;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import someassemblyrequired.SomeAssemblyRequired;
import someassemblyrequired.common.init.ModAdvancementTriggers;
import someassemblyrequired.common.init.ModBlockEntityTypes;
import someassemblyrequired.common.init.ModItems;
import someassemblyrequired.common.item.sandwich.SandwichItemHandler;

import javax.annotation.Nullable;

public class SandwichBlockEntity extends BlockEntity {

    private final SandwichItemHandler sandwich = new ItemHandler();
    private final LazyOptional<SandwichItemHandler> itemHandler = LazyOptional.of(() -> sandwich);

    public SandwichBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.SANDWICH.get(), pos, state);
    }

    private int getMaxHeight() {
        return 16; // TODO config
    }

    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.getItemInHand(InteractionHand.OFF_HAND).isEmpty() && player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
            removeItem(player);
            return InteractionResult.SUCCESS;
        } else {
            return addItem(player, hand);
        }
    }

    private void removeItem(Player player) {
        if (level == null || level.isClientSide()) {
            return;
        }

        ItemStack stack = sandwich.pop();
        BlockPos pos = getBlockPos();
        boolean isSpread = false; // TODO
        if (isSpread) {
            level.playSound(null, pos, SoundEvents.HONEY_BLOCK_BREAK, SoundSource.BLOCKS, 0.3F, 1.6F);
        } else {
            if (!player.isCreative()) {
                ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, stack);
                item.setPickUpDelay(5);
                level.addFreshEntity(item);
            }
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.3F, 1.6F);
        }

        if (sandwich.isEmpty()) {
            level.removeBlock(pos, false);
        } else {
            SandwichBlock.updateHeight(level, pos);
        }
    }

    private InteractionResult addItem(Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }
        ItemStack itemToAdd = player.getItemInHand(hand).copy();
        itemToAdd.setCount(1);
        if (itemToAdd.is(ModItems.SANDWICH.get())) {
            addSandwich(player, hand, itemToAdd);
            return InteractionResult.SUCCESS;
        }
        if (!itemToAdd.isEdible()) {
            return InteractionResult.PASS;
        }
        if (sandwich.size() >= getMaxHeight()) {
            player.displayClientMessage(new TranslatableComponent("message.%s.full_sandwich".formatted(SomeAssemblyRequired.MODID)), true);
            return InteractionResult.SUCCESS;
        }

        addSingleItem(player, hand, itemToAdd);
        if (getLevel() != null) {
            SandwichBlock.updateHeight(getLevel(), getBlockPos());
        }
        return InteractionResult.SUCCESS;
    }

    private void addSingleItem(Player player, InteractionHand hand, ItemStack stack) {
        if (level == null || level.isClientSide()) {
            return;
        }

        sandwich.add(stack);

        boolean hasSpread = false; // TODO
        if (!hasSpread) {
            level.playSound(null, getBlockPos(), SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.3F, 1.3F);
        } else {
            level.playSound(null, getBlockPos(), SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 0.3F, 1.3F);

            if (player instanceof ServerPlayer && stack.getItem() == Items.POTION && PotionUtils.getPotion(stack) != Potions.WATER) {
                ModAdvancementTriggers.ADD_POTION_TO_SANDWICH.trigger((ServerPlayer) player, stack);
            }
        }

        shrinkHeldItem(player, hand);
    }

    private void addSandwich(Player player, InteractionHand hand, ItemStack stack) {
        if (level == null || level.isClientSide()) {
            return;
        }
        SandwichItemHandler.get(stack).ifPresent(handler -> {
            if (!sandwich.canAdd(handler)) {
                player.displayClientMessage(new TranslatableComponent("message.%s.full_sandwich".formatted(SomeAssemblyRequired.MODID)), true);
            } else {
                sandwich.add(handler);
                shrinkHeldItem(player, hand);
                level.playSound(null, getBlockPos(), SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.3F, 1.3F);
            }
        });
    }

    private static void shrinkHeldItem(Player player, InteractionHand hand) {
        if (!player.isCreative()) {
            player.getItemInHand(hand).shrink(1);
            // TODO return spread type container
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        sandwich.deserializeNBT(tag.getList("Sandwich", Tag.TAG_COMPOUND));
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        tag.put("Sandwich", sandwich.serializeNBT());
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return itemHandler.cast();
        }
        return super.getCapability(capability, side);
    }

    private class ItemHandler extends SandwichItemHandler {

        @Override
        protected void onContentsChanged() {
            if (getLevel() instanceof ServerLevel level) {
                level.getChunkSource().blockChanged(getBlockPos());
                setChanged();
            }
        }
    }
}
