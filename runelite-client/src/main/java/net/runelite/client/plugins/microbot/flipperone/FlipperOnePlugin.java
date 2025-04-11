package net.runelite.client.plugins.microbot.flipperone;

import com.google.gson.Gson;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.fooshapvp.data.MembershipStatus;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.plugins.microbot.util.misc.Operation;
import javax.inject.Inject;
import java.awt.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static net.runelite.api.ObjectID.*;

@PluginDescriptor(

        name = PluginDescriptor.Elaida + "Flipper Thing",
        description = "Flips stuff",
        tags = {"money", "bitches"},
        enabledByDefault = false
)
@Slf4j
public class FlipperOnePlugin extends Plugin {

    public boolean members_update_latch = false;
    private Gson gson;

    private HttpClient client;
    @Inject
    private FlipperOneConfig config;

    @Provides
    FlipperOneConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(FlipperOneConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;

    @Inject
    FlipperOneScript flipperOneScript;


    @Override
    protected void startUp() throws AWTException {
        client = HttpClient.newHttpClient();
        gson = new Gson();
        members_update_latch = false;

        if (overlayManager != null) {
            //overlayManager.add(agilityOverlay);
        }
        flipperOneScript.run(config, this);


    }

    protected void shutDown() {
        members_update_latch = false;
        flipperOneScript.shutdown();
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!members_update_latch){
            Widget membersTab = Rs2Widget.getWidget(109,31);
            if (membersTab != null) {
                MembershipStatus m_s = new MembershipStatus();
                m_s.setMembership(membersTab.getText());
                m_s.setDisplay_name(flipperOneScript.currentAccount.getUsername());

                var m_s_payload = gson.toJson(m_s);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://192.168.1.193:3146/" + Rs2Player.getLocalPlayer().getName().replace(" ", "_") + "/membership"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(m_s_payload))
                        .build();

                try {
                    HttpResponse<String> response = client.send(request,
                            HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 500) {
                        System.out.println("Got a 500 error");
                    } else {
                        System.out.println("Sent membership status: " + m_s_payload);


                    }

                } catch (Exception e) {
                    System.out.println("Got an exception: " + e.getMessage());
                }

                members_update_latch = true;
            } else {
                System.out.println("Widget is null");
                members_update_latch = false;
            }
        }
    }
}
