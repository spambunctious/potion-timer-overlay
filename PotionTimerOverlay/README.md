# Potion Timer Overlay

**Version 1.17.0**

Potion Timer Overlay is a RuneLite plugin that helps you keep track of active potion effects and buffs while playing Old School RuneScape. It displays customizable countdown timers and visual indicators directly on your inventory items, making it easier to see when an effect is about to expire.

## Features

- **Potion timers:** Display the remaining duration of active potion effects directly on inventory items.
- **Pie timers:** Show a circular countdown indicator over affected items.
- **Bar timers:** Display a customizable progress bar showing the remaining duration of an effect.
- **Expiration alerts:** Inventory icons pulse darker as an effect approaches expiration. Once expired, a white silhouette outline pulses around the item until the configured reset delay ends.
- **Customizable appearance:** Adjust timer visibility, colors, sizes, opacity, and other visual settings.
- **Individual effect settings:** Enable or disable timer displays for different potion and buff groups.

## Supported Effects

### Divine Potions
- Divine Bastion
- Divine Battlemage
- Divine Magic
- Divine Ranging
- Divine Super Attack
- Divine Super Combat
- Divine Super Defence
- Divine Super Strength

### Poison Protection
- Antipoisons
- Antidotes
- Sanfew Serum
- Anti-venom
- Anti-venom+
- Extended Anti-venom+

### Hearts
- Imbued Heart
- Saturated Heart

### Other Buffs
- Smelling Salts
- Prayer Enhance
- Overloads
- Prayer Regeneration
- Stamina and Extended Stamina
- Antifire variants
- Goading
- Menaphite Remedy
- Liquid Adrenaline
- Surge
- Moonlight Potions

## Configuration

After installing the plugin, open the RuneLite configuration panel using the wrench icon and select **Potion Timer Overlay**.

The plugin provides individual display settings for each supported effect group, along with shared appearance settings.

### Potion Timer
Customize the countdown text displayed over inventory items.

- Enable or disable the timer text.
- Adjust text size.
- Choose the font.
- Customize text color.

### Blinking
Control the visual alerts displayed as potion effects expire.

- **Blinking threshold:** Choose how many seconds before expiration the inventory icon begins pulsing darker. Set to `0` to disable the automatic pre-expiration pulse.
- **Blinking icon reset delay:** Set how long the white outline remains visible after an effect expires.

### Visual Timers
Customize the appearance of pie and bar countdown indicators.

- Enable or disable pie and bar timers.
- Choose pie chart size.
- Adjust bar orientation and thickness.
- Customize fill, background, and border colors.
- Adjust background color opacity.
- Adjust overall visual timer opacity.
- Set border thickness to `0` to hide the outline.

### Venom Color
Choose a separate visual color for the venom-immunity phase of Anti-venom effects. Once venom immunity ends, the timer switches to the regular poison-protection phase.

## Debug Previews

The Debug section includes optional visual previews to help configure the plugin.

Available previews include:
- Five-minute potion timer.
- Expired potion appearance.
- Near-expiration blinking.
- Anti-venom protection phases.