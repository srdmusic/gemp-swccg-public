package com.gempukku.swccgo.async.auth;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gempukku.swccgo.common.ApplicationConfiguration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class JwtService {
    private static final JwtService INSTANCE = new JwtService();

    private final String _issuer;
    private final byte[] _secret;
    private final long _ttlSeconds;

    private JwtService() {
        _issuer = ApplicationConfiguration.getProperty("jwt.issuer");
        String secretValue = ApplicationConfiguration.getProperty("jwt.secret");
        if (secretValue == null || secretValue.isEmpty())
            secretValue = "change-me";
        _secret = secretValue.getBytes(StandardCharsets.UTF_8);
        _ttlSeconds = parseLong(ApplicationConfiguration.getProperty("jwt.ttl.seconds"), 86400L);
    }

    public static JwtService getInstance() {
        return INSTANCE;
    }

    public long getTtlSeconds() {
        return _ttlSeconds;
    }

    public String issueToken(String subject) {
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + _ttlSeconds;

        Map<String, Object> header = new LinkedHashMap<String, Object>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("sub", subject);
        if (_issuer != null && !_issuer.isEmpty())
            payload.put("iss", _issuer);
        payload.put("iat", issuedAt);
        payload.put("exp", expiresAt);

        String encodedHeader = base64UrlEncode(JSON.toJSONString(header));
        String encodedPayload = base64UrlEncode(JSON.toJSONString(payload));
        String signature = sign(encodedHeader + "." + encodedPayload);

        return encodedHeader + "." + encodedPayload + "." + signature;
    }

    public JwtToken verifyToken(String token) {
        if (token == null || token.isEmpty())
            return null;

        String[] parts = token.split("\\.");
        if (parts.length != 3)
            return null;

        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);
        if (!constantTimeEquals(expectedSignature, parts[2]))
            return null;

        JSONObject header = parseJson(base64UrlDecodeToString(parts[0]));
        if (header == null || !"HS256".equals(header.getString("alg")))
            return null;

        JSONObject payload = parseJson(base64UrlDecodeToString(parts[1]));
        if (payload == null)
            return null;

        String issuer = payload.getString("iss");
        if (_issuer != null && !_issuer.isEmpty()) {
            if (issuer == null || !_issuer.equals(issuer))
                return null;
        }

        Long exp = payload.getLong("exp");
        String subject = payload.getString("sub");
        if (exp == null || subject == null || subject.isEmpty())
            return null;

        long now = Instant.now().getEpochSecond();
        if (exp < now)
            return null;

        return new JwtToken(subject, exp);
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(_secret, "HmacSHA256"));
            byte[] signature = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception e) {
            return "";
        }
    }

    private String base64UrlEncode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String base64UrlDecodeToString(String value) {
        try {
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private JSONObject parseJson(String json) {
        try {
            return JSON.parseObject(json);
        } catch (Exception e) {
            return null;
        }
    }

    private long parseLong(String value, long defaultValue) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null)
            return false;
        if (left.length() != right.length())
            return false;
        int result = 0;
        for (int i = 0; i < left.length(); i++) {
            result |= left.charAt(i) ^ right.charAt(i);
        }
        return result == 0;
    }

    public static final class JwtToken {
        private final String _subject;
        private final long _expiresAt;

        public JwtToken(String subject, long expiresAt) {
            _subject = subject;
            _expiresAt = expiresAt;
        }

        public String getSubject() {
            return _subject;
        }

        public long getExpiresAt() {
            return _expiresAt;
        }
    }
}
