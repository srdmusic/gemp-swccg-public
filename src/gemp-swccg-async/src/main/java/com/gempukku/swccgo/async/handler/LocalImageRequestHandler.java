package com.gempukku.swccgo.async.handler;

import com.gempukku.swccgo.async.ResponseWriter;
import io.netty.handler.codec.http.HttpRequest;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaders.Names.IF_NONE_MATCH;

/** Serves the installed image mirror from disk without using the global web-file cache. */
public class LocalImageRequestHandler implements UriRequestHandler {
    private final File _root;
    private final String _rootPath;

    public LocalImageRequestHandler(String root) {
        try {
            _root = new File(root).getCanonicalFile();
            _rootPath = _root.getPath() + File.separator;
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid offline image path: " + root, e);
        }
    }

    @Override
    public void handleRequest(String uri, HttpRequest request,
                              Map<Type, Object> context,
                              ResponseWriter responseWriter,
                              String remoteIp) throws Exception {
        final File image;
        try {
            image = resolveImage(uri);
        } catch (SecurityException | IllegalArgumentException e) {
            responseWriter.writeError(403);
            return;
        }

        if (!image.isFile()) {
            responseWriter.writeError(404);
            return;
        }

        String etag = "\"" + image.lastModified() + "-" + image.length() + "\"";
        if (clientHasCurrentVersion(request, etag)) {
            responseWriter.writeError(304);
            return;
        }

        byte[] bytes = Files.readAllBytes(image.toPath());
        Map<CharSequence, String> headers = new HashMap<>();
        headers.put("Content-Type", contentType(bytes, image.getName()));
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Cache-Control", "no-cache");
        headers.put("ETag", etag);
        headers.put("X-Content-Type-Options", "nosniff");
        if (image.getName().toLowerCase(Locale.ROOT).endsWith(".svg")) {
            headers.put("Content-Security-Policy", "sandbox; default-src 'none'");
        }
        responseWriter.writeByteResponse(bytes, headers);
    }

    private boolean clientHasCurrentVersion(HttpRequest request, String etag) {
        String ifNoneMatch = request.headers().get(IF_NONE_MATCH);
        if (ifNoneMatch == null) {
            return false;
        }
        for (String clientKnownVersion : ifNoneMatch.split(",")) {
            if (clientKnownVersion.trim().equals(etag)) {
                return true;
            }
        }
        return false;
    }

    File resolveImage(String uri) throws IOException {
        int queryStart = uri.indexOf('?');
        String path = queryStart < 0 ? uri : uri.substring(0, queryStart);
        path = URLDecoder.decode(path.replace("+", "%2B"), StandardCharsets.UTF_8);
        if (path.isEmpty() || path.startsWith("/") || path.startsWith(".")
                || path.contains("..") || path.contains("\\")) {
            throw new SecurityException("Unsafe image path");
        }

        File image = new File(_root, path).getCanonicalFile();
        if (!image.getPath().startsWith(_rootPath) || !isImageFilename(image.getName())) {
            throw new SecurityException("Image path escaped the configured root");
        }
        return image;
    }

    private boolean isImageFilename(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        return lower.endsWith(".gif") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".svg") || lower.endsWith(".webp");
    }

    private String contentType(byte[] bytes, String filename) {
        if (startsWith(bytes, new byte[] {'G', 'I', 'F', '8'})) return "image/gif";
        if (startsWith(bytes, new byte[] {(byte) 0x89, 'P', 'N', 'G'})) return "image/png";
        if (startsWith(bytes, new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff})) return "image/jpeg";
        if (startsWith(bytes, new byte[] {'B', 'M'})) return "image/bmp";
        if (startsWith(bytes, new byte[] {'R', 'I', 'F', 'F'}) && bytes.length >= 12
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) return false;
        }
        return true;
    }
}
