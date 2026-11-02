package com.mason.divinepotiontimer;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.BasicStroke;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import javax.inject.Inject;
import net.runelite.api.ItemComposition;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.util.AsyncBufferedImage;

class DivinePotionTimerOverlay extends WidgetItemOverlay
{
    private static final float DARK_BRIGHTNESS = 0.50f;

    private final DivinePotionTimerPlugin plugin;
    private final DivinePotionTimerConfig config;
    private final ItemManager itemManager;
    private final Map<Integer, BufferedImage> darkenedImages = new ConcurrentHashMap<>();
    private final Map<Integer, BufferedImage> whiteOutlineImages = new ConcurrentHashMap<>();
    private final Set<Integer> requestedImages = ConcurrentHashMap.newKeySet();
    private final Set<Integer> requestedWhiteOutlineImages = ConcurrentHashMap.newKeySet();

    @Inject
    private DivinePotionTimerOverlay(
        DivinePotionTimerPlugin plugin,
        DivinePotionTimerConfig config,
        ItemManager itemManager)
    {
        this.plugin = plugin;
        this.config = config;
        this.itemManager = itemManager;

        showOnInventory();
        setPriority(PRIORITY_HIGH);
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
    {
        ItemComposition itemComposition = itemManager.getItemComposition(itemId);
        if (itemComposition == null)
        {
            return;
        }

        Rectangle bounds = widgetItem.getCanvasBounds();
        if (bounds == null)
        {
            return;
        }

        final long now = System.currentTimeMillis();
        DivinePotion divinePotion = DivinePotion.fromItemName(itemComposition.getName());
        if (divinePotion != null)
        {
            renderDivinePotion(graphics, itemId, bounds, divinePotion, now);
            return;
        }

        InventoryBuff inventoryBuff = InventoryBuff.fromItemName(itemComposition.getName());
        if (inventoryBuff != null)
        {
            renderInventoryBuff(graphics, itemId, bounds, inventoryBuff, now);
        }
    }

    private void renderDivinePotion(Graphics2D graphics, int itemId, Rectangle bounds,
        DivinePotion potion, long now)
    {
        boolean enabled = config.showDivineTimer();
        PotionTimer timer = plugin.getTimer(potion);
        boolean active = enabled && timer != null && timer.isActive(now);
        int secondsRemaining = active ? timer.getSecondsRemaining(now) : 0;

        // Test options deliberately remain usable even when the live divine
        // display is disabled. The near-expiration test only pulses the icon;
        // only Test Potion Timer creates a simulated countdown and indicators.
        boolean testTimer = config.testTimerDisplay();
        TestTimerPreview testPreview = plugin.getTestPotionTimerPreview(testTimer, now);
        if (testPreview != null)
        {
            secondsRemaining = testPreview.getSecondsRemaining();
        }

        boolean testBlink = config.testBlinkingAnimation();
        boolean blinkWindowReached = config.blinkStartSeconds() > 0
            && (active || testTimer)
            && secondsRemaining <= config.blinkStartSeconds();
        boolean pulse = testBlink || blinkWindowReached;
        boolean expiredOutline = enabled && !active && timer != null
            && timer.isWithinResetDelay(now, config.darkIconResetDelay());
        boolean showExpiredOutline = config.testExpiredPotion() || expiredOutline;

        drawInactiveIconState(graphics, itemId, bounds, now, showExpiredOutline, pulse);

        boolean displayTimer = (enabled && active) || testTimer;
        if (displayTimer)
        {
            double remainingFraction = testPreview != null
                ? testPreview.getRemainingFraction()
                : timer.getRemainingFraction(now);
            drawTimerDisplay(graphics, bounds, secondsRemaining, remainingFraction, config.timerTextColor());
        }
    }

    private void renderInventoryBuff(Graphics2D graphics, int itemId, Rectangle bounds,
        InventoryBuff buff, long now)
    {
        boolean enabled = isBuffDisplayEnabled(buff);
        boolean itemTestEnabled = buff == InventoryBuff.ANTIVENOM && config.testAntivenomTimer();

        // The shared Potion Timer test previews every recognized item family,
        // even when that family's live display is disabled. The near-expiration
        // test is intentionally separate and only pulses the icon without
        // creating a simulated timer or timer indicators.
        itemTestEnabled = itemTestEnabled || config.testTimerDisplay();

        boolean testExpiredPotion = config.testExpiredPotion();
        if (!enabled && !itemTestEnabled && !testExpiredPotion && !config.testBlinkingAnimation())
        {
            return;
        }

        if (buff == InventoryBuff.ANTIVENOM)
        {
            renderAntivenomItem(graphics, itemId, bounds, now);
            return;
        }

        PotionTimer timer = plugin.getTimer(buff);
        TestTimerPreview testPreview = plugin.getTestPotionTimerPreview(itemTestEnabled, now);
        boolean active = testPreview != null || (timer != null && timer.isActive(now));
        int secondsRemaining = testPreview != null
            ? testPreview.getSecondsRemaining()
            : (active ? timer.getSecondsRemaining(now) : 0);
        double remainingFraction = testPreview != null
            ? testPreview.getRemainingFraction()
            : (active && timer != null ? timer.getRemainingFraction(now) : 0.0);
        boolean pulse = config.testBlinkingAnimation()
            || (active && config.blinkStartSeconds() > 0
                && secondsRemaining <= config.blinkStartSeconds());

        // Anti-venom replaces the poison-protection state with its venom phase.
        // Do not darken ordinary antipoison bottles while that phase is active.
        TestTimerPreview testAntivenom = plugin.getTestAntivenomPreview(config.testAntivenomTimer(), now);
        PotionTimer antivenomTimer = plugin.getTimer(InventoryBuff.ANTIVENOM);
        boolean protectedByAntivenom = buff == InventoryBuff.ANTIPOISON
            && ((antivenomTimer != null && antivenomTimer.isActive(now))
                || (testAntivenom != null && testAntivenom.isVenomPhase()));
        boolean expiredOutline = enabled && !active && !protectedByAntivenom && timer != null
            && timer.isWithinResetDelay(now, config.darkIconResetDelay());
        boolean showExpiredOutline = testExpiredPotion || expiredOutline;

        drawInactiveIconState(graphics, itemId, bounds, now, showExpiredOutline, pulse);

        if (active && (enabled || itemTestEnabled))
        {
            drawTimerDisplay(graphics, bounds, secondsRemaining, remainingFraction, config.timerTextColor());
        }
    }

    private boolean isBuffDisplayEnabled(InventoryBuff buff)
    {
        switch (buff)
        {
            case ANTIPOISON: return config.showAntipoisonTimer();
            case ANTIVENOM: return config.showAntivenomTimer();
            case IMBUED_HEART: return config.showImbuedHeartTimer();
            case SMELLING_SALTS: return config.showSmellingSalts();
            case PRAYER_ENHANCE: return config.showPrayerEnhance();
            case OVERLOAD: return config.showOverloads();
            case PRAYER_REGENERATION: return config.showPrayerRegeneration();
            case STAMINA: return config.showStaminaPotion();
            case ANTIFIRE:
            case SUPER_ANTIFIRE: return config.showAntifirePotions();
            case GOADING_POTION: return config.showGoadingPotion();
            case MENAPHITE_REMEDY: return config.showMenaphiteRemedy();
            case LIQUID_ADRENALINE: return config.showLiquidAdrenaline();
            case SURGE_POTION: return config.showSurgePotion();
            case MOONLIGHT_POTION: return config.showMoonlightPotion();
            default: return false;
        }
    }

    private void renderAntivenomItem(Graphics2D graphics, int itemId, Rectangle bounds, long now)
    {
        boolean testEnabled = config.testAntivenomTimer();
        boolean testPotionTimer = config.testTimerDisplay();
        boolean testNearExpiration = config.testBlinkingAnimation();
        boolean testExpiredPotion = config.testExpiredPotion();
        if (!config.showAntivenomTimer() && !testEnabled && !testPotionTimer
            && !testNearExpiration && !testExpiredPotion)
        {
            return;
        }

        TestTimerPreview testPreview = plugin.getTestAntivenomPreview(testEnabled, now);
        // Do not include testNearExpiration here: that Debug option previews
        // only the blinking icon state, never a simulated countdown or pie/bar.
        TestTimerPreview generalTestPreview = testPreview == null
            ? plugin.getTestPotionTimerPreview(testPotionTimer, now)
            : null;
        PotionTimer venomTimer = plugin.getTimer(InventoryBuff.ANTIVENOM);
        PotionTimer poisonTimer = plugin.getTimer(InventoryBuff.ANTIPOISON);
        boolean venomActive = testPreview != null
            ? testPreview.isVenomPhase()
            : (generalTestPreview == null && venomTimer != null && venomTimer.isActive(now));
        boolean poisonActive = testPreview != null
            ? !testPreview.isVenomPhase()
            : (generalTestPreview != null || (poisonTimer != null && poisonTimer.isActive(now)));
        boolean effectActive = testPreview != null || generalTestPreview != null || venomActive || poisonActive;

        // The game exposes one poison-protection value with two sequential
        // phases. The test preview follows the same display transition.
        boolean displayTimer = false;
        int secondsRemaining = 0;
        double remainingFraction = 0.0;
        Color timerColor = config.timerTextColor();
        if (testPreview != null)
        {
            displayTimer = true;
            secondsRemaining = testPreview.getSecondsRemaining();
            remainingFraction = testPreview.getRemainingFraction();
            if (config.showAntivenomPhaseTimer() && testPreview.isVenomPhase())
            {
                timerColor = config.useCustomColorForVenom()
                    ? config.venomTimerTextColor()
                    : config.timerTextColor();
            }
            else if (!config.showAntivenomPhaseTimer())
            {
                // The test cycle uses a 60-second venom phase followed by
                // 120 seconds of poison protection.
                if (testPreview.isVenomPhase())
                {
                    secondsRemaining += 120;
                }
                remainingFraction = Math.min(1.0, secondsRemaining / 180.0);
            }
        }
        else if (generalTestPreview != null)
        {
            displayTimer = true;
            secondsRemaining = generalTestPreview.getSecondsRemaining();
            remainingFraction = generalTestPreview.getRemainingFraction();
        }
        else
        {
            PotionTimer displayTimerSource;
            if (!config.showAntivenomPhaseTimer())
            {
                displayTimerSource = plugin.getCombinedAntivenomTimer();
            }
            else if (venomActive)
            {
                displayTimerSource = venomTimer;
                timerColor = config.useCustomColorForVenom()
                    ? config.venomTimerTextColor()
                    : config.timerTextColor();
            }
            else
            {
                displayTimerSource = poisonActive ? poisonTimer : null;
            }

            if (displayTimerSource != null && displayTimerSource.isActive(now))
            {
                displayTimer = true;
                secondsRemaining = displayTimerSource.getSecondsRemaining(now);
                remainingFraction = displayTimerSource.getRemainingFraction(now);
            }
        }

        boolean pulse = testNearExpiration
            || (displayTimer && config.blinkStartSeconds() > 0
                && secondsRemaining <= config.blinkStartSeconds());

        PotionTimer resetTimer = !config.showAntivenomPhaseTimer()
            ? plugin.getCombinedAntivenomTimer()
            : (poisonTimer != null ? poisonTimer : venomTimer);
        boolean expiredOutline = !effectActive && resetTimer != null
            && resetTimer.isWithinResetDelay(now, config.darkIconResetDelay());
        boolean showExpiredOutline = testExpiredPotion || expiredOutline;
        drawInactiveIconState(graphics, itemId, bounds, now, showExpiredOutline, pulse);

        if (displayTimer && (config.showAntivenomTimer() || testEnabled || generalTestPreview != null))
        {
            drawTimerDisplay(graphics, bounds, secondsRemaining, remainingFraction, timerColor);
        }
        // If the venom-phase display is disabled, venom immunity remains active
        // but its text and visual timer are hidden.
    }

    /**
     * Near expiration, briefly darkens the original item sprite. Once the effect
     * expires, the sprite returns to its normal brightness and only its white
     * silhouette outline continues pulsing until the reset delay ends.
     */
    private void drawInactiveIconState(Graphics2D graphics, int itemId, Rectangle bounds,
        long now, boolean showExpiredOutline, boolean pulse)
    {
        if (showExpiredOutline)
        {
            drawWhiteBlinkingOutline(graphics, itemId, bounds, now);
            return;
        }

        if (pulse)
        {
            // Fade the 50%-brightness sprite over the original icon. Its alpha
            // mask preserves the potion silhouette and leaves the inventory slot untouched.
            drawBlinkingImage(graphics, itemId, bounds, now);
        }
    }

    private void drawTimerDisplay(Graphics2D graphics, Rectangle bounds, int seconds,
        double fraction, Color textColor)
    {
        drawTimerIndicators(graphics, bounds, fraction);
        if (config.showTimer())
            drawTimer(graphics, bounds, formatTime(seconds), config.timerFontSize(), config.timerFont(), textColor);
    }

    private void drawTimerIndicators(Graphics2D graphics, Rectangle bounds, double fraction)
    {
        float opacity = Math.max(0, Math.min(100, config.timerVisualOpacity())) / 100.0f;
        if (opacity <= 0.0f)
        {
            return;
        }

        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            if (config.pieChartSize() != TimerPieChartSize.NONE)
                drawPieTimer(g, bounds, fraction, config.timerBarColor(), config.timerBackgroundColor(),
                    config.pieChartSize(), config.borderColor(), config.borderWidth());
            drawBarTimer(g, bounds, fraction, config.timerBarColor(), config.timerBackgroundColor(),
                config.barTimerStyle(), config.barWidth(), config.borderColor(), config.borderWidth());
        }
        finally
        {
            g.dispose();
        }
    }

