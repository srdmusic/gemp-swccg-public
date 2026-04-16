package com.gempukku.swccgo;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ServerCleaner {
    private static final Logger LOG = LogManager.getLogger(ServerCleaner.class);
    private final Set<AbstractServer> _servers = Collections.synchronizedSet(new HashSet<AbstractServer>());
    private CleaningThread _thr;
    private final ExecutorService _cleanupPool = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()));

    public synchronized void addServer(AbstractServer server) {
        LOG.debug("Adding server: " + server.getClass());
        _servers.add(server);
        if (_thr == null) {
            _thr = new CleaningThread();
            _thr.start();
        }
    }

    public synchronized void removeServer(AbstractServer server) {
        _servers.remove(server);
        if (_servers.size() == 0 && _thr != null) {
            _thr.pleaseStop();
            _thr = null;
        }
    }

    private class CleaningThread extends Thread {
        private boolean _stopped;

        public void run() {
            try {
                while (!_stopped) {
                    // Snapshot the server list, then clean up in parallel
                    List<AbstractServer> snapshot;
                    synchronized (ServerCleaner.this) {
                        snapshot = new ArrayList<>(_servers);
                    }

                    List<Future<?>> futures = new ArrayList<>();
                    for (AbstractServer server : snapshot) {
                        futures.add(_cleanupPool.submit(() -> {
                            try {
                                server.cleanup();
                            } catch (Exception exp) {
                                LOG.error("Error while cleaning up a server", exp);
                            }
                        }));
                    }

                    // Wait for all cleanup tasks to complete (with timeout)
                    for (Future<?> f : futures) {
                        try {
                            f.get(2, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            LOG.warn("Cleanup task timed out or failed", e);
                        }
                    }

                    Thread.sleep(1000);
                }
            } catch (InterruptedException exp) {
                // Thread interrupted - get lost
            }
        }

        public void pleaseStop() {
            _stopped = true;
        }
    }
}
