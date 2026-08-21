package co.eci.snake.core.engine;

import co.eci.snake.core.GameState;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class GameClock implements AutoCloseable {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final long periodMillis;
    private final Runnable tick;
    private final AtomicReference<GameState> state = new AtomicReference<>(GameState.STOPPED);
    private final Object pauseLock = new Object();

    private volatile int totalWorkers = 0;
    private volatile CountDownLatch pauseLatch = new CountDownLatch(0);

    public GameClock(long periodMillis, Runnable tick) {
        if (periodMillis <= 0) throw new IllegalArgumentException("periodMillis must be > 0");
        this.periodMillis = periodMillis;
        this.tick = Objects.requireNonNull(tick, "tick");
    }

    public void setWorkerCount(int n) {
        this.totalWorkers = n;
    }

    public void start() {
        if (state.compareAndSet(GameState.STOPPED, GameState.RUNNING)) {
            scheduler.scheduleAtFixedRate(() -> {
                if (state.get() == GameState.RUNNING) tick.run();
            }, 0, periodMillis, TimeUnit.MILLISECONDS);
        }
    }

    public void pause() {
        synchronized (pauseLock) {
            pauseLatch = new CountDownLatch(totalWorkers); // nuevo "cerrojo" para este ciclo de pausa
            state.set(GameState.PAUSED);
        }
    }

    public void resume() {
        synchronized (pauseLock) {
            state.set(GameState.RUNNING);
            pauseLock.notifyAll();
        }
    }

    public void stop() {
        synchronized (pauseLock) {
            state.set(GameState.STOPPED);
            pauseLock.notifyAll();
        }
    }

    public void awaitRunning() throws InterruptedException {
        synchronized (pauseLock) {
            boolean announced = false;
            while (state.get() == GameState.PAUSED) {
                if (!announced) {
                    pauseLatch.countDown(); // confirma: "yo ya llegué al punto de pausa"
                    announced = true;
                }
                pauseLock.wait();
            }
        }
    }

    public void awaitAllPaused() throws InterruptedException {
        pauseLatch.await();
    }

    public GameState state() {
        return state.get();
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}