package teal.drawme.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import teal.drawme.command.argument.BetterStringArgument;
import teal.drawme.util.Images;

import java.awt.image.BufferedImage;
import java.io.File;

import static teal.drawme.Drawme.*;

public interface Draw extends Command<FabricClientCommandSource> {

    @Override
    default int run(CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {
        if(client.player == null || client.interactionManager == null) throw new SimpleCommandExceptionType(Text.literal("Player not found")).create();
        if(drawing || playing) throw new SimpleCommandExceptionType(Text.literal("Wait for other drawing to finish.")).create();
        if(!context.getSource().getPlayer().isCreative()) throw new SimpleCommandExceptionType(Text.literal("Must be in creative mode")).create();

        BufferedImage image = Images.getSeq(new File(base.getAbsolutePath() + '/' + BetterStringArgument.getString(context, "image")));
        int[] res = Images.getRes(context, image);
        boolean[] rgb = Images.getRGB(context);
        final boolean useTextDisplay = Images.useDisplayDevice(context);

        final int[][] pixels = Images.readImage(image, res[0], res[1]);

        final int slot = client.player.getInventory().selectedSlot + 36;
        ItemStack prevItem = client.player.getInventory().getStack(slot);

        drawing = true;
        new Thread(() -> {
            Vec3d pos = client.player.getPos();
            final float yaw = client.player.getHeadYaw();
            double y = pos.y;
            String fill = Images.getFill(context, BoolArgumentType.getBool(context, "small"));
            int interval = client.getServer() == null && (client.getCurrentServerEntry() == null || !client.getCurrentServerEntry().isLocal()) ? 75 : 0;
            try { interval = IntegerArgumentType.getInteger(context, "interval"); } catch (IllegalArgumentException ignored) {}
            long st = System.currentTimeMillis();
            boolean error = false;
            try {
                if (interval > 0) context.getSource().sendFeedback(Text.literal("ETA: " + (interval * pixels.length) / (double) 1000 + "s."));
                ItemStack asi = new ItemStack(Items.SNOW_GOLEM_SPAWN_EGG);
                MutableText name = Text.empty();
                for (int[] yRow : pixels) {
                    for (int pixel : yRow) {
                        name.append(Text.literal(fill).setStyle(Style.EMPTY.withColor(
                            ((pixel >> 24) & 0xff) == 0 ? 0xffffff : (rgb[0] ? pixel & 0xff0000 : 0) | (rgb[1] ? pixel & 0xff00 : 0) | (rgb[2] ? pixel & 0xff : 0)
                        )));
                    }
                    if (!useTextDisplay) {
                        asi = new ItemStack(Items.SNOW_GOLEM_SPAWN_EGG);
                        // so much less java to work with if you use a string
                        asi.setNbt(StringNbtReader.parse("{EntityTag:{" + (config.useWatermark() ? "Tags:[\"drawme\"]," : "") + "id:\"armor_stand\",Pos:[" + pos.x + ',' + y + ',' + pos.z + "],ShowArms:0b,NoGravity:1b,CustomNameVisible:1b,Marker:1b,Invisible:1b,PersistenceRequired:1b,CustomName:'" + Text.Serializer.toJson(name).replaceAll(",\"text\":\"\"}", "").replaceAll("\\{\"extra\":", "") + "'}}"));
                    } else {
                        asi.setNbt(StringNbtReader.parse("{EntityTag:{" + (config.useWatermark() ? "Tags:[\"drawme\"]," : "") + "Rotation:[" + ((yaw + 180.F) % 360.F) + "f, " + /*(Drawme.config.useDisplayPitch() ? client.player.getPitch() : 0) + */ "0.0f],line_width:"+ Integer.MAX_VALUE +",id:\"text_display\",Pos:[" + pos.x + ',' + y + ',' + pos.z + "],NoGravity:1b,PersistenceRequired:1b,alignment:\"center\",background:26}}"));
                        NbtCompound a = asi.getNbt();
                        a.getCompound("EntityTag").putString("text", Text.Serializer.toJson(name).replaceAll(",\"text\":\"\"}", "").replaceAll("\\{\"extra\":", ""));
                        asi.setNbt(a);
                    }
                    BlockPos bp = client.player.getBlockPos().down();
                    y += (double) client.textRenderer.getWidth(fill)  / 40;
                    client.player.getInventory().selectedSlot = slot - 36;
                    client.interactionManager.clickCreativeStack(asi, slot);
                    client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(bp), Direction.UP, bp, false));
                    client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                    name = Text.empty();
                    if (interval > 0) Thread.sleep(interval);
                    if (!drawing) break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                error = true;
            } finally {
                if (interval > 0) context.getSource().sendFeedback(Text.literal((error ? "Aborted drawing!" : "Finished drawing!") + " Time elapsed: " + (System.currentTimeMillis() - st) / 1000L + "s."));
                client.interactionManager.clickCreativeStack(prevItem, slot);
                drawing = false;
            }
        }).start();
        return 0;
    }

    static Draw get() {
        return new Draw() {};
    }
}