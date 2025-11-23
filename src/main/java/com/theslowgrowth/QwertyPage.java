package com.theslowgrowth;

import static com.theslowgrowth.Color.*;

public class QwertyPage extends LinnstrumentPage {

    private static final int WIDTH  = 26;
    private static final int HEIGHT = 8;

    private static final KeyDef[][] KEYBOARD = {
            // y = 0  – Empty row
            {k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),
                    k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),
                    k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),
                    k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),
                    k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF)},
            // y = 1  – Empty row
            {k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),
                    k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),
                    k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),
                    k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),
                    k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF)},
            // y = 2  – Special Characters row
            {k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(1,RED),          // E (escape or something)
                    k(2,MAGENTA),k(3,YELLOW),k(4,YELLOW),k(5,YELLOW),k(6,YELLOW),   // ~ ! @ # $
                    k(7,YELLOW),k(8,YELLOW),k(9,YELLOW),k(10,YELLOW),k(11,YELLOW),   // % ^ & * (
                    k(12,YELLOW),k(13,YELLOW),k(14,YELLOW),k(15,RED),k(0,OFF),       // ) _ + d
                    k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF)},
            // y = 3  – Numbers row
            {k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),
                    k(16,MAGENTA),k(17,BLUE),k(18,BLUE),k(19,BLUE),k(20,BLUE),       // ` 1 2 3 4
                    k(21,BLUE),k(22,BLUE),k(23,BLUE),k(24,BLUE),k(25,BLUE),         // 5 6 7 8 9
                    k(26,BLUE),k(27,MAGENTA),k(28,MAGENTA),k(29,RED),k(29,RED), // 0 [ ] \
                    k(29,RED),k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF)},
            // y = 4  – QWERTY top row
            {k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(30,LIME),
                    k(30,LIME),k(32,WHITE),k(33,WHITE),k(34,WHITE),k(35,WHITE),     // T Q W E R
                    k(36,WHITE),k(0,OFF),k(37,WHITE),k(38,WHITE),k(39,WHITE),       // T   Y U I
                    k(40,WHITE),k(41,WHITE),k(42,MAGENTA),k(43,MAGENTA),k(31,MAGENTA),           // O P B B
                    k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF)},
            // y = 5  – QWERTY middle row
            {k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(44,CYAN),
                    k(45,CYAN),k(46,WHITE),k(47,WHITE),k(48,CYAN),k(49,WHITE),      // s A S D F
                    k(50,WHITE),k(0,OFF),k(51,WHITE),k(52,WHITE),k(53,CYAN),        // G   H J K
                    k(54,WHITE),k(55,MAGENTA),k(56,MAGENTA),k(57,GREEN),k(57,GREEN),       // L E E E
                    k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF)},
            // y = 6  – QWERTY bottom row
            {k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(0,OFF),k(58,CYAN),
                    k(59,CYAN),k(60,WHITE),k(61,WHITE),k(62,WHITE),k(63,WHITE),     // S Z X C V
                    k(64,WHITE),k(0,OFF),k(65,WHITE),k(66,WHITE),k(67,MAGENTA),     // B   N M ,
                    k(68,MAGENTA),k(69,MAGENTA),k(0,OFF),k(70,GREEN),k(71,GREEN),   // . / e e
                    k(0,OFF),k(72,WHITE),k(0,OFF),k(0,OFF),k(73,GREEN)},            // u     e
            // y = 7  – Space / function row
            {k(0,OFF),k(0,OFF),k(74,GREEN),k(75,ORANGE),k(0,OFF),k(0,OFF),
                    k(0,OFF),k(76,BLUE),k(77,BLUE),k(78,GREEN),k(79,GREEN),         // c v   c c s s
                    k(80,GREEN),k(81,GREEN),k(82,GREEN),k(83,GREEN),k(84,YELLOW),   // s s s s a
                    k(85,YELLOW),k(86,BLUE),k(87,BLUE),k(0,OFF),k(0,OFF),           // a c c
                    k(88,WHITE),k(89,WHITE),k(90,WHITE),k(0,OFF),k(91,GREEN)}       // l d r   e
    };

    private static KeyDef k(int midiNote, Color color) {
        return new KeyDef(midiNote, color);
    }

    private static class KeyDef {
        final int midiNote;      // 0 = no key, 1-127 = actual MIDI note
        final Color baseColor;
        KeyDef(int note, Color col) { midiNote = note; baseColor = col; }
    }


    QwertyPage(int width, int height, LinnstrumentClipLauncherExtension parent, ClipLauncherPage clipLauncherPage) {
        super(width, height, parent);
    }

    @Override
    protected void showImpl() {
        getParent().getHost().showPopupNotification("LinnStrument QWERTY Keyboard");
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                setLED(x, y, Color.OFF);
            }
        }
        setLED(0, 5, Color.RED);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                KeyDef kd = KEYBOARD[y][x];
                if (kd.midiNote != 0) {
                    setLED(x, y, kd.baseColor);
                }
            }
        }
    }

    @Override
    protected void hideImpl() {
        // Nothing needed
    }

    @Override
    public void buttonDown(int x, int y, int velocity) {
        KeyDef kd = KEYBOARD[y][x];
        if (kd.midiNote != 0) {
            getParent().getQwertyOut().sendMidi(0x90, kd.midiNote, velocity);
            if (kd.baseColor == Color.RED || kd.baseColor == Color.MAGENTA) {
                setLED(x, y, WHITE);
            } else {
                setLED(x, y, Color.RED);
            }
        }
    }

    @Override
    public void buttonUp(int x, int y) {
        KeyDef kd = KEYBOARD[y][x];
        if (kd.midiNote != 0) {
            getParent().getQwertyOut().sendMidi(0x80, kd.midiNote, 0);
            setLED(x, y, kd.baseColor);
        }
    }
}