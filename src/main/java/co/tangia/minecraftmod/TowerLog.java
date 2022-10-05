package co.tangia.minecraftmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ReflectionAccessFilter;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TowerLog {
    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(0,1,0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson gson = new GsonBuilder().addReflectionAccessFilter(ReflectionAccessFilter.BLOCK_INACCESSIBLE_JAVA).create();

    private TowerLog() {
    }

    public static void append(TowerData towerData) {
        executor.execute(()-> store(towerData));
    }

    private static void store(TowerData towerData) {
        try (var fw = new FileWriter("./tangia-tower.data", true)) {
            fw.write(gson.toJson(towerData));
        } catch (IOException e) {
            LOGGER.warn("couldn't store tower data", e);
        }
    }
}
