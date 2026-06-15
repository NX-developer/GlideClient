package me.eldodebug.soar.discord;

import java.time.OffsetDateTime;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.discord.ipc.IPCClient;
import me.eldodebug.soar.discord.ipc.IPCListener;
import me.eldodebug.soar.discord.ipc.entities.RichPresence;
import me.eldodebug.soar.discord.ipc.exceptions.NoDiscordClientException;
import me.eldodebug.soar.injection.mixin.GlideTweaker;

public class DiscordRPC {

	private IPCClient client;
	private boolean started = false;

	public void start() {
		// Discord IPC uses Unix/Windows named pipes – not available on Android.
		if (GlideTweaker.isAndroid()) return;

		try {
			client = new IPCClient(1059341815205068901L);
			client.setListener(new IPCListener() {
				@Override
				public void onReady(IPCClient client) {
					RichPresence.Builder builder = new RichPresence.Builder();
					builder.setState("Playing Glide Client v" + Glide.getInstance().getVersion())
							.setStartTimestamp(OffsetDateTime.now())
							.setLargeImage("icon");
					client.sendRichPresence(builder.build());
				}
			});
			client.connect();
			started = true;
		} catch (NoDiscordClientException e) {
			// Discord is not running – silently ignore.
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		if (started && client != null) {
			try {
				client.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			started = false;
		}
	}

	public IPCClient getClient() {
		return client;
	}

	public boolean isStarted() {
		return started;
	}
}
