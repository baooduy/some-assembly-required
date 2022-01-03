package someassemblyrequired.common.ingredient;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import someassemblyrequired.SomeAssemblyRequired;

import javax.annotation.Nullable;
import java.util.HashMap;

public class CustomIngredients {

    private static final HashMap<Item, SandwichIngredient> INGREDIENTS = new HashMap<>();

    private static final SandwichIngredient DEFAULT_BEHAVIOR = new SandwichIngredient() {
    };

    public static SandwichIngredient get(ItemStack item) {
        return INGREDIENTS.get(item.getItem());
    }

    public static SandwichIngredient getOrDefault(ItemStack item) {
        if (get(item) != null) {
            return get(item);
        }
        return DEFAULT_BEHAVIOR;
    }

    public static void put(Item item, SandwichIngredient properties) {
        if (INGREDIENTS.get(item) != null) {
            SomeAssemblyRequired.LOGGER.error("Multiple ingredients for item " + item.getRegistryName());
        } else {
            INGREDIENTS.put(item, properties);
        }
    }

    public static FoodProperties getFood(ItemStack item) {
        return getOrDefault(item).getFood(item); // TODO
    }

    public static void onEaten(ItemStack item, Player player) {
        getOrDefault(item).onEaten(item, player); // TODO
    }

    public static Component getDisplayName(ItemStack item) {
        return getOrDefault(item).getDisplayName(item);
    }

    public static ItemStack getDisplayItem(ItemStack item) {
        return getOrDefault(item).getDisplayItem(item);
    }

    public static ItemStack getContainer(ItemStack item) {
        return getOrDefault(item).getContainer(item);
    }

    public static boolean hasContainer(ItemStack item) {
        return !getContainer(item).isEmpty();
    }

    public static void playApplySound(ItemStack item, Level level, @Nullable Player player, BlockPos pos) {
        getOrDefault(item).playApplySound(item, level, player, pos);
    }

    public static void playRemoveSound(ItemStack item, Level level, @Nullable Player player, BlockPos pos) {
        getOrDefault(item).playRemoveSound(item, level, player, pos);
    }
}
