package net.runelite.client.plugins.microbot.fooshapvp;

import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.fooshapvp.data.PlayerTrackingInfo;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Map;

public class FooshaPVPOverlay extends OverlayPanel {
    FooshaPVPPlugin fooshaPVPPlugin;
    @Inject
    FooshaPVPOverlay(FooshaPVPPlugin plugin) {
        super(plugin);
        this.fooshaPVPPlugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }

    /**
     * Draws a bar graph showing player distribution by combat level
     * @param g2d Graphics2D object to draw on
     * @param trackedPlayers Map containing player tracking information
     */
    public void drawCombatLevelDistribution(Graphics2D g2d, Map<String, PlayerTrackingInfo> trackedPlayers) {
        // Configure graph dimensions
        int graphWidth = 600;
        int graphHeight = 300;
        int margin = 15;

        // Combat level range
        int minLevel = 3;
        int maxLevel = 126;
        int levelRange = maxLevel - minLevel + 1;

        // Calculate bar width based on available space
        int barWidth = (graphWidth - 2 * margin) / levelRange;

        // Count players at each combat level
        int[] playerCounts = new int[levelRange];
        for (PlayerTrackingInfo info : trackedPlayers.values()) {
            int level = info.getPlayer().getCombatLevel();
            if (level >= minLevel && level <= maxLevel) {
                playerCounts[level - minLevel]++;
            }
        }

        // Find maximum count for scaling
        int maxCount = 0;
        for (int count : playerCounts) {
            maxCount = Math.max(maxCount, count);
        }

        // Calculate scaling factor for bar height
        double scaleFactor = (double)(graphHeight - 2 * margin) / (maxCount > 0 ? maxCount : 1);

        // Draw axes
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));

        // Draw x-axis
        g2d.drawLine(margin, graphHeight - margin, graphWidth - margin, graphHeight - margin);

        // Draw y-axis
        g2d.drawLine(margin, margin, margin, graphHeight - margin);

        // Draw bars
        for (int i = 0; i < levelRange; i++) {
            int level = i + minLevel;
            int count = playerCounts[i];

            int barHeight = (int)(count * scaleFactor);
            int x = margin + i * barWidth;
            int y = graphHeight - margin - barHeight;

            // Set bar color (can customize based on preferences)
            g2d.setColor(new Color(30, 144, 255, 200));  // Semi-transparent blue

            // Draw the bar
            g2d.fillRect(x, y, barWidth - 1, barHeight);

            // Draw bar border
            g2d.setColor(Color.BLACK);
            g2d.drawRect(x, y, barWidth - 1, barHeight);

            // Draw level labels for every 10th level or so to avoid crowding
            if (level % 10 == 0 || level == minLevel || level == maxLevel) {
                g2d.setColor(Color.BLACK);
                String levelStr = String.valueOf(level);
                FontMetrics fontMetrics = g2d.getFontMetrics();
                int textWidth = fontMetrics.stringWidth(levelStr);

                g2d.drawString(levelStr, x + (barWidth - textWidth) / 2, graphHeight - margin + 15);
            }
        }

        // Draw scale for y-axis
        g2d.setColor(Color.BLACK);
        int yTickCount = 5;
        for (int i = 0; i <= yTickCount; i++) {
            int tickValue = (maxCount * i) / yTickCount;
            int y = graphHeight - margin - (int)(tickValue * scaleFactor);
            g2d.drawLine(margin - 5, y, margin, y);
            g2d.drawString(String.valueOf(tickValue), margin - 35, y + 5);
        }

        // Draw title and labels
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("Player Distribution by Combat Level", graphWidth / 2 - 150, margin / 2);

        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Combat Level", graphWidth / 2 - 40, graphHeight - 10);

        // Rotate text for y-axis label
        AffineTransform originalTransform = g2d.getTransform();
        g2d.rotate(-Math.PI/2, margin / 2, graphHeight / 2);
        g2d.drawString("Number of Players", margin / 2 - 50, graphHeight / 2 + 5);
        g2d.setTransform(originalTransform);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            drawCombatLevelDistribution(graphics, fooshaPVPPlugin.trackedPlayers);
            //fooshaPVPPlugin.trackedPlayers
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return super.render(graphics);
    }
}
