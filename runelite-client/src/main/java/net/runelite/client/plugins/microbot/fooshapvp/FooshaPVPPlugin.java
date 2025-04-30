package net.runelite.client.plugins.microbot.fooshapvp;

import com.google.gson.Gson;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.kit.KitType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.flipperone.FooshaPVPConfig;
import net.runelite.client.plugins.microbot.fooshapvp.data.CombatData;
import net.runelite.client.plugins.microbot.fooshapvp.data.GameActionsDeserializer;
import net.runelite.client.plugins.microbot.fooshapvp.data.PlayerTrackingInfo;
import net.runelite.client.plugins.microbot.fooshapvp.data.Session;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@PluginDescriptor(

        name = PluginDescriptor.Elaida + "PVP",
        description = "Kills noobs",
        tags = {"money", "bitches"},
        enabledByDefault = false
)
@Slf4j
public class FooshaPVPPlugin extends Plugin {


    private static final URI session_endpoint = URI.create("http://192.168.1.193:5010/api/create_session");
    @Inject
    private FooshaPVPConfig config;

    @Provides
    FooshaPVPConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(FooshaPVPConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;

    @Inject
    FooshaPVPScript fooshPVPScript;

    @Inject
    FooshaPVPOverlay fooshaPVPOverlay;

    @Inject
    private MenuManager menuManager;

    private int gameTick = 0;

    private Gson gson;

    private final Map<Skill, Integer> previousSkillExpTable = new EnumMap<>(Skill.class);
    public Map<String, PlayerTrackingInfo> trackedPlayers = new HashMap<>();

    private List<CombatData.Hitsplat> heroHitsplats;
    private List<CombatData.Hitsplat> opponentHitsplats;
    private Map<String, Integer> experienceDrops;
    private Integer hero_projectile_id;
    private Integer opponent_projectile_id;
    private String opponentName;
    private HttpClient client;
    private boolean shouldCheckRecoil = false;
    private boolean shouldStartSession = false;
    private boolean stop = false;
    private boolean sessionRunning = false;
    private int recoilRemaining = 0;
    private final List<String> equipOptions = new ArrayList<>(Arrays.asList("Wear", "Wield", "Equip"));
    private static final String MENU_OPTION = "<col=FF0000>MARK FOR DEATH";

    private ScheduledExecutorService exec = Executors.newScheduledThreadPool(10);


    @Override
    protected void startUp() throws AWTException {
//        if (overlayManager != null) {
//            overlayManager.add(fooshaPVPOverlay);
//        }
        fooshPVPScript.run(config);
        gson = new Gson();
        stop = false;
        client = HttpClient.newHttpClient();

        menuManager.addPlayerMenuItem(MENU_OPTION);

        experienceDrops = new HashMap<>();
        experienceDrops.put("attack", 0);
        experienceDrops.put("strength", 0);
        experienceDrops.put("defence", 0);
        experienceDrops.put("ranged", 0);
        experienceDrops.put("magic", 0);
    }

    protected void shutDown() {
        fooshPVPScript.clearActionQueue();
        fooshPVPScript.shutdown();
        //overlayManager.remove(fooshaPVPOverlay);
        menuManager.removePlayerMenuItem(MENU_OPTION);
        this.gameTick = 0;
        this.shouldCheckRecoil = false;
        this.sessionRunning = false;
        this.shouldStartSession = false;
        this.opponentName = null;
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {

    }

    @Subscribe(priority = 1)
    public void onFakeXpDrop(FakeXpDrop event)
    {
        ensureNonNullExperienceDrops();
        final Skill skill = event.getSkill();
        final int xp = event.getXp();

        int previous = Microbot.getClient().getSkillExperience(skill);

        var expGained = xp - previous;
        experienceDrops.put(event.getSkill().getName().toLowerCase(), expGained);
        //System.out.println("Exp fake drop detected, current drops: " + experienceDrops);

    }

    @Subscribe(priority = 1)
    public void onStatChanged(StatChanged event)
    {
        ensureNonNullExperienceDrops();
        final Skill skill = event.getSkill();
        final int xp = event.getXp();

        Integer previous = previousSkillExpTable.put(skill, xp);
        if (previous != null)
        {
            var expGained = xp - previous;
            experienceDrops.put(event.getSkill().getName().toLowerCase(), expGained);
            //System.out.println("Exp drop detected, current drops: " + experienceDrops);
        }

    }

    private String findMatchingEquipOption(String[] actions) {
        for (String action : actions) {
            if (equipOptions.contains(action)) {
                return action; // Return the first matching value
            }
        }
        return null; // Return null if no match is found
    }

    @Subscribe(priority = 0)
    public void onGameTick(GameTick event) {
        this.gameTick++;
        System.out.println("Start tick(" + gameTick + ") time: " + System.currentTimeMillis() + " Client tick: " + Microbot.getClient().getGameCycle());
        // Potentially check if any actions are left from the previous tick queue
        // handle by clearing + probably report the issue clearly in log
        //fooshPVPScript.tickActions.clear();

        if (shouldCheckRecoil) {
            fooshPVPScript.checkRecoil();
            shouldCheckRecoil = false;
            return;
        }

        if (shouldStartSession) {
            var newSession = new Session();
            newSession.setHero(Microbot.getClient().getLocalPlayer().getName());
            newSession.setOpponent(opponentName);
            newSession.setHero_recoil_charges(recoilRemaining);

            var sessionDataJson = gson.toJson(newSession);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(session_endpoint)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(sessionDataJson))
                    .build();

            try {
                HttpResponse<String> response = client.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 500) {
                    System.out.println("[Error] 500 Internal server error from new session API");
                } else {
                    //JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                    System.out.println("Got response: " + response.body());
                    shouldStartSession = false;
                    sessionRunning = true;
                }

            } catch (Exception e) {
                System.out.println("Got an exception when requesting new session \n " + e.getMessage());
            }
        }

        if (!sessionRunning) return;

        ensureNonNullHitsplats();
        ensureNonNullExperienceDrops();
        if (opponentName == null) return;
        //System.out.println("We are fighting: " + opponentName);
        var opponent = Rs2Player.getPlayer(opponentName);

        var tickData = new CombatData();
        var opponentEquipment = new CombatData.Equipment();
        if (opponent != null) {
            //System.out.println("Opponent was loaded");
            tickData.setOpponent_hp(opponent.getHealthRatio());
            tickData.setOpponent_animation(opponent.getAnimation());
            tickData.setOpponent_tile(new int[]{opponent.getWorldLocation().getX(), opponent.getWorldLocation().getY()});

            try {
                opponentEquipment.setAmmunition(-1);
                opponentEquipment.setHands(opponent.getPlayerComposition().getEquipmentId(KitType.HANDS));
                opponentEquipment.setFeet(opponent.getPlayerComposition().getEquipmentId(KitType.BOOTS));
                opponentEquipment.setRing(-1);
                opponentEquipment.setLegs(opponent.getPlayerComposition().getEquipmentId(KitType.LEGS));
                opponentEquipment.setShield(opponent.getPlayerComposition().getEquipmentId(KitType.SHIELD));
                opponentEquipment.setBody(opponent.getPlayerComposition().getEquipmentId(KitType.BOOTS));
                opponentEquipment.setWeapon(opponent.getPlayerComposition().getEquipmentId(KitType.WEAPON));
                opponentEquipment.setCape(opponent.getPlayerComposition().getEquipmentId(KitType.CAPE));
                opponentEquipment.setNeck(opponent.getPlayerComposition().getEquipmentId(KitType.AMULET));
                opponentEquipment.setHead(opponent.getPlayerComposition().getEquipmentId(KitType.HEAD));
            } catch (Exception e) {
                //System.out.println("Failed to set opponent equipment: " + e.getMessage());
                opponentEquipment.setAmmunition(-1);
                opponentEquipment.setHands(-1);
                opponentEquipment.setFeet(-1);
                opponentEquipment.setRing(-1);
                opponentEquipment.setLegs(-1);
                opponentEquipment.setShield(-1);
                opponentEquipment.setBody(-1);
                opponentEquipment.setWeapon(-1);
                opponentEquipment.setCape(-1);
                opponentEquipment.setNeck(-1);
                opponentEquipment.setHead(-1);
            }

            tickData.setOpponent_equipment(opponentEquipment);
        } else {
            //System.out.println("Opponent was not loaded");
            tickData.setOpponent_hp(-1);
            tickData.setOpponent_animation(-1);
            tickData.setOpponent_tile(new int[]{-1, -1});
            opponentEquipment.setAmmunition(-1);
            opponentEquipment.setHands(-1);
            opponentEquipment.setFeet(-1);
            opponentEquipment.setRing(-1);
            opponentEquipment.setLegs(-1);
            opponentEquipment.setShield(-1);
            opponentEquipment.setBody(-1);
            opponentEquipment.setWeapon(-1);
            opponentEquipment.setCape(-1);
            opponentEquipment.setNeck(-1);
            opponentEquipment.setHead(-1);
        }

        tickData.setTick(this.gameTick);
        tickData.setHero(Rs2Player.getLocalPlayer().getName());
        tickData.setOpponent(opponentName);
        tickData.setHero_hp(Rs2Player.getBoostedSkillLevel(Skill.HITPOINTS));
        tickData.setHero_spec(Rs2Combat.getSpecEnergy());
        tickData.setHero_tile(new int[]{Rs2Player.getWorldLocation().getX(), Rs2Player.getWorldLocation().getY()});

        var heroLevels = new CombatData.Levels();
        heroLevels.setAttack(new CombatData.SkillLevel(Rs2Player.getRealSkillLevel(Skill.ATTACK), Rs2Player.getBoostedSkillLevel(Skill.ATTACK)));
        heroLevels.setStrength(new CombatData.SkillLevel(Rs2Player.getRealSkillLevel(Skill.STRENGTH), Rs2Player.getBoostedSkillLevel(Skill.STRENGTH)));
        heroLevels.setDefence(new CombatData.SkillLevel(Rs2Player.getRealSkillLevel(Skill.DEFENCE), Rs2Player.getBoostedSkillLevel(Skill.DEFENCE)));
        heroLevels.setRanged(new CombatData.SkillLevel(Rs2Player.getRealSkillLevel(Skill.RANGED), Rs2Player.getBoostedSkillLevel(Skill.RANGED)));
        heroLevels.setMagic(new CombatData.SkillLevel(Rs2Player.getRealSkillLevel(Skill.MAGIC), Rs2Player.getBoostedSkillLevel(Skill.MAGIC)));
        heroLevels.setPrayer(new CombatData.SkillLevel(Rs2Player.getRealSkillLevel(Skill.PRAYER), Rs2Player.getBoostedSkillLevel(Skill.PRAYER)));
        tickData.setHero_levels(heroLevels);

        var heroPrayers = Arrays.stream(Rs2PrayerEnum.values())
                .filter(Rs2Prayer::isPrayerActive)
                .map(Rs2PrayerEnum::toString)
                .collect(Collectors.toList());

        var heroInventory = new ArrayList<CombatData.InventoryItem>();

        for (int i = 0; i <= 27; i++) {
            var invSlot = Rs2Inventory.getItemInSlot(i);
            if (invSlot != null) {
                heroInventory.add(new CombatData.InventoryItem(invSlot.getId(), invSlot.getQuantity()));
            } else {
                heroInventory.add(new CombatData.InventoryItem(-1, 0));
            }
        }
        tickData.setHero_inventory(heroInventory);

        var heroEquipment = new CombatData.Equipment();

        try {
            heroEquipment.setAmmunition(getNonNullEquipmentID(EquipmentInventorySlot.AMMO));
            heroEquipment.setHands(getNonNullEquipmentID(EquipmentInventorySlot.GLOVES));
            heroEquipment.setFeet(getNonNullEquipmentID(EquipmentInventorySlot.BOOTS));
            heroEquipment.setRing(getNonNullEquipmentID(EquipmentInventorySlot.RING));
            heroEquipment.setLegs(getNonNullEquipmentID(EquipmentInventorySlot.LEGS));
            heroEquipment.setShield(getNonNullEquipmentID(EquipmentInventorySlot.SHIELD));
            heroEquipment.setBody(getNonNullEquipmentID(EquipmentInventorySlot.BOOTS));
            heroEquipment.setWeapon(getNonNullEquipmentID(EquipmentInventorySlot.WEAPON));
            heroEquipment.setCape(getNonNullEquipmentID(EquipmentInventorySlot.CAPE));
            heroEquipment.setNeck(getNonNullEquipmentID(EquipmentInventorySlot.AMULET));
            heroEquipment.setHead(getNonNullEquipmentID(EquipmentInventorySlot.HEAD));
        } catch (Exception e) {
            //System.out.println("Failed to set local players equipment: " + e.getMessage());
        }


        tickData.setHero_equipment(heroEquipment);
        tickData.setHero_projectile_id(new Integer[]{hero_projectile_id});
        tickData.setOpponent_projectile_id(new Integer[]{opponent_projectile_id});


        tickData.setHero_hitsplats(heroHitsplats);
        tickData.setOpponent_hitsplats(opponentHitsplats);
        tickData.setHero_animation(Microbot.getClient().getLocalPlayer().getAnimation());

        tickData.setHero_xp_drops(experienceDrops);

        tickData.setHero_prayers(heroPrayers);

        var accessiblePoints = getAccessibleTileCoords(2);
        tickData.setHero_tile_access(accessiblePoints);

        //System.out.println("Plugin is requesting actions");
        fooshPVPScript.getAndDoStuff(tickData);

        clearTickInfo();
        System.out.println("End tick(" + gameTick + ") time: " + System.currentTimeMillis() + " Client tick: " + Microbot.getClient().getGameCycle());
    }

