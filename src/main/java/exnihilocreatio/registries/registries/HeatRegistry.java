package exnihilocreatio.registries.registries;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import exnihilocreatio.json.CustomBlockInfoJson;
import exnihilocreatio.registries.manager.ExNihiloRegistryManager;
import exnihilocreatio.registries.manager.IHeatDefaultRegistryProvider;
import exnihilocreatio.util.BlockInfo;
import net.minecraft.item.ItemStack;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class HeatRegistry {

    private static Map<BlockInfo, Integer> registry = new HashMap<>();
    private static Map<BlockInfo, Integer> externalRegistry = new HashMap<>();

    private static Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(BlockInfo.class, new CustomBlockInfoJson()).create();

    public static void register(BlockInfo info, int heatAmount) {
        registerInternal(info, heatAmount);
        externalRegistry.put(info, heatAmount);
    }

    public static void register(ItemStack stack, int heatAmount) {
        register(new BlockInfo(stack), heatAmount);
    }

    public static void registerInternal(BlockInfo info, int heatAmount) {
        registry.put(info, heatAmount);
    }

    public static void registerInternal(ItemStack stack, int heatAmount) {
        register(new BlockInfo(stack), heatAmount);
    }

    public static void registerDefaults() {
        for (IHeatDefaultRegistryProvider provider : ExNihiloRegistryManager.getDefaultHeatRecipeHandlers()) {
            provider.registerHeatRecipeDefaults();
        }
    }

    public static int getHeatAmount(ItemStack stack) {
        return registry.get(new BlockInfo(stack));
    }

    public static int getHeatAmount(BlockInfo info) {
        if (registry.containsKey(info))
            return registry.get(info);

        return 0;
    }

    public static void loadJson(File file) {
        registry.clear();


        if (file.exists()) {
            try {
                FileReader fr = new FileReader(file);
                HashMap<String, Integer> gsonInput = gson.fromJson(fr, new TypeToken<HashMap<String, Integer>>() {
                }.getType());

                for (Map.Entry<String, Integer> entry : gsonInput.entrySet()) {
                    registry.put(new BlockInfo(entry.getKey()), entry.getValue());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            registerDefaults();
            saveJson(file);
        }

        registry.putAll(externalRegistry);
    }

    public static void saveJson(File file) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(file);
            gson.toJson(registry, fw);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(fw);
        }
    }

}