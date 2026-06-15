package me.eldodebug.soar.management.mods.impl;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventTick;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.network.play.client.C16PacketClientStatus;

public class AutoRespawnMod extends Mod {

    private NumberSetting delaySetting = new NumberSetting(TranslateText.DELAY, this, 20, 1, 200, true);

    private int tickCount = 0;

    public AutoRespawnMod() {
        super(TranslateText.AUTO_RESPAWN, TranslateText.AUTO_RESPAWN_DESCRIPTION, ModCategory.PLAYER);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (mc.currentScreen instanceof GuiGameOver) {
            tickCount++;
            if (tickCount >= delaySetting.getValueInt()) {
                tickCount = 0;
                mc.getNetHandler().addToSendQueue(
                    new C16PacketClientStatus(C16PacketClientStatus.EnumState.PERFORM_RESPAWN)
                );
                mc.displayGuiScreen(null);
            }
        } else {
            tickCount = 0;
        }
    }
}
