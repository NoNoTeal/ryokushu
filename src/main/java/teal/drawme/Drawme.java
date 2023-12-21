package teal.drawme;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import teal.drawme.command.Draw;
import teal.drawme.command.Help;
import teal.drawme.command.Abort;
import teal.drawme.command.Open;
import teal.drawme.command.suggestion.SuggestVideoWidth;
import teal.drawme.command.suggestion.SuggestVideos;
import teal.drawme.command.video.Pause;
import teal.drawme.command.video.Play;
import teal.drawme.command.argument.BetterStringArgument;
import teal.drawme.command.suggestion.SuggestFiles;
import teal.drawme.command.suggestion.SuggestWidth;
import teal.drawme.command.video.Processor;
import teal.drawme.command.video.Stop;
import teal.drawme.modmenu.Config;

import java.io.File;
import java.util.concurrent.ScheduledFuture;

public class Drawme implements ClientModInitializer {

    public static final Config config = Config.get();
    public static final MinecraftClient client = MinecraftClient.getInstance();
    public static final File base = new File("drawme");
    public static boolean drawing = false;
    public static boolean playing = false;
    public static boolean paused = false;
    public static ScheduledFuture<?> se;

    @Override
    public void onInitializeClient() {
        if(base.isFile()) base.delete();
        base.mkdirs();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            final LiteralArgumentBuilder<FabricClientCommandSource> DRAW = ClientCommandManager.literal("draw").then(
                ClientCommandManager.argument("image", BetterStringArgument.string())
                    .suggests(SuggestFiles.get(false)).then(
                        ClientCommandManager.argument("small", BoolArgumentType.bool()).then(
                            ClientCommandManager.argument("display_device", BoolArgumentType.bool()).then(
                                ClientCommandManager.argument("resize_x", IntegerArgumentType.integer(1))
                                    .suggests(SuggestWidth.getW()).then(
                                        ClientCommandManager.argument("resize_y", IntegerArgumentType.integer(1))
                                            .suggests(SuggestWidth.getH()).then(
                                                ClientCommandManager.argument("interval", IntegerArgumentType.integer(0, 5000))
                                                    .then(
                                                        ClientCommandManager.argument("nr", BoolArgumentType.bool()).then(
                                                            ClientCommandManager.argument("ng", BoolArgumentType.bool()).then(
                                                                ClientCommandManager.argument("nb", BoolArgumentType.bool()).then(
                                                                    ClientCommandManager.argument("fill", BetterStringArgument.string())
                                                                        .executes(Draw.get())
                                                                ).executes(Draw.get())
                                                            )
                                                        )
                                                    ).executes(Draw.get())
                                            ).executes(Draw.get())
                                    ).executes(Draw.get())
                            ).executes(Draw.get())
                        ).executes(Draw.get())
                    )
            );
            final LiteralArgumentBuilder<FabricClientCommandSource> PLAY = ClientCommandManager.literal("play").then(
                ClientCommandManager.argument("image", BetterStringArgument.string())
                    .suggests(SuggestFiles.get(true)).then(
                        ClientCommandManager.argument("small", BoolArgumentType.bool()).then(
                            ClientCommandManager.argument("display_device", BoolArgumentType.bool()).then(
                                ClientCommandManager.argument("mute", BoolArgumentType.bool()).then(
                                    ClientCommandManager.argument("resize_x", IntegerArgumentType.integer(1))
                                        .suggests(SuggestWidth.getW()).then(
                                            ClientCommandManager.argument("resize_y", IntegerArgumentType.integer(1))
                                                .suggests(SuggestWidth.getH()).then(
                                                    ClientCommandManager.argument("fps", DoubleArgumentType.doubleArg(1, 120))
                                                        .then(
                                                            ClientCommandManager.argument("nr", BoolArgumentType.bool()).then(
                                                                ClientCommandManager.argument("ng", BoolArgumentType.bool()).then(
                                                                    ClientCommandManager.argument("nb", BoolArgumentType.bool()).then(
                                                                        ClientCommandManager.argument("fill", BetterStringArgument.string())
                                                                            .executes(Play.get())
                                                                    ).executes(Play.get())
                                                                )
                                                            )
                                                        ).executes(Play.get())
                                                ).executes(Play.get())
                                        ).executes(Play.get())
                                ).executes(Play.get())
                            ).executes(Play.get())
                        ).executes(Play.get())
                    )
            );

            dispatcher.register(DRAW);
            dispatcher.register(PLAY);
            dispatcher.register(
                ClientCommandManager.literal("drawme")
                .then(DRAW)
                .then(PLAY)
                .then(
                    ClientCommandManager.literal("abort")
                        .executes(Abort.get())
                )
                .then(
                    ClientCommandManager.literal("stop")
                        .executes(Stop.get())
                )
                .then(
                    ClientCommandManager.literal("process").then(
                        ClientCommandManager.argument("video", BetterStringArgument.string()).suggests(SuggestVideos.get()).then(
                            ClientCommandManager.argument("folder", BetterStringArgument.fileSafe()).then(
                                ClientCommandManager.argument("width", IntegerArgumentType.integer(1)).suggests(SuggestVideoWidth.get()).then(
                                    ClientCommandManager.argument("fps", DoubleArgumentType.doubleArg(1,120))
                                        .executes(Processor.get())
                                ).executes(Processor.get())
                            ).executes(Processor.get())
                        )
                    )
                )
                .then(
                    ClientCommandManager.literal("pause")
                    .executes(Pause.get())
                )
                .then(
                    ClientCommandManager.literal("open")
                        .executes(Open.get())
                )
                .then(
                    ClientCommandManager.literal("help")
                        .then(ClientCommandManager.literal("draw")
                            .executes(Help.get(Help.Guide.Draw)))
                        .then(ClientCommandManager.literal("play")
                            .executes(Help.get(Help.Guide.Play)))
                        .then(ClientCommandManager.literal("pause")
                            .executes(Help.get(Help.Guide.Pause)))
                        .then(ClientCommandManager.literal("abort")
                           .executes(Help.get(Help.Guide.Abort)))
                        .then(ClientCommandManager.literal("stop")
                            .executes(Help.get(Help.Guide.Stop)))
                        .then(ClientCommandManager.literal("process"))
                            .executes(Help.get(Help.Guide.Process))
                        .then(ClientCommandManager.literal("open"))
                            .executes(Help.get(Help.Guide.Open))
                        .executes(Help.get(Help.Guide.Manual))
                )
            );
        });
    }
}
