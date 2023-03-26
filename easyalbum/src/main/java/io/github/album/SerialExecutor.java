package io.github.album;

import java.util.concurrent.Executor;
import java.util.ArrayDeque;

final class SerialExecutor implements Executor {
    final ArrayDeque<Runnable> mTasks = new ArrayDeque<>();
    Runnable mActive;

    public synchronized void execute(final Runnable r) {
        mTasks.offer(() -> {
            try {
                r.run();
            } finally {
                scheduleNext();
            }
        });
        if (mActive == null) {
            scheduleNext();
        }
    }

    private synchronized void scheduleNext() {
        if ((mActive = mTasks.poll()) != null) {
            AlbumConfig.getExecutor().execute(mActive);
        }
    }
}
