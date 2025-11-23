// FlappyPage.java
package com.theslowgrowth;

import com.bitwig.extension.controller.api.MidiOut;

import static com.theslowgrowth.Color.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FlappyPage extends LinnstrumentPage {

    private static final int WIDTH = 26;
    private static final int HEIGHT = 8;

    private enum State { READY, PLAYING, DYING }

    private static class Pipe {
        float x;
        int gapY;
        int gapH;
        boolean scored;
    }

    private boolean active = false;
    private final Runnable gameLoopRunnable = new Runnable() {
        @Override
        public void run() {
            if (!active) return;
            updateGame();
            drawAll();
            getParent().getHost().scheduleTask(this, 200);
        }
    };

    private State gameState = State.READY;
    private float birdY = 4.0f;
    private float birdVY = 0.0f;  // units per second
    private final List<Pipe> pipes = new ArrayList<>();
    private int score = 0;
    private float pipeSpeed = 4.5f;  // units per second
    private float groundOffset = 0.0f;
    private int dyingFrames = 0;
    private boolean deathNote = false;

    private static final float UPDATE_DT = 0.200f;          // 200 ms per frame → 5 FPS
    private static final int SUBSTEPS = 10;
    private static final float GRAVITY_SEC = 5.0f;         // grid units per second²
    private static final float FLAP_BASE = 2.5f;          // full-strength flap velocity
    private static final float BIRD_X = 6.0f;
    private static final float LEFT_EDGE = 1.0f;
    private static final float PIPE_SPAWN_X = 27.0f;
    private static final float PIPE_WIDTH = 4.0f;
    private static final float PIPE_SPACING = 14.0f;
    private static final Color[] groundPattern = {ORANGE, BLACK, BLACK, ORANGE};

    public FlappyPage(int width, int height, LinnstrumentClipLauncherExtension parent) {
        super(width, height, parent);
    }

    @Override
    protected void showImpl() {
        getParent().getHost().showPopupNotification("LinnStrument Flappy Bird!");
        active = true;
        resetGame();
        gameState = State.READY;
        getParent().getHost().scheduleTask(gameLoopRunnable, 0);
    }

    @Override
    protected void hideImpl() {
        active = false;
    }

    @Override
    public void buttonDown(int x, int y, int velocity) {

        if (x == 0) return;  // Ignore column 0 (UI column)

        getParent().getQwertyOut().sendMidi(0x91, 60, velocity);

        float v = velocity / 127.0f;

        if (gameState == State.PLAYING) {
            // Flap: velocity 0-127 → 50%-100% strength
            float strength = FLAP_BASE * (0.5f + 0.5f * v);
            birdVY = -strength;
        } else if (gameState == State.READY || gameState == State.DYING) {
            // Start / Restart
            resetGame();
            gameState = State.PLAYING;
            spawnPipe();
        }
    }

    @Override
    public void buttonUp(int x, int y) {
        // Nothing
        getParent().getQwertyOut().sendMidi(0x81, 60, 0);
    }

    private void resetGame() {
        birdY = 4.0f;
        birdVY = 0.0f;
        pipes.clear();
        score = 0;
        pipeSpeed = 4.5f;
        groundOffset = 0.0f;
        dyingFrames = 0;
        getParent().getQwertyOut().sendMidi(0x81, 72, 0);
    }

    private void spawnPipe() {
        Pipe p = new Pipe();
        p.x = PIPE_SPAWN_X;
        int gapH = Math.max(2, 4 - (score / 15));
        p.gapH = gapH;
        int maxTopH = HEIGHT - gapH - 1;
        p.gapY = 1 + (int) (Math.random() * maxTopH);
        p.scored = false;
        pipes.add(p);
    }

    private void updateGame() {
        if (gameState == State.DYING) {
            dyingFrames--;
            if (dyingFrames <= 0) {
                gameState = State.READY;
                if(deathNote) {
                    getParent().getQwertyOut().sendMidi(0x81, 72, 0);
                    deathNote = false;
                }
            }
            return;
        }

        if (gameState != State.PLAYING) return;

        // Bird physics with sub-steps (smooth & stable at low FPS)
        float sub_dt = UPDATE_DT / SUBSTEPS;
        float g_sub = GRAVITY_SEC * sub_dt;
        for (int s = 0; s < SUBSTEPS; s++) {
            birdVY += g_sub;
            birdY += birdVY * sub_dt;
        }

        // Only ground kills
        if (birdY < 0.3f) {
            birdY = 0.3f;
        }

        if (birdY < 0.3f || birdY > 6.7f) {
            gameState = State.DYING;
            deathNote = true;
            getParent().getQwertyOut().sendMidi(0x91, 72, 127);
            dyingFrames = 10;
            return;
        }

        // Move pipes
        float pipeMove = pipeSpeed * UPDATE_DT;
        for (Pipe p : pipes) {
            p.x -= pipeMove;
        }

        // Check scoring, collision, removal
        Iterator<Pipe> it = pipes.iterator();
        while (it.hasNext()) {
            Pipe p = it.next();

            if (p.x + PIPE_WIDTH < LEFT_EDGE) {
                it.remove();
                continue;
            }

            // Score once when pipe passes bird
            if (!p.scored && p.x + PIPE_WIDTH < BIRD_X) {
                p.scored = true;
                score++;
                pipeSpeed += 0.12f;
            }

            // Collision check when pipe overlaps bird X
            else if (p.x < BIRD_X && p.x + PIPE_WIDTH > BIRD_X) {
                if (birdY < p.gapY || birdY >= p.gapY + p.gapH) {
                    gameState = State.DYING;
                    dyingFrames = 10;
                    return;
                }
            }
        }

        // Spawn new pipe when needed
        if (pipes.isEmpty() || pipes.get(pipes.size() - 1).x < PIPE_SPAWN_X - PIPE_SPACING) {
            spawnPipe();
        }

        // Scroll ground pattern
        groundOffset -= pipeSpeed * 1.8f * UPDATE_DT;
        if (groundOffset <= -4.0f) {
            groundOffset += 4.0f;
        }
    }

    private void drawAll() {
        // Column 0 always off (UI column)
        for (int y = 0; y < HEIGHT; y++) {
            setLED(0, y, OFF);
        }

        if (gameState == State.DYING) {
            Color flashC = (dyingFrames % 2 == 0) ? RED : OFF;
            for (int x = 1; x < WIDTH; x++) {
                for (int y = 0; y < HEIGHT; y++) {
                    setLED(x, y, flashC);
                }
            }
            return;
        }

        // Sky + scrolling ground (columns 1-25)
        for (int x = 1; x < WIDTH; x++) {
            for (int y = 0; y < 7; y++) {
                setLED(x, y, OFF);
            }
            int patIdx = (int) ((x - groundOffset) % 4.0f);
            if (patIdx < 0) patIdx += 4;
            setLED(x, 7, groundPattern[patIdx]);
        }

        // Pipes
        for (Pipe p : pipes) {
            for (int dx = 0; dx < 4; dx++) {
                int cx = (int) (p.x + dx);
                if (cx < 1 || cx >= WIDTH) continue;
                for (int py = 0; py < p.gapY; py++) {
                    setLED(cx, py, GREEN);
                }
                for (int py = p.gapY + p.gapH; py < HEIGHT; py++) {
                    setLED(cx, py, GREEN);
                }
            }
        }

        // Bird
        int birdXi = (int) BIRD_X;
        int birdYi = Math.round(birdY);
        if (birdYi >= 0 && birdYi < HEIGHT) {
            setLED(birdXi, birdYi, YELLOW);
        }

        // Score bar in column 25 (grows upward from ground)
        int barHeight = Math.min(8, score);
        for (int s = 0; s < barHeight; s++) {
            setLED(25, 7 - s, LIME);
        }
        for (int s = barHeight; s < 8; s++) {
            setLED(25, 7 - s, OFF);
        }
    }


}