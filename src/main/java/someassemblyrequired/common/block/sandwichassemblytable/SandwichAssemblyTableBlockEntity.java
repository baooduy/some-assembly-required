package someassemblyrequired.common.block.sandwichassemblytable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.EmptyHandler;
import someassemblyrequired.common.block.ItemHandlerBlockEntity;
import someassemblyrequired.common.init.ModBlockEntityTypes;
import someassemblyrequired.common.init.ModItems;
import someassemblyrequired.common.init.ModTags;
import someassemblyrequired.common.item.spreadtype.SpreadType;
import someassemblyrequired.common.item.spreadtype.SpreadTypeManager;

import javax.annotation.Nullable;
import java.util.List;

public class SandwichAssemblyTableBlockEntity extends ItemHandlerBlockEntity {

    /**
     * An item handler that contains the table's contents as a sandwich when valid
     * Ingredients can still be inserted in slot 0
     */
    private final SandwichItemHandler sandwichInventory = createSandwichItemHandler();
    private final LazyOptional<SandwichItemHandler> sandwichItemHandler = LazyOptional.of(() -> sandwichInventory);

    public SandwichAssemblyTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.SANDWICH_ASSEMBLY_TABLE.get(), pos, state, 16);
    }

    @Override
    protected boolean canModifyItems() {
        return false;
    }

    private ItemStack createSpreadItem(SpreadType spreadType, ItemStack ingredient) {
        CompoundTag spreadNBT = new CompoundTag();
        spreadNBT.putInt("Color", spreadType.getColor(ingredient));
        spreadNBT.putBoolean("HasEffect", ingredient.hasFoil());
        spreadNBT.put("Ingredient", ingredient.copy().split(1).save(new CompoundTag()));

        List<ItemStack> ingredients = getItems();
        for (int slot = ingredients.size() - 1; slot >= 0; slot--) {
            if (ModTags.SANDWICH_BREADS.contains(ingredients.get(slot).getItem())) {
                if (ModTags.BREAD.contains(ingredients.get(slot).getItem())) {
                    spreadNBT.putBoolean("IsOnLoaf", true);
                }
                break;
            }
        }

        ItemStack spread = new ItemStack(ModItems.SPREAD.get());
        spread.setTag(spreadNBT);
        return spread;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && side == null) {
            return sandwichItemHandler.cast();
        }
        return super.getCapability(capability, side);
    }

    public ItemStack removeTopIngredient() {
        if (getAmountOfItems() > 0) {
            return getInventory().extractItem(getAmountOfItems() - 1, 1, false);
        }
        return ItemStack.EMPTY;
    }

    public boolean hasBreadAsTopIngredient() {
        if (getAmountOfItems() > 0) {
            return ModTags.SANDWICH_BREADS.contains(getInventory().getStackInSlot(getAmountOfItems() - 1).getItem());
        }
        return false;
    }

    public int getInventorySize() {
        return getInventory().getSlots();
    }

    /**
     * Try to add an ingredient to the sandwich table, converting it into a spread when possible. Does not modify the specified itemStack
     *
     * @return whether the ingredient successfully got added
     */
    public boolean addIngredient(ItemStack stack) {
        SpreadType spreadType = SpreadTypeManager.INSTANCE.getSpreadType(stack.getItem());
        ItemStack ingredient = spreadType == null ? stack.copy() : createSpreadItem(spreadType, stack);

        int nextEmptySlot = getAmountOfItems();
        if (nextEmptySlot >= getInventorySize() || !getInventory().isItemValid(nextEmptySlot, ingredient)) {
            return false;
        }
        int count = getInventory().insertItem(nextEmptySlot, ingredient, false).getCount();
        return count != ingredient.getCount();
    }

    @Override
    protected void onContentsUpdated() {
        // check whether the list of ingredients are valid as a sandwich and update the sandwich item handler
        sandwichInventory.update();
    }

    private SandwichItemHandler createSandwichItemHandler() {
        return new SandwichItemHandler();
    }

    @Override
    protected BlockEntityItemHandler createItemHandler(int size) {
        return new IngredientHandler(size);
    }

    private class SandwichItemHandler implements IItemHandler {

        // contains the table's ingredients as a sandwich when valid
        private ItemStack sandwich = ItemStack.EMPTY;

        /**
         * Checks whether the ingredient handler contains a valid sandwich and updates the sandwich handler accordingly
         */
        public void update() {
            int amountOfItems = getAmountOfItems();

            // there must be at least 1 item and the top item must be bread
            if (amountOfItems < 2 || !ModTags.SANDWICH_BREADS.contains(getInventory().getStackInSlot(amountOfItems - 1).getItem())) {
                sandwich = ItemStack.EMPTY;
            } else {
                sandwich = new ItemStack(ModItems.SANDWICH.get());
                saveAdditional(sandwich.getOrCreateTagElement("BlockEntityTag"));
            }
            System.out.println(sandwich.getItem().getRegistryName());
            System.out.println(sandwich.getTag());
        }

        protected void validateSlotIndex(int slot) {
            if (slot != 0) {
                throw new RuntimeException("Slot " + slot + " not in valid range - [0,1)");
            }
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            validateSlotIndex(slot);
            return sandwich;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (sandwich.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            validateSlotIndex(slot);

            ItemStack result = sandwich;

            if (!simulate) {
                removeItems();
            }

            return result;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // check if the stack can be inserted in the regular item handler in the next available slot
            return slot == 0 && getInventory().isItemValid(getAmountOfItems(), stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            validateSlotIndex(slot);
            // insert the stack in the regular item handler in the next available slot
            return getInventory().insertItem(getAmountOfItems(), stack, simulate);
        }
    }

    private class IngredientHandler extends BlockEntityItemHandler {

        private IngredientHandler(int size) {
            super(size);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public NonNullList<ItemStack> removeItems() {
            NonNullList<ItemStack> result = NonNullList.create();
            for (ItemStack ingredient : super.removeItems()) {
                if (ingredient.getItem() != ModItems.SPREAD.get()) {
                    ingredient.removeTagKey("IsOnSandwich");
                    result.add(ingredient);
                }
            }
            return result;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            validateSlotIndex(slot);
            if (slot < getSlots() - 1 && !getStackInSlot(slot + 1).isEmpty()) {
                return ItemStack.EMPTY;
            }

            ItemStack result = super.extractItem(slot, amount, simulate);
            // spreads cannot be obtained as items
            if (result.getItem() == ModItems.SPREAD.get()) {
                return ItemStack.EMPTY;
            }

            result.removeTagKey("IsOnSandwich");

            return result;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot != 0 && getStackInSlot(slot - 1).isEmpty() // the previous slot must have an item
                    || slot >= getSlots() || !getStackInSlot(slot).isEmpty()) { // the specified slot must be empty
                return false;
            }

            if (stack.getItem() == ModItems.SANDWICH.get()) {
                IItemHandler itemHandler = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).orElse(EmptyHandler.INSTANCE);

                int sandwichSize;
                for (sandwichSize = 0; sandwichSize < itemHandler.getSlots() && !itemHandler.getStackInSlot(sandwichSize).isEmpty(); sandwichSize++)
                    ;

                return sandwichSize > 0 && sandwichSize <= getSlots() - getAmountOfItems();
            }

            return (stack.isEdible() || stack.getItem() == ModItems.SPREAD.get() || SpreadTypeManager.INSTANCE.hasSpreadType(stack.getItem())) // the item must be edible
                    && (slot > 0 || ModTags.SANDWICH_BREADS.contains(stack.getItem())); // the first item must be bread
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            if (!isItemValid(slot, stack)) {
                return stack;
            }

            validateSlotIndex(slot);

            // copy the sandwich's ingredients
            if (stack.getItem() == ModItems.SANDWICH.get()) {
                stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(sandwichItemHandler -> {
                    int slotOffset = getAmountOfItems();
                    for (int sandwichSlot = 0; sandwichSlot < sandwichItemHandler.getSlots() && !sandwichItemHandler.getStackInSlot(sandwichSlot).isEmpty(); sandwichSlot++) {
                        setStackInSlot(slotOffset + sandwichSlot, sandwichItemHandler.getStackInSlot(sandwichSlot));
                    }
                });
                return ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - 1);
            }

            // convert ingredients to spreads when possible
            SpreadType spreadType = SpreadTypeManager.INSTANCE.getSpreadType(stack.getItem());
            ItemStack ingredient = spreadType == null ? stack.copy() : createSpreadItem(spreadType, stack);

            ingredient.getOrCreateTag().putBoolean("IsOnSandwich", true);

            // spawn the spread's container as an item
            if (!simulate && spreadType != null && SandwichAssemblyTableBlockEntity.this.getLevel() != null && spreadType.hasContainer(ingredient)) {
                ItemEntity item = new ItemEntity(SandwichAssemblyTableBlockEntity.this.getLevel(), SandwichAssemblyTableBlockEntity.this.worldPosition.getX() + 0.5, SandwichAssemblyTableBlockEntity.this.worldPosition.getY() + 1.2, SandwichAssemblyTableBlockEntity.this.worldPosition.getZ() + 0.5, new ItemStack(spreadType.getContainer(ingredient)));
                item.setPickUpDelay(5);
                SandwichAssemblyTableBlockEntity.this.getLevel().addFreshEntity(item);
            }

            return ItemHandlerHelper.copyStackWithSize(stack, super.insertItem(slot, ingredient, simulate).getCount());
        }
    }
}