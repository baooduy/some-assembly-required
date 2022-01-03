package someassemblyrequired.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import someassemblyrequired.SomeAssemblyRequired;
import someassemblyrequired.common.block.sandwich.SandwichBlockEntity;
import someassemblyrequired.common.init.ModItems;
import someassemblyrequired.common.init.ModTags;
import someassemblyrequired.common.item.sandwich.SandwichItemHandler;

public class SandwichAssemblyTableBlock extends HorizontalDirectionalBlock {

    public SandwichAssemblyTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection());
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos.above()) instanceof SandwichBlockEntity blockEntity) {
            return blockEntity.interact(player, hand);
        } else {
            return tryPlaceSandwich(player, hand, pos, hitResult);
        }
    }

    private static InteractionResult tryPlaceSandwich(Player player, InteractionHand hand, BlockPos pos, BlockHitResult hitResult) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !SandwichBlockEntity.isValidItem(stack)) {
            return InteractionResult.PASS;
        }

        if (!stack.is(ModItems.SANDWICH.get())) {
            if (!stack.is(ModTags.BREAD_SLICES)) {
                player.displayClientMessage(new TranslatableComponent("message.%s.bottom_bread".formatted(SomeAssemblyRequired.MODID)), true);
                return InteractionResult.SUCCESS;
            }
            stack = new SandwichItemHandler(stack.split(1)).getAsItem();
        }

        UseOnContext useOnContext = new UseOnContext(player, hand, hitResult);
        return ModItems.SANDWICH.get().place(useOnContext, pos.above(), stack);
    }
}