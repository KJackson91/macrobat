package net.runelite.client.plugins.microbot.flipperone;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.security.Encryption;
import net.runelite.client.util.WorldUtil;

import java.awt.event.KeyEvent;

import static net.runelite.client.plugins.microbot.util.Global.sleep;

public class FooshaLogin {
    public FooshaLogin(String username, String password, int world) {
        if(Microbot.isLoggedIn())
            return;
        if (Microbot.getClient().getLoginIndex() == 3 || Microbot.getClient().getLoginIndex() == 24) { // you were disconnected from the server.
            int loginScreenWidth = 804;
            int startingWidth = (Microbot.getClient().getCanvasWidth() / 2) - (loginScreenWidth / 2);
            Microbot.getMouse().click(365 + startingWidth, 308); //clicks a button "OK" when you've been disconnected
            sleep(600);
        }
        Rs2Keyboard.keyPress(KeyEvent.VK_ENTER);
        sleep(600);
        try {
            setWorld(world);
        } catch (Exception e) {
            System.out.println("Changing world failed");
        }
        Microbot.getClient().setUsername(username);
        try {
            Microbot.getClient().setPassword(password);
        } catch (Exception e) {
            System.out.println("no password has been set in the profile");
        }
        sleep(300);
        Rs2Keyboard.keyPress(KeyEvent.VK_ENTER);
        sleep(300);
        Rs2Keyboard.keyPress(KeyEvent.VK_ENTER);
        if (Microbot.getClient().getLoginIndex() == 10) {
            int loginScreenWidth = 804;
            int startingWidth = (Microbot.getClient().getCanvasWidth() / 2) - (loginScreenWidth / 2);
            Microbot.getMouse().click(365 + startingWidth, 250); //clicks a button "OK" when you've been disconnected
        } else if (Microbot.getClient().getLoginIndex() == 9) {
            int loginScreenWidth = 804;
            int startingWidth = (Microbot.getClient().getCanvasWidth() / 2) - (loginScreenWidth / 2);
            Microbot.getMouse().click(365 + startingWidth, 300); //clicks a button "OK" when you've been disconnected
        }
    }

    public void setWorld(int worldNumber) {
        try {
            net.runelite.http.api.worlds.World world = Microbot.getWorldService().getWorlds().findWorld(worldNumber);
            final net.runelite.api.World rsWorld = Microbot.getClient().createWorld();
            rsWorld.setActivity(world.getActivity());
            rsWorld.setAddress(world.getAddress());
            rsWorld.setId(world.getId());
            rsWorld.setPlayerCount(world.getPlayers());
            rsWorld.setLocation(world.getLocation());
            rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));
            Microbot.getClient().changeWorld(rsWorld);
        } catch (Exception ex) {
            System.out.println("Failed to find world");
        }
    }
}
