package co.tangia.minecraftmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ReflectionAccessFilter;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ModPersistence {
    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(0, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson gson = new GsonBuilder().addReflectionAccessFilter(ReflectionAccessFilter.BLOCK_INACCESSIBLE_JAVA).create();
    private static final String fileName = "./tangia-persistence.json";

    static ModPersistenceData data = new ModPersistenceData(new HashMap<>());

    private ModPersistence() {
    }

    public static void store() {
        executor.execute(ModPersistence::storeData);
    }

    private static void storeData() {
        try (var fw = new FileWriter(fileName, false)) {
            fw.write(gson.toJson(data));
        } catch (IOException e) {
            LOGGER.warn("couldn't store data", e);
        }
    }

    public static void load() {
        try (var fr = new FileReader(fileName)) {
            data = gson.fromJson(fr, ModPersistenceData.class);
            if (data == null) {
                data = new ModPersistenceData(new HashMap<>());
            }
            if (data.sessions() == null) {
                data.setSessions(new HashMap<>());
            }
        } catch (FileNotFoundException e) {
            LOGGER.info("no data to load - starting with clean state");
        } catch (IOException e) {
            LOGGER.warn("couldn't load data, this is normal on first start", e);
        }
    }
}
