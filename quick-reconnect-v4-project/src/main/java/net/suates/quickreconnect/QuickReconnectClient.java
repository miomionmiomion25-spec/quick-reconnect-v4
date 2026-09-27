package net.suates.quickreconnect;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Quick Reconnect v4 - client only.
 *
 * Behaviour:
 *  - R is the default key and can be changed in Options -> Controls.
 *  - On the configured key press, the current multiplayer server entry is saved.
 *  - The client disconnects immediately.
 *  - Exactly one reconnect attempt is queued for 1000 ms later.
 *  - A normal Esc-menu disconnect never schedules a reconnect because only this
 *    key handler creates a reconnect request.
 *  - No reconnect loop is created.
 */
public final class QuickReconnectClient implements ClientModInitializer {
    public static final String MOD_ID = "quick_reconnect";

    private static final long RECONNECT_DELAY_MS = 1000L;
    private static final AtomicBoolean RECONNECT_PENDING = new AtomicBoolean(false);

    private static KeyBinding quickReconnectKey;
    private static volatile ServerInfo savedServer;

    @Override
    public void onInitializeClient() {
        KeyBinding.Category category = KeyBinding.Category.register(
                Identifier.of(MOD_ID, "main")
        );

        quickReconnectKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.quick_reconnect.reconnect",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_R,
                        category
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(QuickReconnectClient::tick);
    }

    private static void tick(MinecraftClient client) {
        while (quickReconnectKey.consumeClick()) {
            activate(client);
        }
    }

    private static void activate(MinecraftClient client) {
        // One active operation at a time. This also prevents holding R from
        // starting multiple reconnect attempts.
        if (!RECONNECT_PENDING.compareAndSet(false, true)) {
            return;
        }

        ServerInfo currentServer = client.getCurrentServerEntry();

        // Only multiplayer servers are valid targets. In singleplayer this is null.
        if (currentServer == null || client.getNetworkHandler() == null) {
            RECONNECT_PENDING.set(false);
            return;
        }

        savedServer = currentServer;

        // Disconnect on the client thread immediately.
        client.disconnect();

        // Use a real 1000 ms wall-clock delay rather than 20 game ticks.
        Thread reconnectThread = new Thread(() -> {
            try {
                Thread.sleep(RECONNECT_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                savedServer = null;
                RECONNECT_PENDING.set(false);
                return;
            }

            client.execute(QuickReconnectClient::reconnectExactlyOnce);
        }, "QuickReconnect-v4-delay");

        reconnectThread.setDaemon(true);
        reconnectThread.start();
    }

    private static void reconnectExactlyOnce(MinecraftClient client) {
        // Consume the request before connecting. There is deliberately no code
        // anywhere that schedules another reconnect after this method.
        ServerInfo server = savedServer;
        savedServer = null;
        RECONNECT_PENDING.set(false);

        if (server == null) {
            return;
        }

        try {
            ServerAddress address = ServerAddress.parse(server.address);
            ConnectScreen.connect(
                    null,
                    client,
                    address,
                    server,
                    false,
                    null
            );
        } catch (RuntimeException ignored) {
            // Exactly one attempt was made. Failure must never start a loop.
        }
    }
}
