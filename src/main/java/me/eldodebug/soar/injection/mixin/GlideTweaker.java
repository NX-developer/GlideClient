package me.eldodebug.soar.injection.mixin;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import me.eldodebug.soar.injection.transformer.LwjglTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class GlideTweaker implements ITweaker {

    private final List<String> launchArguments = new ArrayList<>();

	public static boolean hasOptifine = false;
	
    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
    	
    	try {
			Class.forName("optifine.Patcher");
			hasOptifine = true;
		}
		catch(ClassNotFoundException e) {
		}
		
        this.launchArguments.addAll(args);

        if (profile != null) {
            launchArguments.add("--version");
            launchArguments.add(profile);
        }

        if (assetsDir != null) {
            launchArguments.add("--assetsDir");
            launchArguments.add(assetsDir.getAbsolutePath());
        }

        if (gameDir != null) {
            launchArguments.add("--gameDir");
            launchArguments.add(gameDir.getAbsolutePath());
        }
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {

        // Unlock LWJGL package before Mixin bootstraps so
        // PACKAGE_CLASSLOADER_EXCLUSION warnings are avoided.
        this.unlockLwjgl();

        // Register the NanoVG GL config patcher only on desktop platforms.
        // On Android (PojavLauncher) the LWJGL bindings are different and
        // this transformer is not needed.
        if (!isAndroid()) {
            classLoader.registerTransformer(LwjglTransformer.class.getName());
        }

        MixinBootstrap.init();

        MixinEnvironment env = MixinEnvironment.getDefaultEnvironment();

        // Do NOT override the obfuscation context here.
        // FML (Forge) remaps Minecraft classes to SRG names at runtime, so
        // the Mixin refmap must use "searge" entries (set in build.gradle).
        // Forcing "notch" here would make Mixin search for Notch-obfuscated
        // class names (e.g. "afh") inside an SRG-named class hierarchy,
        // which causes InvalidMixinException "Super class not found" crashes.

        env.setSide(MixinEnvironment.Side.CLIENT);

        Mixins.addConfiguration("mixins.soar.json");
    }

    public static boolean isAndroid() {
        return new java.io.File("/system/build.prop").exists()
            || System.getProperty("os.name", "").toLowerCase().contains("android");
    }

    @Override
    public String getLaunchTarget() {
        return "net.minecraft.client.main.Main";
    }

    @Override
    public String[] getLaunchArguments() {
        return launchArguments.toArray(new String[0]);
    }
    
    @SuppressWarnings("unchecked")
    private void unlockLwjgl() {
        try {
            Field transformerExceptions = LaunchClassLoader.class.getDeclaredField("classLoaderExceptions");
            transformerExceptions.setAccessible(true);
            Object o = transformerExceptions.get(Launch.classLoader);
            ((Set<String>) o).remove("org.lwjgl.");
        } catch (NoSuchFieldException | IllegalAccessException e) {}
    }
}