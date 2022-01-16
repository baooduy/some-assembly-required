package someassemblyrequired.data.ingredient;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import someassemblyrequired.SomeAssemblyRequired;
import vectorwing.farmersdelight.common.registry.ModItems;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public record FarmersDelightIngredients(Map<Item, IngredientBuilder> ingredients) {

    public static final List<Item> itemsWithCustomModel = Arrays.asList(
            ModItems.BACON.get(),
            ModItems.BEEF_PATTY.get(),
            ModItems.COOKED_BACON.get(),
            ModItems.FRIED_EGG.get(),
            ModItems.MIXED_SALAD.get(),
            ModItems.FRUIT_SALAD.get(),
            ModItems.NETHER_SALAD.get(),
            ModItems.COOKED_RICE.get(),
            ModItems.BEEF_STEW.get(),
            ModItems.CHICKEN_SOUP.get()
    );

    public void addIngredients() {
        builder(ModItems.VEGETABLE_SOUP.get()).setBowled().setSpread(0xa59136);
        builder(ModItems.TOMATO_SAUCE.get()).setBowled().setSpread(0xbe331f);

        builder(ModItems.MILK_BOTTLE.get()).setFullName("ingredient.%s.milk_bucket".formatted(SomeAssemblyRequired.MODID)).setBottled().setSpread(0xEEFDFF);
        builder(ModItems.HOT_COCOA.get()).setBottled().setSpread(0x7b4835);
        builder(ModItems.APPLE_CIDER.get()).setBottled().setSpread(0xbd783d);
        builder(ModItems.MELON_JUICE.get()).setBottled().setSpread(0xc73225);

        builder(ModItems.CABBAGE_LEAF.get()).setCustomDisplayName();
        builder(ModItems.FRIED_EGG.get()).setCustomDisplayName();
        builder(ModItems.BEEF_PATTY.get()).setCustomDisplayName();
        builder(ModItems.COOKED_CHICKEN_CUTS.get()).setCustomDisplayName();
        builder(ModItems.COOKED_BACON.get()).setCustomDisplayName();
        builder(ModItems.COOKED_COD_SLICE.get()).setCustomDisplayName();
        builder(ModItems.COOKED_SALMON_SLICE.get()).setCustomDisplayName();
        builder(ModItems.COOKED_MUTTON_CHOPS.get()).setCustomDisplayName();

        builder(ModItems.COOKED_RICE.get()).setBowled().setCustomDisplayName();

        builder(ModItems.MIXED_SALAD.get()).setBowled().setSound(SoundEvents.AZALEA_LEAVES_PLACE);
        builder(ModItems.FRUIT_SALAD.get()).setBowled();
        builder(ModItems.NETHER_SALAD.get()).setBowled();
        builder(ModItems.BEEF_STEW.get()).setBowled();
        builder(ModItems.CHICKEN_SOUP.get()).setBowled();
    }

    private IngredientBuilder builder(Item item) {
        if (ingredients.containsKey(item)) {
            return ingredients.get(item);
        }
        IngredientBuilder builder = new IngredientBuilder(item);
        ingredients.put(item, builder);
        return builder;
    }
}
