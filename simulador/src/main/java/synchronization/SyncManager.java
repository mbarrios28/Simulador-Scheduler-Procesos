package synchronization;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class SyncManager {
    private static final SyncManager INSTANCE = new SyncManager();
    
    private final Lock globalMutex;
    private final ConcurrentHashMap<String, ProcessLock> processLocks;
    private final ConcurrentHashMap<String, Condition> processConditions;

    private SyncManager() {
        this.globalMutex = new ReentrantLock();
        this.processLocks = new ConcurrentHashMap<>();
        this.processConditions = new ConcurrentHashMap<>();
        System.out.println("[SyncManager] Instancia creada");
    }

    public static SyncManager getInstance() {
        return INSTANCE;
    }

    public void acquireGlobalLock() {
        globalMutex.lock();
    }

    public void releaseGlobalLock() {
        globalMutex.unlock();
    }

    public void acquireProcessLock(String pid) {
        ProcessLock lock = processLocks.computeIfAbsent(pid, k -> new ProcessLock());
        lock.acquire();
    }

    public void releaseProcessLock(String pid) {
        ProcessLock lock = processLocks.get(pid);
        if (lock != null) {
            lock.release();
        }
    }
    public Condition getProcessCondition(String pid) {
        return processConditions.computeIfAbsent(pid, k -> globalMutex.newCondition());
    }

    public void awaitProcessCondition(String pid) throws InterruptedException {
        Condition condition = getProcessCondition(pid);
        condition.await();
    }
    public void signalProcess(String pid) {
        Condition condition = processConditions.get(pid);
        if (condition != null) {
            condition.signal();
        }
    }
    public void signalAllProcesses() {
        processConditions.values().forEach(Condition::signalAll);
    }
    public void cleanupProcess(String pid) {
        processLocks.remove(pid);
        processConditions.remove(pid);
        System.out.println("[SyncManager] Limpiados recursos para: " + pid);
    }

    private static class ProcessLock {
        private final Lock lock;
        private int holdCount;
        private Thread holdingThread;

        public ProcessLock() {
            this.lock = new ReentrantLock();
            this.holdCount = 0;
            this.holdingThread = null;
        }

        public void acquire() {
            lock.lock();
            holdCount++;
            holdingThread = Thread.currentThread();
        }

        public void release() {
            if (holdCount > 0 && Thread.currentThread() == holdingThread) {
                holdCount--;
                if (holdCount == 0) {
                    holdingThread = null;
                }
                lock.unlock();
            }
        }

        public boolean isHeldByCurrentThread() {
            return Thread.currentThread() == holdingThread;
        }
    }
}