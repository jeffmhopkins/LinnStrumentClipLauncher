// PaintPage.java
package com.theslowgrowth;

import static com.theslowgrowth.Color.*;

public class PaintPage extends LinnstrumentPage {

    private static final int WIDTH  = 26;
    private static final int HEIGHT = 8;
    private Color currentColor = WHITE;
    private boolean paletteOpen = false;
    private boolean erase_pressed = false;
    private final Color[][] painted = new Color[WIDTH][HEIGHT];

    PaintPage(int width, int height, LinnstrumentClipLauncherExtension parent) {
        super(width, height, parent);
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                painted[x][y] = OFF;
            }
        }
    }

    @Override
    protected void showImpl() {
        getParent().getHost().showPopupNotification("LinnStrument Paint");
        redrawEverything();
        setLED(0, 0, paletteOpen ? (currentColor == OFF ? WHITE : currentColor) : MAGENTA);
        setLED(0,5, RED);
    }

    @Override
    protected void hideImpl() {
        // Nothing needed
    }

    @Override
    public void buttonDown(int x, int y, int velocity) {
        if (x == 0 && y == 1 && velocity > 0) {
            erase_pressed = true;
            return;
        }
        if (x == 0 && y == 0) {
            paletteOpen = !paletteOpen;
            redrawEverything();
            setLED(0, 0, paletteOpen ? (currentColor == OFF ? WHITE : currentColor) : MAGENTA);
            setLED(0,5, RED);
            return;
        }

        if (paletteOpen && y == 0) {
            Color[] colors = Color.values();
            if (x < colors.length) {
                currentColor = colors[x];
                setLED(0, 7, currentColor);
            }
            return;
        }
        if (x == 0 && y == 7) return;
        painted[x][y] = erase_pressed ? OFF : currentColor;
        setLED(x, y, erase_pressed ? OFF : currentColor);
    }

    @Override
    public void buttonUp(int x, int y) {
        if (x == 0 && y == 1) {
            erase_pressed = false;
        }
    }

    private void redrawEverything() {
        for (int x = 0; x < 26; x++) {
            for (int y = 0; y < 8; y++) {
                if (painted[x][y] != OFF) {
                    setLED(x, y, painted[x][y]);
                } else {
                    setLED(x, y, OFF);
                }
            }
        }
        if (paletteOpen) {
            Color[] colors = Color.values();
            for (int i = 0; i < colors.length && i < 26; i++) {
                setLED(i, 0, colors[i]);
            }
        }
    }
}