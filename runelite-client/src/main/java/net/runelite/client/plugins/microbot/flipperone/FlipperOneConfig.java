package net.runelite.client.plugins.microbot.flipperone;

import net.runelite.client.config.*;

import java.io.File;

@ConfigGroup("FlipperOneConfig")
@ConfigInformation("Flips shit and whatnot")
public interface FlipperOneConfig extends Config {
    @ConfigSection(
            name = "General",
            description = "General",
            position = 0,
            closedByDefault = false
    )
    String generalSection = "general";


    @ConfigItem(
            keyName = "Accounts",
            name = "Accounts.txt location",
            description = "Accounts on newlines, username:password",
            position = 1,
            section = generalSection
    )
    default String accounts()
    {
        String homeDirectory = System.getProperty("user.home");
        return homeDirectory + File.separator + "~/accounts.txt";
    }





}