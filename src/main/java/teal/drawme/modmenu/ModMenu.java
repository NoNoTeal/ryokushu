package teal.drawme.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;
import teal.drawme.Drawme;

public class ModMenu implements ModMenuApi {

    private static final Config VirginConfig = new Config();

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (parent) -> {
            ConfigBuilder cb = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Drawme Config"))
                .setSavingRunnable(Drawme.config::write);

            ConfigCategory options = cb.getOrCreateCategory(Text.literal("Me"));
            ConfigEntryBuilder ceb = cb.entryBuilder();
            options.addEntry(
                ceb.startDoubleField(Text.literal("FPS"), Drawme.config.getFps())
                    .setMin(0.1)
                    .setDefaultValue(VirginConfig.getFps())
                    .setTooltip(Text.literal("Sets the default playback speed for things, i.e. title screen video."))
                    .setSaveConsumer(Drawme.config::setFps)
                    .build()
            );

            // options.addEntry(
            //     ceb.startBooleanToggle(Text.literal("Display Text Pitch"), Drawme.config.useDisplayPitch())
            //         .setDefaultValue(VirginConfig.useDisplayPitch())
            //         .setTooltip(Text.literal("Whether to angle the display text entity or keep it straight."))
            //         .setSaveConsumer(Drawme.config::setDisplayPitch)
            //         .build()
            // );

            options.addEntry(
                ceb.startBooleanToggle(Text.literal("Embed watermark"), Drawme.config.useWatermark())
                    .setDefaultValue(VirginConfig.useWatermark())
                    .setTooltip(Text.literal("Sets a tag on every armorstand / text display called \"drawme\" to be removed via /kill for easy removal."))
                    .setSaveConsumer(Drawme.config::setUseWatermark)
                    .build()
            );

            options.addEntry(
                ceb.startBooleanToggle(Text.literal("Validate suggestions"), Drawme.config.doValidateSuggestions())
                    .setDefaultValue(VirginConfig.doValidateSuggestions())
                    .setTooltip(Text.literal("Check command suggestions to make sure an image or video can be read. May include lag spikes."))
                    .setSaveConsumer(Drawme.config::setValidateSuggestions)
                    .build()
            );

            options.addEntry(
                ceb.startBooleanToggle(Text.literal("Run title screen easter egg"), Drawme.config.useTitleScreenEasterEgg())
                    .setDefaultValue(VirginConfig.useTitleScreenEasterEgg())
                    .setTooltip(Text.literal("Special title screen easter egg, random chance of a video appearing (Put it under the ./drawme/_splash/ folder!)"))
                    .setSaveConsumer(Drawme.config::setTitleScreenEasterEgg)
                    .build()
            );
            options.addEntry(
                ceb.startBooleanToggle(Text.literal("Run videos on title screen only"), Drawme.config.useTitleScreenOnly())
                    .setDefaultValue(VirginConfig.useTitleScreenOnly())
                    .setTooltip(Text.literal("Chooses a random folder from the drawme folder, and plays that video. (Negates above option if this is on)"))
                    .setSaveConsumer(Drawme.config::setTitleScreenOnly)
                    .build()
            );

            return cb.build();
        };
    }

}