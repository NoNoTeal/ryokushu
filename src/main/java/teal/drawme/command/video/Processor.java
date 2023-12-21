package teal.drawme.command.video;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.apache.commons.exec.CommandLine;
import teal.drawme.Drawme;
import teal.drawme.command.argument.BetterStringArgument;

import java.io.File;
import java.io.IOException;

import static teal.drawme.command.suggestion.SuggestVideoWidth.findFFmpegTool;

public interface Processor extends Command<FabricClientCommandSource> {

    @Override
    default int run(CommandContext<FabricClientCommandSource> context) {
        String videoFile = BetterStringArgument.getString(context, "video");
        String folderName = BetterStringArgument.getString(context, "folder");
        final File output = new File(Drawme.base + "/" + folderName);
        final File audio = new File(Drawme.base + "/" + folderName + ".wav");
        if((output.exists() && (output.isDirectory() && output.listFiles().length > 0)) || audio.exists()) {
            context.getSource().sendError(Text.literal("Video folder and/or audio file already exists."));
            return -1;
        }

        output.mkdirs();
        int width = 0;
        double fps = Drawme.config.getFps();
        try {
            width = IntegerArgumentType.getInteger(context, "width");
            fps = DoubleArgumentType.getDouble(context, "fps");
        } catch (IllegalArgumentException ignored) {};

        final String ffmpeg = findFFmpegTool("ffmpeg");
        final CommandLine converter = new CommandLine(ffmpeg)
            .addArgument("-i").addArgument(videoFile)
            .addArgument("-r").addArgument(Double.toString(fps));

        if(width > 0) converter.addArgument("-vf").addArgument("scale=" + width + ":-1");
        converter.addArgument("-compression_level").addArgument("100")
            .addArgument("-q:v").addArgument("5")
            .addArgument("-crf").addArgument("24")
            .addArgument(output.getAbsolutePath() + "/%06d.jpg");

        final CommandLine audioMuxer = new CommandLine(ffmpeg)
            .addArgument("-i").addArgument(videoFile)
            .addArgument("-qscale").addArgument("0")
            .addArgument("-vn")
            .addArgument(audio.getAbsolutePath());

        new Thread(() -> {
            try {
                Process vid = new ProcessBuilder(converter.toStrings()).directory(Drawme.base).start();
                Process aud = new ProcessBuilder(audioMuxer.toStrings()).directory(Drawme.base).start();
                context.getSource().sendFeedback(Text.literal("Writing image sequence to ")
                    .append(
                        Text.literal(output.getAbsolutePath())
                            .setStyle(Style.EMPTY
                                .withUnderline(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, output.getAbsolutePath()))
                            )
                    )
                );
                vid.waitFor();
                aud.waitFor();
                context.getSource().sendFeedback(Text.literal("Finished writing audio and images."));
            } catch (InterruptedException ie) {
                ie.printStackTrace();
                context.getSource().sendError(Text.literal("FFmpeg process was interrupted."));
            } catch (IOException e) {
                e.printStackTrace();
                context.getSource().sendError(Text.literal("FFprobe and/or FFmpeg was not detected."));
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
                context.getSource().sendError(Text.literal("Video is invalid. Does it exist?"));
            }
        }).start();

        return 0;
    }

    static Processor get() {
        return new Processor() {};
    }
}
