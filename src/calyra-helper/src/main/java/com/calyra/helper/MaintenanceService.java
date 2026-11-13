package com.calyra.helper;

import java.nio.file.*;
import java.time.Instant;

/**
 * Periodic maintenance: filter expiry, old version GC, log rotation.
 * Helper crash must not affect renderer — this service is best-effort.
 */
public class MaintenanceService {
    private final Path dataDir;
    public MaintenanceService(Path dataDir){ this.dataDir=dataDir; }

    public void runDaily() throws Exception {
        // Remove filter versions older than 30 days, keep lastKnownGood
        Files.list(dataDir).forEach(p -> {
            try {
                var age = Instant.now().getEpochSecond() - Files.getLastModifiedTime(p).toInstant().getEpochSecond();
                if (age > 30*86400 && p.getFileName().toString().startsWith("filters-v")) {
                    // Do not delete if it's lastKnownGood
                    var lkg = dataDir.resolve("filters.lastgood");
                    if (Files.exists(lkg)) {
                        String target = Files.readString(lkg).trim();
                        if (p.toString().equals(target)) return;
                    }
                    Files.deleteIfExists(p);
                }
            } catch (Exception ignored){}
        });
    }
}
