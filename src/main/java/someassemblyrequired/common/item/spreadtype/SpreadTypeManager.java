package someassemblyrequired.common.item.spreadtype;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.PacketDistributor;
import someassemblyrequired.SomeAssemblyRequired;
import someassemblyrequired.common.network.NetworkHandler;
import someassemblyrequired.common.network.SpreadTypeSyncPacket;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SpreadTypeManager extends SimpleJsonResourceReloadListener {

    public static final SpreadTypeManager INSTANCE = new SpreadTypeManager();

    private Map<ResourceLocation, SimpleSpreadType> spreadTypes = Collections.emptyMap();
    private Map<Item, SpreadType> spreadTypeLookup = Collections.emptyMap();

    private SpreadTypeManager() {
        super(new Gson(), SomeAssemblyRequired.MODID + "/spread_types");
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, SimpleSpreadType> spreadTypes = Maps.newHashMap();

        object.forEach((resourceLocation, element) -> {
            try {
                if (element.isJsonObject() && !net.minecraftforge.common.crafting.CraftingHelper.processConditions(element.getAsJsonObject(), "conditions")) {
                    SomeAssemblyRequired.LOGGER.debug("Skipping loading spread type {} as it's conditions were not met", resourceLocation);
                    return;
                }

                JsonObject jsonobject = GsonHelper.convertToJsonObject(element, "spread type");
                SimpleSpreadType spreadType = SimpleSpreadType.deserialize(jsonobject);
                if (spreadTypes.values().stream().anyMatch(s -> s.getIngredient() == spreadType.getIngredient())) {
                    SomeAssemblyRequired.LOGGER.warn("Multiple spread types found for item {}", spreadType.getIngredient());
                }
                spreadTypes.put(resourceLocation, spreadType);
            } catch (IllegalArgumentException | JsonParseException exception) {
                SomeAssemblyRequired.LOGGER.error("Parsing error loading custom spread type {}: {}", resourceLocation, exception.getMessage());
            }
        });
        SomeAssemblyRequired.LOGGER.info("Loaded {} spread types", spreadTypes.size());

        // boolean isServer = true;
        // try {
        //     LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER); // TODO dataWhateverEvent
        // } catch (Exception exception) {
        //     isServer = false;
        // }
        // if (isServer) {
        //     sendSyncingPackets();
        // }

        setSpreadTypes(spreadTypes);
    }

    @Nullable
    public SpreadType getSpreadType(Item item) {
        return spreadTypeLookup.get(item);
    }

    public boolean hasSpreadType(Item item) {
        return spreadTypeLookup.containsKey(item);
    }

    public void setSpreadTypes(Map<ResourceLocation, SimpleSpreadType> spreadTypes) {
        this.spreadTypes = spreadTypes;
        refreshSpreadTypeLookup();
    }

    private void refreshSpreadTypeLookup() {
        spreadTypeLookup = new HashMap<>();
        spreadTypeLookup.put(Items.POTION, new PotionSpreadType());
        for (SpreadType spreadType : spreadTypes.values()) {
            spreadTypeLookup.put(spreadType.getIngredient(), spreadType);
        }
    }

    private void sendSyncingPackets() {
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new SpreadTypeSyncPacket(spreadTypes));
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayer) {
            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getPlayer()), new SpreadTypeSyncPacket(spreadTypes));
        }
    }
}
