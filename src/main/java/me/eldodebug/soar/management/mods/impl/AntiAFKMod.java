package me.eldodebug.soar.management.mods.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventUpdate;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;

public class AntiAFKMod extends Mod {

    private NumberSetting intervalSetting = new NumberSetting(TranslateText.INTERVAL, this, 200, 20, 1200, true);
    private ComboSetting modeSetting = new ComboSetting(TranslateText.MODE, this, TranslateText.JUMP,
            new ArrayList<Option>(Arrays.asList(
                    new Option(TranslateText.JUMP),
                    new Option(TranslateText.SNEAK),
                    new Option(TranslateText.MOVEMENT))));

    private int tickCount = 0;
    private final Random random = new Random();

    public AntiAFKMod() {
        super(TranslateText.ANTI_AFK, TranslateText.ANTI_AFK_DESCRIPTION, ModCategory.PLAYER);
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null) return;
        if (mc.thePlayer.getHealth() <= 0) return;

        tickCount++;
        if (tickCount < intervalSetting.getValueInt()) return;
        tickCount = 0;

        TranslateText mode = modeSetting.getOption().getTranslate();

        if (mode.equals(TranslateText.JUMP)) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump();
            }
        } else if (mode.equals(TranslateText.SNEAK)) {
            mc.thePlayer.setSneaking(true);
            mc.thePlayer.setSneaking(false);
        } else if (mode.equals(TranslateText.MOVEMENT)) {
            float yaw = mc.thePlayer.rotationYaw + (random.nextFloat() * 360f - 180f);
            mc.thePlayer.rotationYaw = yaw;
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump();
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        tickCount = 0;
    }
}
