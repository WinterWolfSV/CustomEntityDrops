package winterwolfsv.custom_entity_drops.hash_map_handling;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SystemDetails;
import winterwolfsv.custom_entity_drops.DropSaver;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static winterwolfsv.custom_entity_drops.CED.LOGGER;
import static winterwolfsv.custom_entity_drops.CED.customEntityDrop;


public class HashMapHandler {

    private static final String filePath = "config/custom_entity_drops/data.json";

    public static int saveHashMap() {
        if (customEntityDrop == null) {
            customEntityDrop = new ArrayList<>();
        }
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(customEntityDrop);
            System.out.println(json);

            FileWriter fileWriter = new FileWriter(filePath);
            fileWriter.write(json);
            fileWriter.close();
            

            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static int loadHashMap() {
        if (customEntityDrop == null) {
            customEntityDrop = new ArrayList<>();
        }
        StringBuilder JSONString = new StringBuilder();
        try {
            FileReader fileReader = new FileReader(filePath);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = bufferedReader.readLine();
            while (line != null) {
                JSONString.append(line);
                line = bufferedReader.readLine();
            }
        } catch (IOException e) {
            LOGGER.info("Failed to load data.json. Creating new file.");
            if (createSaveFile()) {
                LOGGER.info("Successfully created data.json.");
            }
        }
        try {


            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Type listType = new TypeToken<List<DropSaver>>() {
            }.getType();
            customEntityDrop = gson.fromJson(JSONString.toString(), listType);
            System.out.println(customEntityDrop);
            return 1;
        } catch (Exception e) {
            LOGGER.info("Incorrect data in data.json. Creating new file.");
            e.printStackTrace();
            if (createSaveFile()) {
                LOGGER.info("Successfully created new data.json.");
            }
            return 0;
        }
    }

    private static boolean createSaveFile() {
        if (customEntityDrop == null) {
            customEntityDrop = new ArrayList<>();
        }
        try {
            File file = new File(filePath);
            if (file.exists()) {
                LOGGER.info("File exists, deleting.");
                FileWriter fileWriter = new FileWriter(file, false);

                fileWriter.close();
            }
            file.getParentFile().mkdirs();
            file.createNewFile();
            return true;
        } catch (IOException e) {
            LOGGER.info("Custom Entity Drops failed to create data.json. Stacktrace below:");
            e.printStackTrace();
            return false;
        }
    }


}
