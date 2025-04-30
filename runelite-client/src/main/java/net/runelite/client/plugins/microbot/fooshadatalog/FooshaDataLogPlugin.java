package net.runelite.client.plugins.microbot.fooshadatalog;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.kit.KitType;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.fooshapvp.FooshaPVPPlugin;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import javax.inject.Inject;
import java.awt.*;
import java.net.http.HttpClient;
import java.util.HashMap;

@PluginDescriptor(

        name = PluginDescriptor.Elaida + "DataLog",
        description = "Logs data and whatnot",
        tags = {"money", "bitches"},
        enabledByDefault = false
)
@Slf4j
public class FooshaDataLogPlugin extends Plugin {

    private static final String MENU_OPTION = "<col=808080>Log";
    private String playerToLog;

    private int gameTick = 0;

    @Inject
    private MenuManager menuManager;

    @Override
    protected void startUp() throws AWTException {
        menuManager.addPlayerMenuItem(MENU_OPTION);
    }

    protected void shutDown() {
        menuManager.removePlayerMenuItem(MENU_OPTION);
        this.gameTick = 0;
        playerToLog = null;
    }

    @Subscribe(priority = 0)
    public void onGameTick(GameTick event) {
        gameTick++;

        Player local = Microbot.getClient().getLocalPlayer();

        System.out.println("=".repeat(20) + "Tick: " + gameTick + "=".repeat(20));
        System.out.println("Local player: ");
        printPlayerData(Microbot.getClient().getLocalPlayer());

        Player targetPlayer = Rs2Player.getPlayer(playerToLog);

        if (targetPlayer != null) {
            System.out.println("Target player: ");
            printPlayerData(targetPlayer);
        }





    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (event.getMenuOption().equals(MENU_OPTION)) {
            String targetPlayer = event.getMenuTarget();
            String cleanName = FooshaPVPPlugin.extractPlayerName(targetPlayer);
            playerToLog = cleanName.replace("\u00A0", " ");
        }
    }

    private void printPlayerData(Player p) {
        System.out.println("    Weapon: " + p.getPlayerComposition().getEquipmentId(KitType.WEAPON));
        System.out.println("    Anims: " + " (A: " + p.getAnimation() + ") (P: " + p.getPoseAnimation() + ")");

        var spotAnims = p.getSpotAnims();

        if (spotAnims.iterator().hasNext()) {
            StringBuilder spotAnimString = new StringBuilder();
            spotAnimString.append("    Spot anims: ");

            for (var spotAnim : spotAnims
            ) {
                spotAnimString.append(spotAnim.getId()).append(",");
            }

//            System.out.println("        " + spotAnimString);
        }
    }

    @Subscribe(priority = 1)
    public void onProjectileMoved(ProjectileMoved event)
    {
        System.out.println("Projectile moved: " + event.getProjectile().getId());

    }
}