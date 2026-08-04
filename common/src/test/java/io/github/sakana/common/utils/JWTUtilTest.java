package io.github.sakana.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWTUtil 单元测试。
 *
 * <p>注意：测试密钥必须 >= 32 字节，否则 {@code Keys.hmacShaKeyFor} 会抛 {@link WeakKeyException}。</p>
 */
class JWTUtilTest {

    /** 测试密钥（51 字节，满足 HMAC-SHA 最低 256-bit 要求） */
    private static final String SECRET = "unique-mall-test-secret-0123456789-abcdefghijklmnop";

    /** 60 秒，保证 exp - iat 在 NumericDate(秒) 精度下正好等于该值 */
    private static final long TTL_MILLIS = 60_000L;

    @Test
    @DisplayName("签发 -> 解析：subject / iat / exp 往返一致")
    void issueAndParse_roundTrip() {
        String jwt = JWTUtil.issueJWT(SECRET, TTL_MILLIS, "user-001");
        Claims claims = JWTUtil.parseJWT(SECRET, jwt);

        assertEquals("user-001", claims.getSubject());
        assertNotNull(claims.getIssuedAt(), "iat 不应为空");
        assertNotNull(claims.getExpiration(), "exp 不应为空");
        assertEquals(TTL_MILLIS,
                claims.getExpiration().getTime() - claims.getIssuedAt().getTime(),
                "exp - iat 应等于 ttl");
        assertTrue(claims.getExpiration().getTime() > System.currentTimeMillis(),
                "未过期 token 的 exp 应晚于当前时间");
        assertTrue(Math.abs(System.currentTimeMillis() - claims.getIssuedAt().getTime()) < 5_000,
                "iat 应接近当前时间");
    }

    @Test
    @DisplayName("自定义 claims 全部保留，且不影响 subject")
    void issueAndParse_withCustomClaims() {
        Map<String, Object> custom = new HashMap<>();
        custom.put("userId", 10001L);
        custom.put("username", "sakana");
        custom.put("roles", List.of("ADMIN", "USER"));
        custom.put("active", true);

        String jwt = JWTUtil.issueJWT(SECRET, TTL_MILLIS, "user-001", custom);
        Claims parsed = JWTUtil.parseJWT(SECRET, jwt);

        assertEquals("user-001", parsed.getSubject());
        assertEquals(10001L, parsed.get("userId", Number.class).longValue());
        assertEquals("sakana", parsed.get("username"));
        assertEquals(List.of("ADMIN", "USER"), parsed.get("roles"));
        assertEquals(Boolean.TRUE, parsed.get("active"));
    }

    @Test
    @DisplayName("三参重载（无自定义 claims）可正常往返")
    void issueAndParse_threeArgOverload() {
        String jwt = JWTUtil.issueJWT(SECRET, TTL_MILLIS, "user-002");
        Claims parsed = JWTUtil.parseJWT(SECRET, jwt);

        assertEquals("user-002", parsed.getSubject());
        assertNull(parsed.get("userId"));
    }

    @Test
    @DisplayName("null claims 应按空处理，不抛 NPE（回归）")
    void issueAndParse_nullClaimsTreatedAsEmpty() {
        String jwt = JWTUtil.issueJWT(SECRET, TTL_MILLIS, "user-003", null);
        Claims parsed = JWTUtil.parseJWT(SECRET, jwt);

        assertEquals("user-003", parsed.getSubject());
    }

    @Test
    @DisplayName("ttlMillis <= 0 时 token 立即过期")
    void nonPositiveTtl_tokenImmediatelyExpired() {
        String negativeTtl = JWTUtil.issueJWT(SECRET, -1_000L, "user-004");
        assertThrows(ExpiredJwtException.class, () -> JWTUtil.parseJWT(SECRET, negativeTtl));

        String zeroTtl = JWTUtil.issueJWT(SECRET, 0L, "user-004");
        assertThrows(ExpiredJwtException.class, () -> JWTUtil.parseJWT(SECRET, zeroTtl));
    }

    @Test
    @DisplayName("篡改 token 内容后校验失败：SignatureException")
    void tamperedToken_throwsSignatureException() {
        String jwt = JWTUtil.issueJWT(SECRET, TTL_MILLIS, "user-005");
        String signature = jwt.substring(jwt.lastIndexOf('.') + 1);
        char last = signature.charAt(signature.length() - 1);
        String tampered = jwt.substring(0, jwt.length() - 1) + (last == 'a' ? 'b' : 'a');
        assertNotEquals(jwt, tampered, "篡改后的 token 必须与原 token 不同");

        assertThrows(SignatureException.class, () -> JWTUtil.parseJWT(SECRET, tampered));
    }

    @Test
    @DisplayName("用错误密钥解析：SignatureException")
    void wrongSecret_throwsSignatureException() {
        String jwt = JWTUtil.issueJWT(SECRET, TTL_MILLIS, "user-006");

        assertThrows(SignatureException.class, () -> JWTUtil.parseJWT(SECRET + "-other", jwt));
    }

    @Test
    @DisplayName("畸形字符串：MalformedJwtException")
    void malformedToken_throwsMalformedJwtException() {
        assertThrows(MalformedJwtException.class, () -> JWTUtil.parseJWT(SECRET, "not-a-jwt"));
    }

    @Test
    @DisplayName("解析 null / 空字符串：IllegalArgumentException")
    void parseNullOrEmpty_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> JWTUtil.parseJWT(SECRET, null));
        assertThrows(IllegalArgumentException.class, () -> JWTUtil.parseJWT(SECRET, ""));
    }

    @Test
    @DisplayName("密钥不足 32 字节：WeakKeyException")
    void shortSecret_throwsWeakKeyException() {
        assertThrows(WeakKeyException.class, () -> JWTUtil.issueJWT("too-short", TTL_MILLIS, "user-007"));
    }

    @Test
    @DisplayName("中文等 Unicode 内容可正常往返")
    void unicodeSubjectAndClaims_roundTrip() {
        Map<String, Object> custom = new HashMap<>();
        custom.put("nickname", "早坂爱");
        custom.put("description", "雪之下雪乃");

        String jwt = JWTUtil.issueJWT(SECRET, TTL_MILLIS, "用户-001", custom);
        Claims parsed = JWTUtil.parseJWT(SECRET, jwt);

        assertEquals("用户-001", parsed.getSubject());
        assertEquals("早坂爱", parsed.get("nickname"));
        assertEquals("雪之下雪乃", parsed.get("description"));
    }

}
