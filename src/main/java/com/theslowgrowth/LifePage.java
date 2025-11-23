// LifePage.java
package com.theslowgrowth;

import static com.theslowgrowth.Color.*;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Transport;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class LifePage extends LinnstrumentPage {

    // 0,0 - Start / Stop
    // 0,1 - Short Press Clear Cells, Long Press Clear Beacons
    // 0,2 - Tempo Sync Mode, toggles between DAW SYNC variations or 100ms
    // 0,3 - Display Mode, toggles between B/W, Warm, Cool, Magenta
    // 0,4 - Change Extension App (Clip launcher etc)
    // 0,5 - Exit back to LinnStrument
    // 0,6 - Auto-Seed, toggles between ON/OFF and sets the number of ticks between reseeding

    private static final int WIDTH  = 26;
    private static final int HEIGHT = 8;
    private final boolean[][] alive = new boolean[WIDTH][HEIGHT];
    private final int[][] ageSinceDeath = new int[WIDTH][HEIGHT];
    private final int[][] beaconNote = new int[WIDTH][HEIGHT]; // -1 = no beacon
    private boolean running = true;
    private int autoSeedAfterTicks = 0; // 0 = off, otherwise number of ticks until reseed
    private int ticksUntilReseed = 0;
    private static final int[] AUTO_SEED_OPTIONS = {4, 8, 16, 32, 64, 128, 256, 1024};
    private int displayMode = 1; // 0=BW, 1=Warm, 2=Cool, 3=Magenta
    private static final int TEMPO_OFF   = 0;
    private static final int TEMPO_1_32  = 1;
    private static final int TEMPO_1_16  = 2;
    private static final int TEMPO_1_8   = 3;
    private static final int TEMPO_1_4   = 4;
    private static final int TEMPO_1_2   = 5;
    private int tempoSyncMode = TEMPO_OFF;
    private final TaskTimer taskTimer_;
    private TaskTimer.TimedTask lifeTask_;
    private final Transport transport;
    private final MidiOut midiOut;
    private final Random random = new Random();
    private int nextMidiNote = 60;
    private int lastNonZeroAutoSeed = 16;
    private final Map<Integer, Long> pressStartTime = new HashMap<>(); // key = x*100 + y
    private final Map<Integer, Runnable> pendingLongPressTask = new HashMap<>();

    LifePage(int width, int height, LinnstrumentClipLauncherExtension parent) {
        super(width, height, parent);
        clearCellsOnly();
        this.transport = parent.getTransport();
        this.midiOut = parent.getQwertyOut();
        taskTimer_ = new TaskTimer(parent, transport);
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                beaconNote[x][y] = -1;
            }
        }
        running = true;
        startSimulation();
    }

    @Override
    protected void showImpl() {
        getParent().getHost().showPopupNotification("LinnStrument Life + MIDI Beacons");
        redrawEverything();
        updateUIButtons();
        taskTimer_.setActive(true);
        startSimulation();
    }

    @Override
    protected void hideImpl() {
        running = false;
        if (lifeTask_ != null) {
            taskTimer_.removeTask(lifeTask_);
            lifeTask_ = null;
        }
        for (Runnable task : pendingLongPressTask.values()) {
            getParent().getHost().scheduleTask(task, -1);
        }
        pendingLongPressTask.clear();
        pressStartTime.clear();
        taskTimer_.setActive(false);
    }

    public void buttonDown(int x, int y, int velocity) {
        if (velocity == 0) return;
        if (x == 0) {
            handleControlColumn(y);
            return;
        }
        if (running) {
            alive[x][y] = !alive[x][y];
            ageSinceDeath[x][y] = alive[x][y] ? 0 : 999;
            setLED(x, y, colorForCell(x, y));
            return;
        }
        int key = x * 100 + y;
        long now = System.currentTimeMillis();
        pressStartTime.put(key, now);
        Runnable longPressRunnable = new Runnable() {
            @Override
            public void run() {
                if (!pressStartTime.containsKey(key)) return;
                long duration = System.currentTimeMillis() - pressStartTime.get(key);
                if (duration >= 1000 && !running) {
                    handleLongPress(x, y);
                }
                pendingLongPressTask.remove(key);
            }
        };
        pendingLongPressTask.put(key, longPressRunnable);
        getParent().getHost().scheduleTask(longPressRunnable, 1000);
    }

    @Override
    public void buttonUp(int x, int y) {
        if (x == 0 && y == 6) {
            cancelPendingLongPress(x * 100 + y);
            Long start = pressStartTime.get(x * 100 + y);
            if (start == null) return;
            long duration = System.currentTimeMillis() - start;
            if (duration < 1000) {
                if (autoSeedAfterTicks == 0) {
                    autoSeedAfterTicks = lastNonZeroAutoSeed;
                    getParent().getHost().showPopupNotification("Auto-Seed: ON (" + autoSeedAfterTicks + " ticks)");
                } else {
                    int currentIndex = 0;
                    for (int i = 0; i < AUTO_SEED_OPTIONS.length; i++) {
                        if (AUTO_SEED_OPTIONS[i] == autoSeedAfterTicks) {
                            currentIndex = i;
                            break;
                        }
                    }
                    int nextIndex = (currentIndex + 1) % AUTO_SEED_OPTIONS.length;
                    int nextValue = AUTO_SEED_OPTIONS[nextIndex];
                    autoSeedAfterTicks = nextValue;
                    lastNonZeroAutoSeed = nextValue;
                    getParent().getHost().showPopupNotification("Auto-Seed: " + nextValue + " ticks");
                }
                if (running) {
                    ticksUntilReseed = autoSeedAfterTicks;
                }
                updateUIButtons();
            }
            pressStartTime.remove(x * 100 + y);
            return;
        } else if (x == 0) {
            cancelPendingLongPress(x * 100 + y);
            pressStartTime.remove(x * 100 + y);
        }
        if (x == 0 || running) return;
        cancelPendingLongPress(x * 100 + y);
        int key = x * 100 + y;
        Long start = pressStartTime.get(key);
        if (start == null) return;
        long duration = System.currentTimeMillis() - start;
        if (duration < 1000) {
            cancelPendingLongPress(key);
            if (beaconNote[x][y] == -1) {
                alive[x][y] = !alive[x][y];
                ageSinceDeath[x][y] = alive[x][y] ? 0 : 999;
            } else {
                alive[x][y] = !alive[x][y];
                ageSinceDeath[x][y] = alive[x][y] ? 0 : 999;
            }
            setLED(x, y, colorForCell(x, y));
        }
        pressStartTime.remove(key);
    }

    private void handleLongPress(int x, int y) {
        if (running) return;

        if (beaconNote[x][y] != -1) {
            beaconNote[x][y] = -1;
            alive[x][y] = false;
            ageSinceDeath[x][y] = 999;
            getParent().getHost().showPopupNotification("Beacon Removed (Note " + (nextMidiNote - 1) + ")");
        } else {
            beaconNote[x][y] = nextMidiNote++;
            alive[x][y] = false;
            ageSinceDeath[x][y] = 999;
            getParent().getHost().showPopupNotification("Beacon Created! MIDI Note " + (nextMidiNote - 1));
        }
        setLED(x, y, colorForCell(x, y));
    }

    private void cancelPendingLongPress(int key) {
        Runnable task = pendingLongPressTask.get(key);
        if (task != null) {
            getParent().getHost().scheduleTask(task, -1);
            pendingLongPressTask.remove(key);
        }
    }

    private void handleControlColumn(int y) {
        if (y == 0) {
            running = !running;
            getParent().getHost().showPopupNotification("Life: " + (running ? "Running!" : "Paused"));
            updateUIButtons();
            if (running) {
                startSimulation();
                redrawEverything();
            } else {
                if (lifeTask_ != null) {
                    taskTimer_.removeTask(lifeTask_);
                    lifeTask_ = null;
                }
                redrawEverything();
            }
        } else if (y == 1) {
            clearCellsOnly();
            updateUIButtons();
            int key = 0 * 100 + y;
            long now = System.currentTimeMillis();
            pressStartTime.put(key, now);
            Runnable longPressRunnable = () -> {
                if (!pressStartTime.containsKey(key)) return;
                long duration = System.currentTimeMillis() - pressStartTime.get(key);
                if (duration >= 1000) {
                    clearBeacons();
                    updateUIButtons();
                }
                pendingLongPressTask.remove(key);
            };
            pendingLongPressTask.put(key, longPressRunnable);
            getParent().getHost().scheduleTask(longPressRunnable, 1000);
        } else if (y == 2) {
            tempoSyncMode = (tempoSyncMode + 1) % 6;
            String[] labels = {"Off (Fixed 100ms)", "1/32", "1/16", "1/8", "1/4", "1/2"};
            getParent().getHost().showPopupNotification("Life Tempo: " + labels[tempoSyncMode]);
            if (running) startSimulation();
            updateUIButtons();
        } else if (y == 3) {
            displayMode = (displayMode + 1) % 4;
            redrawEverything();
            updateUIButtons();
        } else if (y == 6) {
            int key = 0 * 100 + 6;
            long now = System.currentTimeMillis();
            pressStartTime.put(key, now);
            Runnable longPressRunnable = () -> {
                if (!pressStartTime.containsKey(key)) return;
                long duration = System.currentTimeMillis() - pressStartTime.get(key);
                if (duration >= 1000) {
                    if (autoSeedAfterTicks > 0) {
                        lastNonZeroAutoSeed = autoSeedAfterTicks;
                        autoSeedAfterTicks = 0;
                        ticksUntilReseed = 0;
                        getParent().getHost().showPopupNotification("Auto-Seed: OFF (long press)");
                    } else {
                        autoSeedAfterTicks = lastNonZeroAutoSeed;
                        if (running) ticksUntilReseed = autoSeedAfterTicks;
                        getParent().getHost().showPopupNotification("Auto-Seed: ON (" + autoSeedAfterTicks + " ticks)");
                    }
                    updateUIButtons();
                    pressStartTime.remove(key);
                }
                pendingLongPressTask.remove(key);
            };

            pendingLongPressTask.put(key, longPressRunnable);
            getParent().getHost().scheduleTask(longPressRunnable, 1000);
        }
    }

    private void updateUIButtons() {
        setLED(0, 0, running ? GREEN : OFF);
        setLED(0, 1, RED);
        setLED(0, 2, tempoSyncMode == TEMPO_OFF ? OFF : LIME);
        setLED(0, 3, displayMode == 0 ? OFF : (displayMode == 1 ? RED : displayMode == 2 ? BLUE : MAGENTA));
        setLED(0, 6, autoSeedAfterTicks > 0 ? GREEN : OFF);
    }

    private void clearCellsOnly() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                alive[x][y] = false;
                ageSinceDeath[x][y] = 999;
            }
        }
        nextMidiNote = 60;
        redrawEverything();
        getParent().getHost().showPopupNotification("Life: All Cells Killed (Beacons preserved)");
    }

    private void clearBeacons() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (beaconNote[x][y] != -1) {
                    sendNoteOff(x, y);
                }
                beaconNote[x][y] = -1;
                alive[x][y] = false;
                ageSinceDeath[x][y] = 999;
            }
        }
        nextMidiNote = 60;
        redrawEverything();
        getParent().getHost().showPopupNotification("All Beacons Removed");
    }

    private void startSimulation() {
        if (lifeTask_ != null) {
            taskTimer_.removeTask(lifeTask_);
            lifeTask_ = null;
        }
        if (autoSeedAfterTicks > 0) {
            ticksUntilReseed = autoSeedAfterTicks;
        }
        if (tempoSyncMode == TEMPO_OFF) {
            lifeTask_ = null;
            getParent().getHost().scheduleTask(this::tick100ms, 100);
        } else {
            TaskTimer.BlinkSpeed speed = TaskTimer.BlinkSpeed._32ND;
            switch (tempoSyncMode) {
                case TEMPO_1_32: speed = TaskTimer.BlinkSpeed._32ND; break;
                case TEMPO_1_16: speed = TaskTimer.BlinkSpeed._16TH; break;
                case TEMPO_1_8:  speed = TaskTimer.BlinkSpeed._8TH;  break;
                case TEMPO_1_4:  speed = TaskTimer.BlinkSpeed.QUARTER; break;
                case TEMPO_1_2:  speed = TaskTimer.BlinkSpeed.HALF;   break;
            }
            lifeTask_ = new TaskTimer.TimedTask(this::tick, speed);
            taskTimer_.addTask(lifeTask_);
        }
    }

    private void tick100ms() {
        if (!running || tempoSyncMode != TEMPO_OFF) return;
        tick();
        getParent().getHost().scheduleTask(this::tick100ms, 100);
    }

    private void seedRandom() {
        int count = 30 + random.nextInt(40);
        for (int i = 0; i < count; i++) {
            int x = 1 + random.nextInt(WIDTH - 2);
            int y = random.nextInt(HEIGHT);
            alive[x][y] = true;
            ageSinceDeath[x][y] = 0;
        }
    }

    private void tick() {
        if (!running) return;
        if (autoSeedAfterTicks > 0) {
            ticksUntilReseed--;
            if (ticksUntilReseed <= 0) {
                seedRandom();
                ticksUntilReseed = autoSeedAfterTicks;
            }
        }
        boolean[][] nextGen = new boolean[WIDTH][HEIGHT];
        int[][] nextAge = new int[WIDTH][HEIGHT];
        for (int x = 1; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                int neighbors = countNeighbors(x, y);
                boolean currentlyAlive = alive[x][y];
                boolean willBeAlive;
                if (currentlyAlive) {
                    willBeAlive = (neighbors == 2 || neighbors == 3);
                } else {
                    willBeAlive = (neighbors == 3);
                }
                if (beaconNote[x][y] != -1) {
                    if (!currentlyAlive && willBeAlive) {
                        sendNoteOn(x, y, neighbors);
                    } else if (currentlyAlive && !willBeAlive) {
                        sendNoteOff(x, y);
                    }
                }
                nextGen[x][y] = willBeAlive;
                if (willBeAlive) {
                    nextAge[x][y] = 0;
                } else {
                    int old = ageSinceDeath[x][y];
                    nextAge[x][y] = (old >= 999) ? 999 : old + 1;
                }
            }
        }
        for (int x = 1; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                alive[x][y] = nextGen[x][y];
                ageSinceDeath[x][y] = nextAge[x][y];
                setLED(x, y, colorForCell(x, y));
            }
        }
    }

    private void sendNoteOn(int x, int y, int neighbors) {
        int note = beaconNote[x][y];
        if (note == -1) return;
        int velocity = 20 + (int)(neighbors * 13.375);
        velocity = Math.min(127, Math.max(20, velocity));
        midiOut.sendMidi(0x91, note, velocity);
    }

    private void sendNoteOff(int x, int y) {
        int note = beaconNote[x][y];
        if (note == -1) return;
        midiOut.sendMidi(0x81, note, 0);
    }

    private int countNeighbors(int cx, int cy) {
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = cx + dx;
                if (nx < 1) nx = WIDTH - 1;
                else if (nx >= WIDTH) nx = 1;
                int ny = (cy + dy + HEIGHT) % HEIGHT;
                if (alive[nx][ny]) count++;
            }
        }
        return count;
    }

    private Color colorForCell(int x, int y) {
        boolean isBeacon = beaconNote[x][y] != -1;
        if (isBeacon && !running) {
            return alive[x][y] ? MAGENTA : PINK;
        }
        if (alive[x][y]) {
            return isBeacon ? MAGENTA : WHITE;
        }
        if (displayMode == 0) return OFF;
        int age = ageSinceDeath[x][y];
        switch (displayMode) {
            case 1: // Warm
                if (age <= 2) return YELLOW;
                if (age <= 5) return ORANGE;
                if (age <= 8) return RED;
                break;
            case 2: // Cool
                if (age <= 2) return LIME;
                if (age <= 5) return CYAN;
                if (age <= 8) return BLUE;
                break;
            case 3: // Magenta
                if (age <= 2) return LIME;
                if (age <= 5) return PINK;
                if (age <= 8) return MAGENTA;
                break;
        }
        return OFF;
    }

    private void redrawEverything() {
        for (int x = 1; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                setLED(x, y, colorForCell(x, y));
            }
        }
    }
}