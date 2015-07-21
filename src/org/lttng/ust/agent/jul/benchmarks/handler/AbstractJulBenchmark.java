package org.lttng.ust.agent.jul.benchmarks.handler;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractJulBenchmark {

    // ------------------------------------------------------------------------
    // Configurable test parameters
    // ------------------------------------------------------------------------

    /** Nb of runs per test, result will be averaged */
    private static final int NB_RUNS = 10;

    /** Trace/log events per run */
    private static final int NB_ITER = 100000;

    /** Which tests to run (for different number of threads) */
    private static final int[] NB_THREADS = {1, 1, 2, 3, 4, 5, 6, 7, 8};

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    protected Logger logger;
    protected Handler handler;

    // ------------------------------------------------------------------------
    // Maintenance methods
    // ------------------------------------------------------------------------

    @Before
    public void setup() {
        /* Set up the logger */
        logger = Logger.getLogger("Test logger");
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);

        /* Sub-classes' @Before will setup the Handler */
    }

    @After
    public void teardown() {
        if (handler != null) {
            logger.removeHandler(handler);
            handler.close();
        }
        handler = null;
        logger = null;
    }

    // ------------------------------------------------------------------------
    // Test methods
    // ------------------------------------------------------------------------

    @Test
    public void runBenchmark() {
        if (handler != null) {
            logger.addHandler(handler);
        }

        System.out.println();
        System.out.println("Running benchmark: " + this.getClass().getCanonicalName());
        for (int i : NB_THREADS) {
            runTest(logger, i);
        }
    }

    private static void runTest(Logger log, int nbThreads) {
        long start, end, average, total = 0;
        for (int i = 0; i < NB_RUNS; i++) {
            Runner runner = new Runner(nbThreads, NB_ITER, log);

            start = System.nanoTime();
            runner.run();
            end = System.nanoTime();

            total += (end - start);
        }
        average = total / NB_RUNS;
        System.out.println(nbThreads + " threads, average = " + average / NB_ITER + " ns/event");
    }

    // ------------------------------------------------------------------------
    // Helper classes
    // ------------------------------------------------------------------------

    private static class Runner implements Runnable {

        private final List<Worker> workers = new LinkedList<>();
        private final List<Thread> workerThreads = new LinkedList<>();

        public Runner(int nbThreads, int nbIter, Logger log) {

            for (int id = 0; id < nbThreads; id++) {
                Worker curWorker = new Worker(id, nbIter, log);
                workers.add(curWorker);
                workerThreads.add(new Thread(curWorker, "worker " + id));
            }
        }

        @Override
        public void run() {
            for (Thread curThread : workerThreads) {
                curThread.start();
            }

            for (Thread curThread : workerThreads) {
                try {
                    curThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private static class Worker implements Runnable {

            private final Logger log;
            private final int threadId;
            private final int nbIter;

            public Worker(int threadId, int nbIter, Logger log) {
                this.log = log;
                this.threadId = threadId;
                this.nbIter = nbIter;
            }

            @Override
            public void run() {
                for (int i = 0; i < nbIter; i++) {
                    log.info("Thread " + threadId + ", iteration " + i);
                }
            }

        }
    }
}