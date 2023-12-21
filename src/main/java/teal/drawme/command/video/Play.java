package teal.drawme.command.video;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import teal.drawme.command.argument.BetterStringArgument;
import teal.drawme.util.Images;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static teal.drawme.Drawme.*;
import static teal.drawme.util.Images.shittyInput;

public interface Play extends Command<FabricClientCommandSource> {

    @Override
    default int run(CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {
        if(client.player == null || client.world == null) throw new SimpleCommandExceptionType(Text.literal("Player not found")).create();
        if(drawing || playing) throw new SimpleCommandExceptionType(Text.literal("Wait for other drawing to finish.")).create();

        File[] imgs = new File(base.getAbsolutePath() + '/' + BetterStringArgument.getString(context, "image")).listFiles();
        @Nullable File audio = new File(base.getAbsolutePath() + '/' + BetterStringArgument.getString(context, "image") + ".wav");
        if(imgs == null || imgs.length == 0) throw new SimpleCommandExceptionType(shittyInput).create();
        BufferedImage image = Images.getSeq(imgs[0]);
        if(!Images.playAudio(context)) audio = null;

        int[] res = Images.getRes(context, image);
        boolean[] rgb = Images.getRGB(context);
        boolean useTextDisplay = Images.useDisplayDevice(context);
        String fill = Images.getFill(context, BoolArgumentType.getBool(context, "small"));

        double fps = config.getFps();
        try {
            fps = DoubleArgumentType.getDouble(context, "fps");
        } catch (IllegalArgumentException ignored) {}
        fps = 1000 / fps;
        Arrays.sort(imgs);
        se = Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new VideoPlayer(res[0], res[1], rgb[0], rgb[1], rgb[2], useTextDisplay, fill, imgs, audio, context), 0L, Math.round(fps * 1E3), TimeUnit.MICROSECONDS);
        return 0;
    }

    static List<Entity> getVideoEntities() { return VideoPlayer.ens; }
    static Play get() {
        return new Play() {};
    }
}

class VideoPlayer implements Runnable {

    private final int resX;
    private final int resY;

    private final boolean R;
    private final boolean G;
    private final boolean B;

    @Nullable
    private final Clip clip;
    public static List<Entity> ens = new ArrayList<>();
    
    private final String fill;
    private final File[] images;

    private int i = 0;

    public VideoPlayer(int resX, int resY, boolean R, boolean G, boolean B, boolean useDisplayDevice, String fill, File[] images, @Nullable File audio, CommandContext<FabricClientCommandSource> context) {
        playing = true;
        this.resX = resX;
        this.resY = resY;
        this.R = R;
        this.G = G;
        this.B = B;
        this.fill = fill;
        this.images = images;

        Vec3d location = client.player.getPos();
        @Nullable Clip temp = null;
        try {
            if(audio != null && audio.exists() && audio.isFile()) {
                temp = AudioSystem.getClip();
                AudioInputStream as = AudioSystem.getAudioInputStream(audio);
                temp.open(as);
                temp.start();
            }
        } catch (LineUnavailableException | IOException | UnsupportedAudioFileException ignored) {
            context.getSource().sendError(Text.literal("Could not load audio."));
        } finally {
            clip = temp;
        }
        for (int i = 0; i < resY; i++) {
            Entity en;
            if (useDisplayDevice) {
                en = new DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, client.world);
                ((DisplayEntity.TextDisplayEntity)en).setBackground(0x1A);
                ((DisplayEntity.TextDisplayEntity)en).setLineWidth(Integer.MAX_VALUE);
                en.setPosition(location);
                en.setYaw((client.player.getHeadYaw() + 180.F) % 360.F);
            } else {
                en = new ArmorStandEntity(client.world, location.x, location.y, location.z);
                en.setNoGravity(true);
                en.setInvisible(true);
                en.setCustomName(Text.literal(""));
                en.setCustomNameVisible(true);
            }
            en.resetPosition();
            en.setId(Integer.MIN_VALUE + i);
            location = location.add(0.0, (double) client.textRenderer.getWidth(fill) / 40, 0.0);
            ens.add(en);
        }
    }

    public void cleanup() {
        ens = new ArrayList<>();
        playing = false;
        paused = false;
        if(clip != null) clip.stop();
    }

    @Override
    public void run() {
        if(!(playing && client.world != null && i < images.length)) {
            cleanup();
            se.cancel(true);
            return;
        }
        if(paused || client.isPaused()) {
            if(clip != null && clip.isRunning()) clip.stop();
            return;
        } else
            if(clip != null && !clip.isRunning()) clip.start();
        try {
            final int[][] pix = Images.readImage(ImageIO.read(images[i]), resX, resY);
            MutableText name = Text.empty();
            for (int j = 0; j < pix.length; j++) {
                int[] row = pix[j];
                for(int pixel : row)
                    name.append(Text.literal(fill).setStyle(Style.EMPTY.withColor(0xFFFFFF & (((pixel >> 24) & 0xff) == 0 ? 0xffffff : ((R ? pixel & 0xff0000 : 0) | (G ? pixel & 0xff00 : 0) | (B ? pixel & 0xff : 0))))));

                if(!ens.get(j).isRemoved()) {
                    Entity en = ens.get(j);
                    if(en instanceof ArmorStandEntity) {
                        en.setCustomName(name);
                    } else {
                        ((DisplayEntity.TextDisplayEntity) en).setText(name);
                    }
                    // client.getNetworkHandler().onEntityStatus();
                }
                name = Text.empty();
            }
        } catch (Exception e) {
            // bad frame, neglect error
            e.printStackTrace();
        }
        ++i;
    }
}