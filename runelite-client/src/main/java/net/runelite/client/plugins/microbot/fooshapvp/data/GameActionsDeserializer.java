package net.runelite.client.plugins.microbot.fooshapvp.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GameActionsDeserializer {

    // Base Action class
    public static abstract class Action {
        private final String type;

        protected Action(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        @Override
        public String toString() {
            return "Action{type='" + type + "'}";
        }
    }

    // Attack Action
    public static class AttackAction extends Action {
        private final String target;

        public AttackAction(String target) {
            super("attack");
            this.target = target;
        }

        public String getTarget() {
            return target;
        }

        @Override
        public String toString() {
            return "AttackAction{target='" + target + "'}";
        }
    }

    // Check recoil action

    public static class CheckRecoilAction extends Action {

        public CheckRecoilAction() {
            super("CheckRecoil");
        }
    }

    // Movement Action
    public static class MoveAction extends Action {
        private final int[] position;

        public MoveAction(int[] position) {
            super("move");
            this.position = position;
        }

        public int[] getPosition() {
            return position;
        }

        @Override
        public String toString() {
            return "MoveAction{position=[" + position[0] + ", " + position[1] + "]}";
        }
    }

    // Inventory Action
    public static class InventoryAction extends Action {
        private final String actionType;
        private final int itemId;
        private final int slotIndex;

        public InventoryAction(String actionType, int itemId, int slotIndex) {
            super("inventory");
            this.actionType = actionType;
            this.itemId = itemId;
            this.slotIndex = slotIndex;
        }

        public String getActionType() {
            return actionType;
        }

        public int getItemId() {
            return itemId;
        }

        public int getSlotIndex() {
            return slotIndex;
        }

        @Override
        public String toString() {
            return "InventoryAction{" +
                    "actionType='" + actionType + '\'' +
                    ", itemId=" + itemId +
                    ", slotIndex=" + slotIndex +
                    '}';
        }
    }

    // Special Attack Action
    public static class SpecialAttackAction extends Action {
        public SpecialAttackAction() {
            super("special");
        }

        @Override
        public String toString() {
            return "SpecialAttackAction{}";
        }
    }

    // Prayer Action
    public static class PrayerAction extends Action {
        private final String prayer;

        public PrayerAction(String prayer) {
            super("prayer");
            this.prayer = prayer;
        }

        public String getPrayer() {
            return prayer;
        }

        @Override
        public String toString() {
            return "PrayerAction{prayers=" + prayer + "}";
        }
    }

    // Custom deserializer for Action objects
    public static class ActionDeserializer implements JsonDeserializer<Action> {
        @Override
        public Action deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            String type = jsonObject.get("type").getAsString();

            switch (type) {
                case "attack":
                    return new AttackAction(jsonObject.get("target").getAsString());

                case "move":
                    int[] position = context.deserialize(jsonObject.get("position"), int[].class);
                    return new MoveAction(position);

                case "inventory":
                    String actionType = jsonObject.get("actionType").getAsString();
                    int itemId = jsonObject.get("itemId").getAsInt();
                    int slotIndex = jsonObject.get("slotIndex").getAsInt();
                    return new InventoryAction(actionType, itemId, slotIndex);

                case "special":
                    return new SpecialAttackAction();

                case "prayer":
                    String prayer = jsonObject.get("prayer").getAsString();
                    return new PrayerAction(prayer);

                default:
                    throw new JsonParseException("Unknown action type: " + type);
            }
        }
    }

    /**
     * Parses a JSON string and always returns a list of actions.
     * The JSON can either be a direct action array or a JSON object with an "action" field containing an array.
     *
     * @param jsonString The JSON string to parse
     * @return A list of Action objects, never null (empty list if no actions are found)
     */
    public static List<Action> parseActions(String jsonString) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Action.class, new ActionDeserializer())
                .create();

        try {
            // First try to parse as a JsonObject to check for "action" field
            JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);

            if (jsonObject.has("action")) {
                // Handle the case with a top-level "action" field
                JsonElement actionElement = jsonObject.get("action");

                if (actionElement.isJsonArray()) {
                    // It's an array of actions
                    Type actionListType = new TypeToken<List<Action>>(){}.getType();
                    return gson.fromJson(actionElement, actionListType);
                } else if (actionElement.isJsonObject()) {
                    // It's a single action
                    Action singleAction = gson.fromJson(actionElement, Action.class);
                    return Collections.singletonList(singleAction);
                }
            } else if (jsonObject.has("type")) {
                // It's a single action JSON without wrapper
                Action singleAction = gson.fromJson(jsonObject, Action.class);
                return Collections.singletonList(singleAction);
            }

            // Try parsing as direct array
            Type actionListType = new TypeToken<List<Action>>(){}.getType();
            List<Action> actionList = gson.fromJson(jsonString, actionListType);
            if (actionList != null) {
                return actionList;
            }

        } catch (Exception e) {
            System.err.println("Error parsing actions: " + e.getMessage());
        }

        // Return empty list if parsing fails
        return new ArrayList<>();
    }

    public static void main(String[] args) {
        // Create a Gson instance with our custom deserializer
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Action.class, new ActionDeserializer())
                .setPrettyPrinting()
                .create();

        // Sample JSON examples from the documentation
        String attackJson = "{\"type\": \"attack\", \"target\": \"opponent\"}";
        String moveJson = "{\"type\": \"move\", \"position\": [10, 20]}";
        String inventoryJson = "{\"type\": \"inventory\", \"actionType\": \"eat\", \"itemId\": 385, \"slotIndex\": 3}";
        String specialJson = "{\"type\": \"special\"}";
        String prayerJson = "{\"type\": \"prayer\", \"prayers\": [\"Piety\", \"Protect from Melee\"]}";

        // JSON array example
        String combinedJson = "[" +
                "{\"type\": \"inventory\", \"actionType\": \"eat\", \"itemId\": 385, \"slotIndex\": 3}," +
                "{\"type\": \"inventory\", \"actionType\": \"equip\", \"itemId\": 4153, \"slotIndex\": 5}," +
                "{\"type\": \"special\"}," +
                "{\"type\": \"attack\", \"target\": \"opponent\"}" +
                "]";

        // Parse individual actions
        Action attackAction = gson.fromJson(attackJson, Action.class);
        Action moveAction = gson.fromJson(moveJson, Action.class);
        Action inventoryAction = gson.fromJson(inventoryJson, Action.class);
        Action specialAction = gson.fromJson(specialJson, Action.class);
        Action prayerAction = gson.fromJson(prayerJson, Action.class);

        // Print individual actions
        System.out.println("Attack Action: " + attackAction);
        System.out.println("Move Action: " + moveAction);
        System.out.println("Inventory Action: " + inventoryAction);
        System.out.println("Special Action: " + specialAction);
        System.out.println("Prayer Action: " + prayerAction);

        // Parse and print combined actions
        Type actionListType = new TypeToken<List<Action>>(){}.getType();
        List<Action> combinedActions = gson.fromJson(combinedJson, actionListType);

        System.out.println("\nCombined Actions:");
        for (Action action : combinedActions) {
            System.out.println("  " + action);
        }
    }
}