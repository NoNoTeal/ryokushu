package teal.drawme.modmenu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.IOUtils;
import org.spongepowered.include.com.google.common.base.Charsets;

import java.io.*;

public class Config {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final File config = FabricLoader.getInstance().getConfigDir().resolve("drawme.config").toFile();

    private double fps;

    private boolean displayPitch;

    private boolean doWatermark;
    private boolean validateSuggestions;

    private boolean titleScreenEasterEgg;
    private boolean titleScreenOnly;

    public Config() {
        this.fps = 30.0;
        this.displayPitch = false;
        this.doWatermark = false;
        this.validateSuggestions = false;
        this.titleScreenEasterEgg = false;
        this.titleScreenOnly = false;
    }

    public void setFps(double fps) {this.fps = fps;}
    public double getFps() {return fps;}

    // This is unused because in order to make this work, you need ONE text display entity instead of multiple.
    // Having one giant text_display however will limit how big your image can be.
    public boolean useDisplayPitch() {return displayPitch;}
    public void setDisplayPitch(boolean displayPitch) {this.displayPitch = displayPitch;}

    public boolean useTitleScreenEasterEgg() {return titleScreenEasterEgg;}
    public void setTitleScreenEasterEgg(boolean titleScreenEasterEgg) {this.titleScreenEasterEgg = titleScreenEasterEgg;}

    public boolean useTitleScreenOnly() {return titleScreenOnly;}
    public void setTitleScreenOnly(boolean titleScreenOnly) {this.titleScreenOnly = titleScreenOnly;}

    public boolean useWatermark() {return doWatermark;}
    public void setUseWatermark(boolean doWatermark) {this.doWatermark = doWatermark;}

    public boolean doValidateSuggestions() {return validateSuggestions;}
    public void setValidateSuggestions(boolean validateSuggestions) {this.validateSuggestions = validateSuggestions;}

    public void write() {
        try {
            Writer writer = new FileWriter(config);
            gson.toJson(this, writer);
            writer.flush();
            writer.close();
        } catch (IOException IOE) {
            IOE.printStackTrace();
        }
    }

    public static Config get(){
        Config obj = new Config();
        try {
            try {
                FileInputStream FIS = new FileInputStream(config);
                obj = gson.fromJson(IOUtils.toString(FIS, Charsets.UTF_8), Config.class);
                if(obj == null) {
                    obj = new Config();
                    throw new JsonSyntaxException("");
                }
            } catch (JsonSyntaxException e) {
                Writer writer = new FileWriter(config);
                gson.toJson(new Config(), writer);
                writer.flush();
                writer.close();
            }
        } catch (IOException e) {}
        return obj;
    }
}
