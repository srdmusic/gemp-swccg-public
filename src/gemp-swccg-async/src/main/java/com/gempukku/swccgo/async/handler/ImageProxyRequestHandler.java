package com.gempukku.swccgo.async.handler;

import com.gempukku.swccgo.async.ResponseWriter;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Proxies card image requests to res.starwarsccg.org.
 *
 * The Unity (Epic Duel) client fetches card images via fetch() which automatically
 * includes an Origin header. The CDN at res.starwarsccg.org blocks cross-origin
 * fetch requests with a 503. Plain <img> tags (used by the standard GUI) don't send
 * Origin, so they work fine.
 *
 * This handler lets the Unity client request images through the local server:
 *   GET /gemp-swccg/imageproxy?url=https%3A%2F%2Fres.starwarsccg.org%2F...
 *
 * The server fetches from the CDN server-to-server (no Origin header) and returns
 * the image bytes with Access-Control-Allow-Origin: * so Unity can use them.
 *
 * Restored 2026-05-20 — was dropped when master was rebased onto upstream, leaving
 * the Unity client with no working card-image source.
 */
public class ImageProxyRequestHandler implements UriRequestHandler {

    private static final Logger _log = LogManager.getLogger(ImageProxyRequestHandler.class);
    private static final String ALLOWED_HOST = "res.starwarsccg.org";
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 15000;

    @Override
    public void handleRequest(String uri, HttpRequest request,
                              Map<Type, Object> context,
                              ResponseWriter responseWriter,
                              String remoteIp) throws Exception {

        // Parse ?url= query parameter from the full request URI
        String fullUri = request.uri();
        String urlParam = null;
        int urlIdx = fullUri.indexOf("?url=");
        if (urlIdx >= 0) {
            urlParam = URLDecoder.decode(
                    fullUri.substring(urlIdx + 5), StandardCharsets.UTF_8.name());
        }

        // Security: only allow proxying to the known card-image CDN
        if (urlParam == null || !isAllowed(urlParam)) {
            _log.warn("ImageProxy rejected URL: " + urlParam + " from " + remoteIp);
            responseWriter.writeError(400);
            return;
        }

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlParam).openConnection();
            conn.setRequestMethod("GET");
            // Mimic a normal browser image load so the CDN doesn't block us
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (compatible; GEMP-SWCCG-Proxy/1.0)");
            conn.setRequestProperty("Referer", "https://gemp.starwarsccg.org/");
            conn.setRequestProperty("Accept", "image/gif,image/png,image/jpeg,image/*,*/*");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setInstanceFollowRedirects(true);

            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                _log.warn("ImageProxy upstream returned " + status + " for " + urlParam);
                responseWriter.writeError(status);
                return;
            }

            String contentType = conn.getContentType();
            if (contentType == null) {
                // Guess from extension
                if (urlParam.endsWith(".png"))       contentType = "image/png";
                else if (urlParam.endsWith(".gif"))  contentType = "image/gif";
                else                                  contentType = "image/jpeg";
            }

            byte[] imageBytes;
            try (InputStream is = conn.getInputStream()) {
                imageBytes = is.readAllBytes();
            }

            Map<CharSequence, String> headers = new HashMap<>();
            headers.put("Content-Type", contentType);
            headers.put("Access-Control-Allow-Origin", "*");
            headers.put("Cache-Control", "public, max-age=86400");

            responseWriter.writeByteResponse(imageBytes, headers);

        } catch (Exception e) {
            _log.error("ImageProxy error fetching " + urlParam, e);
            responseWriter.writeError(502);
        }
    }

    private boolean isAllowed(String url) {
        if (!url.startsWith("https://")) return false;
        String withoutScheme = url.substring("https://".length());
        // Must start with the allowed host (prevents path-traversal tricks)
        return withoutScheme.startsWith(ALLOWED_HOST + "/");
    }
}
