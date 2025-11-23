package com.theslowgrowth;

import com.bitwig.extension.controller.api.Transport;
import java.util.ArrayList;
import java.util.List;

public class TaskTimer {
    private final LinnstrumentClipLauncherExtension parent_;
    private final Transport transport_;
    private final List<TimedTask> tasks_ = new ArrayList<TimedTask>();
    private boolean active_ = true;

    private double internalPos = 0.0;
    private static final double INTERNAL_STEP = 0.015625; // 1/64 note

    public TaskTimer(LinnstrumentClipLauncherExtension parent, Transport transport) {
        parent_ = parent;
        transport_ = transport;
    }

    public void setActive(boolean active) {
        active_ = active;
        if (active_) startTimer();
    }

    public void addTask(TimedTask task) {
        if (!tasks_.contains(task)) tasks_.add(task);
        if (active_) startTimer();
    }

    public void removeTask(TimedTask task) {
        tasks_.remove(task);
    }

    public void removeAllTasks() { tasks_.clear(); }

    private void startTimer() { scheduleNextTick(); }

    private void scheduleNextTick() {
        double pos = transport_.isPlaying().get() ? transport_.getPosition().get() : internalPos;

        long delayMs;

        // Special case: FIXED_100MS → always 100ms, ignore transport
        boolean hasFixedTask = tasks_.stream().anyMatch(t -> t.speed == BlinkSpeed.FIXED_100MS);
        if (hasFixedTask) {
            delayMs = 100;
        } else {
            // 1/64 grid → rock-solid sync
            double phase = pos * 64.0;
            double fraction = phase - Math.floor(phase);
            double beatsUntil = INTERNAL_STEP - (fraction * INTERNAL_STEP);
            if (beatsUntil <= 0.001) beatsUntil += INTERNAL_STEP;

            double bpm = Math.max(20.0, transport_.tempo().value().getRaw() / 1000.0);
            delayMs = Math.max(1L, (long)(beatsUntil * 60000.0 / bpm));
        }

        parent_.getHost().scheduleTask(new Runnable() {
            @Override public void run() { timerCallback(); }
        }, delayMs);
    }

    private void timerCallback() {
        double pos = transport_.isPlaying().get()
                ? transport_.getPosition().get()
                : advanceInternalAndGet();

        for (TimedTask task : new ArrayList<TimedTask>(tasks_)) {
            if (task.speed == BlinkSpeed.FIXED_100MS || shouldTriggerNow(pos, task.speed, task.lastTriggerPos)) {
                task.runnable.run();
                if (task.speed != BlinkSpeed.FIXED_100MS) {
                    task.lastTriggerPos = pos;
                }
            }
        }

        if (active_ && !tasks_.isEmpty()) {
            scheduleNextTick();
        }
    }

    private boolean shouldTriggerNow(double pos, BlinkSpeed speed, double lastPos) {
        if (lastPos < 0) return true;

        double grid;
        switch (speed) {
            case FULL:    grid = 4.0;    break;
            case HALF:    grid = 2.0;    break;
            case QUARTER: grid = 1.0;    break;
            case _8TH:    grid = 0.5;    break;
            case _16TH:   grid = 0.25;   break;
            case _32ND:   grid = 0.125;  break;
            case FIXED_100MS: grid = 1;  break;
            default:      grid = 1.0;    break;
        }

        return Math.floor(pos / grid) > Math.floor(lastPos / grid);
    }

    private double advanceInternalAndGet() {
        internalPos += INTERNAL_STEP;
        internalPos -= Math.floor(internalPos);
        return internalPos;
    }

    public static class TimedTask {
        public final Runnable runnable;
        public final BlinkSpeed speed;
        public double lastTriggerPos = -1;

        public TimedTask(Runnable runnable, BlinkSpeed speed) {
            this.runnable = runnable;
            this.speed = speed;
        }
    }

    public enum BlinkSpeed {
        _32ND, _16TH, _8TH, QUARTER, HALF, FULL, FIXED_100MS
    }
}