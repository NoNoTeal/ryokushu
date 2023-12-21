package teal.drawme.util;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import teal.drawme.command.suggestion.SuggestWidth;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import static teal.drawme.Drawme.base;

public class Images {

    public static final String defFill = "█";

    public static final MutableText shittyInput = Text.literal("Bad image input. Provide image(s) in ")
        .append(
            Text.literal(base.getAbsolutePath())
            .setStyle(Style.EMPTY.withUnderline(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, base.getAbsolutePath()))
            )
        );

    public static BufferedImage getSeq(File file) throws CommandSyntaxException {
        try { return Objects.requireNonNull(ImageIO.read(file)); } catch (NullPointerException | IOException ignored) {}
        throw new SimpleCommandExceptionType(shittyInput).create();
    }

    public static boolean playAudio(CommandContext<FabricClientCommandSource> ctx) {
        try {
            return !BoolArgumentType.getBool(ctx, "mute");
        } catch (IllegalArgumentException IAE) {
            return true;
        }
    }

    public static int[] getRes(CommandContext<FabricClientCommandSource> ctx, BufferedImage image) {
        int resX = SuggestWidth.max, resY;
        try {
            resX = IntegerArgumentType.getInteger(ctx, "resize_x");
            resY = IntegerArgumentType.getInteger(ctx, "resize_y");
        } catch (IllegalArgumentException IAE) {
            float factor = image.getWidth() / (float) resX;
            resY = Math.round(image.getHeight() / factor);
        }
        return new int[] { resX, resY };
    }

    public static boolean useDisplayDevice(CommandContext<FabricClientCommandSource> ctx) {
        try {
            return BoolArgumentType.getBool(ctx, "display_device");
        } catch (IllegalArgumentException ignored) {}
        return false;
    }

    public static boolean[] getRGB(CommandContext<FabricClientCommandSource> ctx) {
        boolean r,g,b;
        r = g = b = true;
        try {
            r = !BoolArgumentType.getBool(ctx, "nr");
            g = !BoolArgumentType.getBool(ctx, "ng");
            b = !BoolArgumentType.getBool(ctx, "nb");
        } catch (IllegalArgumentException ignored) {}
        return new boolean[] { r, g, b };
    }

    public static String getFill(CommandContext<FabricClientCommandSource> ctx, boolean small) {
        String fill;
        try {
            fill = StringArgumentType.getString(ctx, "fill");
        } catch (IllegalArgumentException | NullPointerException EXC) { fill = !small ? defFill : "."; }
        return fill;
    }

    public static int[][] readImage(BufferedImage orig, int x, int y) {
        BufferedImage scale = new BufferedImage(x, y, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics g = scale.createGraphics();
        g.drawImage(orig, 0, 0, x, y, null);
        g.dispose();
        byte[] pix = ((DataBufferByte) scale.getRaster().getDataBuffer()).getData();
        int w = scale.getWidth();
        int h = scale.getHeight();
        int l = scale.getAlphaRaster() != null ? 1 : 0;
        int[][] pixl = new int[h][w];
        for(int pos = 0, row = 0, col = 0; pos + 2 < pix.length; pos += l+3) {
            pixl[h-1-row][col] = (l > 0 ? (pix[pos] & 0xff) << 24 : -16777216) |
                    (pix[pos+l+2] & 0xff) << 16 |
                    (pix[pos+l+1] & 0xff) << 8 |
                    (pix[pos+l] & 0xff);
            if(++col == w) {
                col = 0; row++;
            }
        }
        return pixl;
    }
}
