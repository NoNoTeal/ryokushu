package teal.drawme.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public interface Help extends Command<FabricClientCommandSource> {
    @Override
    default int run(CommandContext<FabricClientCommandSource> context) {
        context.getSource().getPlayer().sendMessage(getType().message, false);
        return 0;
    }

    Guide getType();

    static Help get(Guide type) {
        return () -> type;
    }

    enum Guide {
        Draw(
                "draw <image> <small> [<display_device>] [<resize_x>] [<resize_y>] [<interval>] [<nr>] [<ng>] [<nb>] [<fill>]",
                """
                        Draw an image using armorstands.
                        • <image> is the path to your image.
                        • <small> will use periods instead of boxes to scale down the image in Minecraft at the same resolution.
                        • [<display_device>] indicates whether to use the new text_display entity instead of armorstands.
                        • [<resize_x>] resizes the width of the image. You may see 3 numbers: The original width, a number smaller than 480 (max height is 480), or 480 (max width is 480).
                        • [<resize_y>] resizes the height of the image. Leaving this blank will scale the height down appropriately.
                        • [<interval>] indicates how frequently to place an armorstand. Not needed for singleplayer, default is 75 (ms) for multiplayer. The higher this value is, the more likely armorstands will be placed.
                        • [<nr>] indicates whether to toggle the RED channel
                        • [<ng>] indicates whether to toggle the GREEN channel
                        • [<nb>] indicates whether to toggle the BLUE channel
                        • [<fill>] indicates what character to fill the names with. Overrides <small>."""
        ),
        Abort(
                "drawme abort",
                """
                        Stops drawing an image.
                        """
        ),
        Pause(
                "drawme pause",
                """
                        Pauses a video.
                        """
        ),
        Stop(
                "drawme stop",
                """
                        Stops a video from playing.
                        """
        ),
        Open(
            "drawme open",
            """
                    Simple text to open the drawme folder.
                    """
        ),
        Play(
                "play <image> <small> [<display_device>] [<mute>] [<resize_x>] [<resize_y>] [<fps>] [<nr>] [<ng>] [<nb>] [<fill>]",
                """
                        Playback video using armorstands.
                        The command parameters are similar to draw, the only difference being...
                        • <image> is the name of the folder with images inside. See the manual on how to set it up.
                        • <mute> will determine whether or not the attached video audio should be played. Audio will play by default, and volume is independent of Minecraft's sound system.
                        • [<fps>] is the frame rate to play the image sequence at. The default frame rate is 30 FPS.
                        """
        ),
        Process(
                "drawme process <video> <folder> [<width>] [<fps>]",
                """
                        Processes a video in the drawme folder to then be played. FFmpeg is required.
                        • <video> is the name of the video in your drawme folder.
                        • <folder> is the name of the folder AND audio file to dump video and audio to.
                        • [<width>] is the new width of the image sequence.
                        • [<fps>] is the frame rate to write the image sequence as.
                        """
        ),
        Manual(
                "Manual",
                """
                        & Creating images:
                            This mod does not make any network requests to receive images. All files must be local and under the drawme folder in your minecraft folder.
                        & Video folders:
                            Video folders are a sequence of images. Use ffmpeg on a video to extract the frames into a folder, OR use the drawme process command. Your command will look something like this:
                            ffmpeg -i <video> -r <framerate> -vf scale=<desired image length>:-1 -compression_level 100 -q:v 5 ./folder/%06d.jpg
                            This command exports frames in a compressed format that do not take up too much disk space. The files are sorted using Arrays.sort.
                        & Audio file:
                            When you allow audio on the "play" command, it will play an audio file with the same name of the folder that contains the image sequence.
                            For example, if your folder is located at ./folder/music/ where music is a folder containing an image sequence, then the audio file should be located at ./folder/music.wav.
                            The wav extension should be lowercase, and the filename is case-sensitive. Extract audio from your video using the following ffmpeg command:
                            ffmpeg -i <video> -qscale 0 -vn ./folder/<name>.wav
                            This command exports the audio at the same quality while removing the video codec. WAV is an uncompressed lossless format, so the file size will be larger.
                        & Servers:
                            Draw is usable on servers, while play is clientside. If implemented serverside, play requires serverside access to manipulate the armorstands' names. Since Minecraft is not intended to play videos, the end result is astoundingly laggy even on a local server.
                            An interval is recommended for servers when using draw, otherwise there will be missing lines of the image.
                            This mod cannot remove "draw" armorstands. "play" armorstands' names are removed after playback is complete or "stop" is called.
                        & Multitasking:
                            It is possible to run multiple play commands at once, but it is not recommended and is soft-blocked. Playing one video is already putting a lot of stress on your client and disk drive.
                            Additionally, drawing an image spams a spawn egg which makes drawing multiple images at once impossible given the current code.
                        & Processing:
                            Videos are processed by processing each frame when called rather than preprocessing an entire folder. Preprocessing may allow faster playback but ultimately sacrifices memory. When an individual frame is called, less memory is needed while sacrificing the CPU.
                        & Text Display VS Armorstand:
                            Text displays are only a feature for 1.20+.
                            Text display rotations are static, while armorstands are dynamic. As a result, text displays are one-sided.
                            The pitch of a text display can be modified.
                        """,
            true
        );
        public final MutableText message;
        Guide(String header, String contents) {
            this(header, contents, false);
        }
        Guide(String header, String contents, boolean highlights) {
            message = Text.literal(header+"\n\n").setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(true));
            // yeah intellij this has too few cases so let's make it a SWITCH STATEMENT WHERE FUCKING JAVA THROWS A FIT ABOUT ENUMS AND CONSTRUCTORS!!!
            if(highlights) {
                String[] a = contents.split("\\n");
                for(String l : a) {
                    if(l.startsWith("&"))
                        message.append(Text.literal(l + '\n').setStyle(Style.EMPTY.withColor(Formatting.WHITE).withItalic(false).withBold(true)));
                    else
                        message.append(Text.literal(l + '\n').setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true).withBold(false)));
                }
            } else {
                message.append(Text.literal(contents).setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true).withBold(false)));
            }
        }
    }
}