    /** Draws a draining bar along the item's right or bottom edge. */
    private static void drawBarTimer(Graphics2D graphics, Rectangle bounds, double fractionRemaining,
        Color fill, Color background, TimerBarStyle style, int configuredWidth,
        Color borderColor, int configuredBorderWidth)
    {
        if (style == null || style == TimerBarStyle.NONE || bounds.width < 8 || bounds.height < 8) return;
        double fraction = Math.max(0, Math.min(1, fractionRemaining));
        int thickness = Math.max(1, Math.min(12, configuredWidth));
        boolean vertical = style == TimerBarStyle.VERTICAL;
        int width = vertical ? Math.min(thickness, bounds.width - 4) : Math.max(4, bounds.width - 4);
        int height = vertical ? Math.max(4, bounds.height - 4) : Math.min(thickness, bounds.height - 4);
        int x = vertical ? bounds.x + bounds.width - width - 1 : bounds.x + 2;
        int y = vertical ? bounds.y + 2 : bounds.y + bounds.height - height - 1;
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setColor(withAlpha(background, 190));
            g.fillRect(x, y, width, height);
            int borderWidth = Math.max(0, Math.min(8, configuredBorderWidth));
            int inset = borderWidth > 0 && Math.min(width, height) >= 3 ? 1 : 0;
            int innerX = x + inset, innerY = y + inset;
            int innerWidth = Math.max(1, width - 2 * inset), innerHeight = Math.max(1, height - 2 * inset);
            g.setColor(withAlpha(fill, 235));
            int filled = (int) Math.round((vertical ? innerHeight : innerWidth) * fraction);
            if (filled > 0)
                g.fillRect(innerX, innerY + (vertical ? innerHeight - filled : 0),
                    vertical ? innerWidth : filled, vertical ? filled : innerHeight);
            if (borderWidth > 0)
            {
                float stroke = Math.min(borderWidth, Math.max(1, Math.min(width, height) - 1));
                g.setStroke(new BasicStroke(stroke));
                g.setColor(withAlpha(borderColor, 255));
                g.draw(new Rectangle2D.Float(x + stroke / 2, y + stroke / 2,
                    Math.max(0, width - stroke), Math.max(0, height - stroke)));
            }
        }
        finally { g.dispose(); }
    }

    /** Draws the remaining-time pie in the selected size and position. */
    private static void drawPieTimer(Graphics2D graphics, Rectangle bounds, double fractionRemaining,
        Color fill, Color background, TimerPieChartSize sizeMode,
        Color borderColor, int configuredBorderWidth)
    {
        if (sizeMode == TimerPieChartSize.NONE) return;
        double fraction = Math.max(0, Math.min(1, fractionRemaining));
        int min = Math.min(bounds.width, bounds.height);
        int size = sizeMode == TimerPieChartSize.LARGE
            ? Math.max(18, Math.min(28, (int) Math.round(min * 0.78)))
            : Math.max(9, Math.min(12, min / 2));
        int x = sizeMode == TimerPieChartSize.LARGE
            ? bounds.x + (bounds.width - size) / 2 : bounds.x + bounds.width - size - 1;
        int y = sizeMode == TimerPieChartSize.LARGE
            ? bounds.y + (bounds.height - size) / 2 : bounds.y + bounds.height - size - 1;
        int bgAlpha = sizeMode == TimerPieChartSize.LARGE ? 145 : 205;
        double bleed = 0.6;
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setColor(withAlpha(background, bgAlpha));
            g.fillOval(x, y, size, size);
            if (fraction > 0)
            {
                g.setColor(withAlpha(fill, sizeMode == TimerPieChartSize.LARGE ? 215 : 245));
                g.fill(new Arc2D.Double(x - bleed, y - bleed, size + 2 * bleed, size + 2 * bleed,
                    90, -360 * fraction, Arc2D.PIE));
            }
            int borderWidth = Math.max(0, Math.min(8, configuredBorderWidth));
            if (borderWidth > 0)
            {
                float stroke = Math.min(borderWidth, Math.max(1, size - 1));
                double radius = size / 2.0 + bleed + 0.5 - stroke / 2.0;
                double cx = x + size / 2.0, cy = y + size / 2.0;
                g.setStroke(new BasicStroke(stroke));
                g.setColor(withAlpha(borderColor, 255));
                g.draw(new Ellipse2D.Double(cx - radius, cy - radius, 2 * radius, 2 * radius));
            }
        }
        finally { g.dispose(); }
    }

    private static Color withAlpha(Color color, int alpha)
    {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private void drawBlinkingImage(Graphics2D graphics, int itemId, Rectangle bounds, long now)
    {
        drawImage(graphics, getDarkenedImage(itemId), bounds, getBlinkOpacity(now));
    }

    private static float getBlinkOpacity(long now)
    {
        return (float) ((Math.sin(now % 1000L / 1000.0 * Math.PI * 2) + 1) * 0.5);
    }

    /** Draws an asynchronously cached sprite copy without changing the caller's graphics state. */
    private static void drawImage(Graphics2D graphics, BufferedImage image, Rectangle bounds, float opacity)
    {
        if (image == null || opacity <= 0) return;
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1, opacity)));
            g.drawImage(image, bounds.x, bounds.y, bounds.width, bounds.height, null);
        }
        finally { g.dispose(); }
    }

    private void drawWhiteBlinkingOutline(Graphics2D graphics, int itemId, Rectangle bounds, long now)
    {
        drawImage(graphics, getWhiteOutlineImage(itemId), bounds, 0.15f + getBlinkOpacity(now) * 0.85f);
    }

    private BufferedImage getWhiteOutlineImage(int itemId)
    {
        return getCachedImage(itemId, whiteOutlineImages, requestedWhiteOutlineImages,
            DivinePotionTimerOverlay::createWhiteOutlineImage);
    }

    private BufferedImage getDarkenedImage(int itemId)
    {
        return getCachedImage(itemId, darkenedImages, requestedImages,
            DivinePotionTimerOverlay::createDarkenedImage);
    }

    private BufferedImage getCachedImage(int itemId, Map<Integer, BufferedImage> cache,
        Set<Integer> requested, Function<BufferedImage, BufferedImage> transform)
    {
        BufferedImage cached = cache.get(itemId);
        if (cached != null || !requested.add(itemId)) return cached;
        AsyncBufferedImage source = itemManager.getImage(itemId);
        if (source == null)
        {
            requested.remove(itemId);
            return null;
        }
        source.onLoaded(() -> {
            cache.put(itemId, transform.apply(source));
            requested.remove(itemId);
        });
        return null;
    }

    /**
     * Creates a soft white outer edge from the source sprite's alpha silhouette.
     * The first pixel ring is bright and the second ring creates a subtle glow.
     * Pixels belonging to the original sprite remain transparent in this image,
     * so the outline never washes out the potion artwork itself.
     */
    static BufferedImage createWhiteOutlineImage(BufferedImage source)
    {
        int width = source.getWidth(), height = source.getHeight();
        int[] pixels = source.getRGB(0, 0, width, height, null, 0, width);
        int[] outline = new int[pixels.length];
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                if ((pixels[y * width + x] >>> 24) != 0) continue;
                int nearest = Integer.MAX_VALUE;
                for (int dy = -2; dy <= 2; dy++)
                    for (int dx = -2; dx <= 2; dx++)
                    {
                        int distance = dx * dx + dy * dy, sx = x + dx, sy = y + dy;
                        if (distance == 0 || distance > 4 || distance >= nearest
                            || sx < 0 || sx >= width || sy < 0 || sy >= height) continue;
                        if ((pixels[sy * width + sx] >>> 24) > 16) nearest = distance;
                    }
                int alpha = nearest <= 1 ? 245 : nearest <= 2 ? 155 : nearest <= 4 ? 75 : 0;
                if (alpha > 0) outline[y * width + x] = (alpha << 24) | 0x00FFFFFF;
            }
        }
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, width, height, outline, 0, width);
        return result;
    }

    /**
     * Creates a darkened copy of the real item sprite. The original cached
     * RuneLite image is never modified and its per-pixel alpha is preserved.
     */
    static BufferedImage createDarkenedImage(BufferedImage source)
    {
        int width = source.getWidth(), height = source.getHeight();
        int[] pixels = source.getRGB(0, 0, width, height, null, 0, width);
        for (int i = 0; i < pixels.length; i++)
        {
            int argb = pixels[i], alpha = argb >>> 24;
            if (alpha == 0) continue;
            pixels[i] = (alpha << 24)
                | ((int) (((argb >>> 16) & 0xFF) * DARK_BRIGHTNESS) << 16)
                | ((int) (((argb >>> 8) & 0xFF) * DARK_BRIGHTNESS) << 8)
                | (int) ((argb & 0xFF) * DARK_BRIGHTNESS);
        }
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, width, height, pixels, 0, width);
        return result;
    }

    void clearImageCache()
    {
        darkenedImages.clear();
        whiteOutlineImages.clear();
        requestedImages.clear();
        requestedWhiteOutlineImages.clear();
    }

    private static void drawTimer(Graphics2D graphics, Rectangle bounds, String text,
        int fontSize, TimerFont timerFont, Color textColor)
    {
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            Font font = timerFont.getFont().deriveFont((float) fontSize);
            g.setFont(font);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            int x = bounds.x + (bounds.width - g.getFontMetrics(font).stringWidth(text)) / 2;
            int y = bounds.y + bounds.height + 3;
            g.setComposite(AlphaComposite.SrcOver);
            g.setColor(new Color(0, 0, 0, 210));
            g.drawString(text, x + 1, y + 1);
            g.setColor(textColor);
            g.drawString(text, x, y);
        }
        finally { g.dispose(); }
    }

    private static String formatTime(int seconds)
    {
        int minutes = seconds / 60, remainder = seconds % 60;
        return minutes + ":" + (remainder < 10 ? "0" : "") + remainder;
    }
}
