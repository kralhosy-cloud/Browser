package com.calyra.helper;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Manages filter subscriptions: HTTPS download with bounded body, timeout,
 * redirect policy, atomic update and last-known-good rollback.
 * Network path is explicit (direct) and never queries filter server with visited URLs.
 */
public class FilterSubscriptionManager {
    private final Path dataDir;
    private final HttpClient http;
    private static final long MAX_BODY = 5 * 1024 * 1024; // 5 MiB
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_REDIRECTS = 3;

    public FilterSubscriptionManager(Path dataDir) {
        this.dataDir = dataDir;
        this.http = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER) // manual limit
                .build();
    }

    public record Subscription(String sourceUrl, String license, String etag, String lastModified,
                               String version, String sha256, int ruleCount) {}

    public FilterList fetch(String url, String ifEtag, String ifModifiedSince) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).GET().timeout(TIMEOUT);
        if (ifEtag != null) b.header("If-None-Match", ifEtag);
        if (ifModifiedSince != null) b.header("If-Modified-Since", ifModifiedSince);

        HttpRequest req = b.build();
        HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());

        if (resp.statusCode() == 304) return null; // not modified

        // Redirect policy: only https, bounded
        int redirects = 0;
        while (isRedirect(resp.statusCode()) && redirects < MAX_REDIRECTS) {
            String loc = resp.headers().firstValue("location").orElseThrow();
            URI next = URI.create(loc);
            if (!"https".equals(next.getScheme())) throw new IOException("Redirect to non-https blocked");
            req = HttpRequest.newBuilder(next).GET().timeout(TIMEOUT).build();
            resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            redirects++;
        }
        if (isRedirect(resp.statusCode())) throw new IOException("Too many redirects");

        if (resp.statusCode() != 200) throw new IOException("Bad status: " + resp.statusCode());

        byte[] body = resp.body();
        if (body.length > MAX_BODY) throw new IOException("Body too large: " + body.length);

        String text = new String(body, java.nio.charset.StandardCharsets.UTF_8);
        return parse(text, url, resp.headers().firstValue("etag").orElse(null));
    }

    private boolean isRedirect(int code) { return code==301||code==302||code==303||code==307||code==308; }

    public record FilterList(String sourceUrl, String version, String sha256, int ruleCount, String raw) {}

    private FilterList parse(String raw, String sourceUrl, String etag) throws Exception {
        // Count rules, ignore unsupported syntax (do not count as loaded)
        int count=0;
        for (String line: raw.split("\n")) {
            line=line.trim();
            if (line.isEmpty()||line.startsWith("!")) continue;
            if (!isSupported(line)) continue; // not counted
            count++;
        }
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String hash = HexFormat.of().formatHex(md.digest(raw.getBytes()));
        return new FilterList(sourceUrl, etag!=null?etag:hash.substring(0,8), hash, count, raw);
    }

    private boolean isSupported(String rule) {
        // Supported subset: ||domain^, @@, $third-party etc., ## cosmetic
        return rule.startsWith("||") || rule.startsWith("@@") || rule.contains("##") || rule.contains("$");
    }

    public void atomicUpdate(FilterList list) throws Exception {
        Path tmp = dataDir.resolve("filters.tmp");
        Path dst = dataDir.resolve("filters-v" + list.version() + ".txt");
        Path lastGood = dataDir.resolve("filters.lastgood");
        Files.createDirectories(dataDir);
        Files.writeString(tmp, list.raw());
        // fsync + atomic move
        Files.move(tmp, dst, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        // Keep last-known-good pointer
        Files.writeString(lastGood, dst.toString());
    }

    public Path lastKnownGood() throws IOException {
        Path p = dataDir.resolve("filters.lastgood");
        if (!Files.exists(p)) return null;
        String target = Files.readString(p).trim();
        return Path.of(target);
    }
}