    // Helper function to check if a tile is walkable
    private boolean isPathable(WorldArea area, WorldPoint destination) {
        int dx = destination.getX() - area.getX();
        int dy = destination.getY() - area.getY();
        return area.canTravelInDirection(Microbot.getClient().getTopLevelWorldView(), dx, dy);
    }
    private int[][] getAccessibleTileCoords(int range) {
        List<int[]> accessibleTiles = new ArrayList<>();

        Player localPlayer = Microbot.getClient().getLocalPlayer();
        if (localPlayer == null) {
            return new int[0][0]; // Return an empty array if no player is found
        }

        WorldArea area = localPlayer.getWorldArea();
        if (area == null) {
            return new int[0][0]; // Return an empty array if no area is found
        }

        int initialX = area.getX() - range;
        int initialY = area.getY() - range;
        int maxX = area.getX() + range;
        int maxY = area.getY() + range;
        WorldView worldView = Microbot.getClient().getTopLevelWorldView();

        for (int x = initialX; x <= maxX; x++) {
            for (int y = initialY; y <= maxY; y++) {
                if (x == area.getX() && y == area.getY()) {
                    continue;
                }

                WorldPoint newSightWorldPoint = new WorldPoint(x, y, area.getPlane());

                if (!area.hasLineOfSightTo(worldView, newSightWorldPoint)) {
                    continue;
                }

                // Check if the tile is pathable
                if (isPathable(area, newSightWorldPoint)) {
                    accessibleTiles.add(new int[]{x, y});
                }
            }
        }

        return accessibleTiles.toArray(new int[0][0]);
    }

