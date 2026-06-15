package me.eldodebug.soar.management.mods.impl;

import java.util.LinkedList;
import java.util.Queue;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventReceivePacket;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.IChatComponent;

public class ChatSpamFilterMod extends Mod {

    private NumberSetting maxRepeatedSetting = new NumberSetting(TranslateText.MAX_REPEATED, this, 3, 1, 10, true);
    private NumberSetting similaritySetting = new NumberSetting(TranslateText.SIMILARITY_THRESHOLD, this, 85, 50, 100, true);
    private BooleanSetting blockCapsSetting = new BooleanSetting(TranslateText.BLOCK_CAPS, this, false);
    private NumberSetting capsThresholdSetting = new NumberSetting(TranslateText.CAPS_THRESHOLD, this, 80, 50, 100, true);

    private final Queue<String> recentMessages = new LinkedList<>();
    private static final int HISTORY_SIZE = 20;

    public ChatSpamFilterMod() {
        super(TranslateText.CHAT_SPAM_FILTER, TranslateText.CHAT_SPAM_FILTER_DESCRIPTION, ModCategory.OTHER, "chatfilter antispam");
    }

    @EventTarget
    public void onReceivePacket(EventReceivePacket event) {
        if (!(event.getPacket() instanceof S02PacketChat)) return;

        S02PacketChat packet = (S02PacketChat) event.getPacket();
        IChatComponent component = packet.getChatComponent();
        if (component == null) return;

        String message = component.getUnformattedText();
        if (message == null || message.isEmpty()) return;

        String normalised = message.toLowerCase().trim();

        // Block excessive caps
        if (blockCapsSetting.isToggled()) {
            long upperCount = normalised.chars().filter(c -> c >= 'A' && c <= 'Z').count();
            long letterCount = normalised.chars().filter(Character::isLetter).count();
            if (letterCount > 5) {
                long rawUpper = message.chars().filter(c -> c >= 'A' && c <= 'Z').count();
                double capsRatio = (double) rawUpper / letterCount * 100;
                if (capsRatio >= capsThresholdSetting.getValue()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Block duplicate/spam messages
        int repeatCount = 0;
        for (String recent : recentMessages) {
            if (similarity(recent, normalised) >= similaritySetting.getValue()) {
                repeatCount++;
            }
        }

        if (repeatCount >= maxRepeatedSetting.getValueInt()) {
            event.setCancelled(true);
            return;
        }

        recentMessages.offer(normalised);
        if (recentMessages.size() > HISTORY_SIZE) {
            recentMessages.poll();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        recentMessages.clear();
    }

    private double similarity(String a, String b) {
        if (a.equals(b)) return 100.0;
        if (a.isEmpty() || b.isEmpty()) return 0.0;

        int maxLen = Math.max(a.length(), b.length());
        int distance = levenshtein(a, b);
        return (1.0 - (double) distance / maxLen) * 100.0;
    }

    private int levenshtein(String a, String b) {
        int lenA = a.length();
        int lenB = b.length();
        int[][] dp = new int[lenA + 1][lenB + 1];

        for (int i = 0; i <= lenA; i++) dp[i][0] = i;
        for (int j = 0; j <= lenB; j++) dp[0][j] = j;

        for (int i = 1; i <= lenA; i++) {
            for (int j = 1; j <= lenB; j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        return dp[lenA][lenB];
    }
}
