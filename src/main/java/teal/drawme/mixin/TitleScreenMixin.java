package teal.drawme.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import teal.drawme.Drawme;
import teal.drawme.util.Images;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Shadow @Nullable private SplashTextRenderer splashText;
    @Unique private static final String fill = "█";
    @Unique private File FOI = null;
    @Unique private boolean preservedSplash = false;
    @Unique private int frameNo = 0;
    @Unique private boolean playVideo = false;

    @Unique private float SCALE = 0.F;
    @Unique private final Random random = new Random();

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(
        method = "init",
        at = @At("HEAD")
    )
    private void init(CallbackInfo ci) {
        if((!Drawme.config.useTitleScreenEasterEgg() && !Drawme.config.useTitleScreenOnly()) || playVideo || preservedSplash) return;
        int size = client.getSplashTextLoader().splashTexts.size();
        if(Drawme.config.useTitleScreenOnly() || random.nextInt(size+1) == size) preservedSplash = true; else return;
        File[] imgs;
        if(Drawme.config.useTitleScreenOnly()) {
            if(!Drawme.base.isDirectory()) return;
            List<File> folders = Arrays.stream(Drawme.base.listFiles()).filter(File::isDirectory).toList();
            imgs = folders.get(random.nextInt(folders.size())).listFiles();
        } else
            imgs = new File(Drawme.base.getAbsolutePath() + "/_splash").listFiles();
        if(FOI != null || imgs == null || imgs.length == 0) return;
        try {
            Arrays.sort(imgs);
            BufferedImage img;
            for(File imf : imgs) {
                try {
                    img = ImageIO.read(imf);
                    if(img != null) break;
                } catch(IOException ignored) {};
            }
            double FPS = 1000.00 / Drawme.config.getFps();

            SCALE = this.client.options.getGuiScale().getValue() % 6;
            if(SCALE == 0) SCALE = 1F/6;
            else SCALE = 1F / SCALE;

            playVideo = true;
            if(!Drawme.config.useTitleScreenOnly() && Drawme.config.useTitleScreenEasterEgg())
                this.splashText = new SplashTextRenderer("Draw me.");
            Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
                if (!playVideo) return;
                if (this.client != null && this.client.currentScreen != this) return;
                if (imgs.length - 1 < frameNo) frameNo = 0;
                FOI = imgs[frameNo];
                frameNo++;
            }, 0, Math.round(FPS * 1E3), TimeUnit.MICROSECONDS);
        } catch(Exception ignore) {

        }
    }

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/RotatingCubeMapRenderer;render(FF)V"
        )
    )
    private void z(RotatingCubeMapRenderer instance, float delta, float alpha) {
        if(FOI == null || !playVideo)
            instance.render(delta, alpha);
    }

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/ClickableWidget;setAlpha(F)V"
        )
    )
    private void t(ClickableWidget instance, float alpha) {
        if(!(FOI == null || !playVideo))
            instance.setAlpha(0.6F);
    }

    @Inject(
        method = "render",
        at = @At("HEAD")
    )
    private void s(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if(FOI == null || !playVideo) {
            return;
        }
        context.setShaderColor(0.F, 0.F, 0.F, 1.0F);
        context.drawTexture(OPTIONS_BACKGROUND_TEXTURE, 0, 0, 0, 0.0F, 0.0F, this.width, this.height, 32, 32);
        context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        try {
            context.getMatrices().scale(SCALE, SCALE, SCALE);
            BufferedImage img = ImageIO.read(FOI);
            if(img == null) return;
            int[][] pix = Images.readImage(
                img,
                (int) Math.ceil((float) this.width / (this.textRenderer.getWidth(fill)) / SCALE),
                (int) Math.ceil((float) this.height / (this.textRenderer.fontHeight) / SCALE)
            );
            for(int i = 0; i < pix.length; i++) {
                int[] strip = pix[pix.length - i - 1];
                MutableText txt = Text.empty();
                for(int ind : strip)
                    txt.append(Text.literal(fill).setStyle(Style.EMPTY.withColor(0xFFFFFF & ind)));
                context.drawText(textRenderer, txt, 0, i*textRenderer.fontHeight, 0xffffff, false);
            }
        } catch (Exception ignored) {} finally {
            context.getMatrices().scale(1 / SCALE, 1 / SCALE, 1 / SCALE);
        }
    }

}