    private int getNonNullEquipmentID(EquipmentInventorySlot slot) {
        var equipmentID = Rs2Equipment.get(slot);
        if (equipmentID == null) return -1;
        return equipmentID.getId();
    }
    private void clearTickInfo() {
        heroHitsplats = null;
        opponentHitsplats = null;
        experienceDrops = new HashMap<>();
        experienceDrops.put("attack", 0);
        experienceDrops.put("strength", 0);
        experienceDrops.put("defence", 0);
        experienceDrops.put("ranged", 0);
        experienceDrops.put("magic", 0);

        hero_projectile_id = null;
        opponent_projectile_id = null;
    }

    @Subscribe(priority = 1)
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        this.gameTick = 0;
    }

    @Subscribe
    public void onChatMessage(ChatMessage event){
        if (event.getType() == ChatMessageType.GAMEMESSAGE){
            try {
                recoilRemaining = extractDamagePoints(event.getMessage());
                shouldStartSession = true;
            } catch (IllegalArgumentException e) {
                System.out.println("This is not the message we are looking for... " + event.getMessage());
            }
        }
    }

    @Subscribe(priority = 1)
    public void onProjectileMoved(ProjectileMoved event)
    {
        try {
            final Projectile projectile = event.getProjectile();

            if (projectile.getStartCycle() == Microbot.getClient().getGameCycle()){


                if (projectile.getInteracting().equals(Microbot.getClient().getLocalPlayer())) {

                    opponent_projectile_id = projectile.getId();
                } else if (projectile.getInteracting().getName().equals(opponentName)) {

                    hero_projectile_id = projectile.getId();
                }
            }
        } catch (Exception e) {
            System.out.println("Something went wrong in the projectile handler: " + e.getMessage());
        }

    }

    @Subscribe(priority = 10)
    public void onHitsplatApplied(HitsplatApplied event){

        ensureNonNullHitsplats();

        var hitSplat = new CombatData.Hitsplat();
        hitSplat.setDamage(event.getHitsplat().getAmount());
        hitSplat.setType(getNameFromValue(event.getHitsplat().getHitsplatType()));


        if (event.getActor().getName().equals(Microbot.getClient().getLocalPlayer().getName())){
            heroHitsplats.add(hitSplat);
        } else if (event.getActor().getName().equals(opponentName)) {
            opponentHitsplats.add(hitSplat);
        }
    }

    private void ensureNonNullHitsplats() {
        if (heroHitsplats == null) {
            heroHitsplats = new ArrayList<>();
        }

        if (opponentHitsplats == null) {
            opponentHitsplats = new ArrayList<>();
        }
    }

    private void ensureNonNullExperienceDrops() {
        if (experienceDrops == null) {
            experienceDrops = new HashMap<>();
            experienceDrops.put("attack", 0);
            experienceDrops.put("strength", 0);
            experienceDrops.put("defence", 0);
            experienceDrops.put("ranged", 0);
            experienceDrops.put("magic", 0);
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (event.getMenuOption().equals(MENU_OPTION)) {
            String targetPlayer = event.getMenuTarget();
            String cleanName = extractPlayerName(targetPlayer);
            opponentName = cleanName.replace("\u00A0", " ");
            shouldCheckRecoil = true;
        }
    }



    /**
     * Maps an integer value back to its corresponding constant name in lower snake_case format.
     *
     * @param value The integer value to map to a name
     * @return The name in lower snake_case format, or "unknown" if the value is not recognized
     */
    public static String getNameFromValue(int value) {
        switch (value) {
            case 12: return "block_me";
            case 13: return "block_other";
            case 16: return "damage_me";
            case 17: return "damage_other";
            case 65: return "poison";
            case 4: return "disease";
            case 5: return "venom";
            case 6: return "heal";
            case 11: return "cyan_up";
            case 15: return "cyan_down";
            case 18: return "damage_me_cyan";
            case 19: return "damage_other_cyan";
            case 20: return "damage_me_orange";
            case 21: return "damage_other_orange";
            case 22: return "damage_me_yellow";
            case 23: return "damage_other_yellow";
            case 24: return "damage_me_white";
            case 25: return "damage_other_white";
            case 43: return "damage_max_me";
            case 44: return "damage_max_me_cyan";
            case 45: return "damage_max_me_orange";
            case 46: return "damage_max_me_yellow";
            case 47: return "damage_max_me_white";
            case 53: return "damage_me_poise";
            case 54: return "damage_other_poise";
            case 55: return "damage_max_me_poise";
            case 0: return "corruption";
            case 60: return "prayer_drain";
            case 67: return "bleed";
            case 71: return "sanity_drain";
            case 72: return "sanity_restore";
            case 73: return "doom";
            case 74: return "burn";
            default: return "unknown";
        }
    }

    public static String extractPlayerName(String rawName) {
        if (rawName == null || rawName.isEmpty()) {
            return "";
        }

        // Remove color codes like <col=ffffff>
        String cleanName = rawName.replaceAll("<col=[0-9a-fA-F]+>", "").trim();

        // Remove level info like (level-72)
        int levelIndex = cleanName.lastIndexOf(" (level-");
        if (levelIndex != -1) {
            cleanName = cleanName.substring(0, levelIndex).trim();
        }

        return cleanName;
    }

    public static int extractDamagePoints(String input) {
        // Define a regular expression pattern that matches a number between "inflict " and " more points"
        String pattern = "inflict (\\d+) more points";

        // Create a Pattern object
        java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);

        // Create a Matcher object
        java.util.regex.Matcher m = r.matcher(input);

        // Check if the pattern was found
        if (m.find()) {
            // Extract and return the number as an integer
            return Integer.parseInt(m.group(1));
        } else {
            // Return -1 or throw an exception if the pattern isn't found
            throw new IllegalArgumentException("Could not extract damage points from input string");
        }
    }
}
