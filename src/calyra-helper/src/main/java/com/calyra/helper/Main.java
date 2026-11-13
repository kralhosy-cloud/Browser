package com.calyra.helper;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        Path dataDir = Path.of(System.getProperty("user.home"), ".config", "calyra", "filters");
        FilterSubscriptionManager mgr = new FilterSubscriptionManager(dataDir);
        Path ipc = Path.of(System.getenv().getOrDefault("XDG_RUNTIME_DIR","/tmp"), "calyra", "helper-" + getUid() + ".sock");
        try (IpcServer srv = new IpcServer(ipc)) {
            srv.start();
            System.out.println("Calyra Helper started, ipc="+ipc);
            // Keep running; crash of helper does not crash renderer — parent monitors exit code
            Thread.currentThread().join();
        }
    }
    private static String getUid(){
        try { return String.valueOf(new ProcessBuilder("id","-u").start().waitFor()); } catch (Exception e){ return "0"; }
    }
}
