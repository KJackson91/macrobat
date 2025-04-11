package net.runelite.client.plugins.microbot.fooshapvp.data;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class MembershipStatus {
    @SerializedName(value = "display_name")
    private String display_name;

    private String membership;
}
