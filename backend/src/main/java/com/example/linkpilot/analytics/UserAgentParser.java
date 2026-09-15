package com.example.linkpilot.analytics;

/**
 * Deliberately hand-rolled instead of pulling in a UA-parsing library: a handful of
 * substring checks is enough to bucket device/browser/OS for analytics purposes, and
 * it's a dependency-free, easy-to-explain piece of the pipeline.
 */
public final class UserAgentParser {

    public record Result(String deviceType, String browser, String operatingSystem) {
    }

    private static final Result UNKNOWN = new Result("unknown", "unknown", "unknown");

    public static Result parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return UNKNOWN;
        }
        String ua = userAgent.toLowerCase();
        return new Result(deviceType(ua), browser(ua), operatingSystem(ua));
    }

    private static String deviceType(String ua) {
        if (ua.contains("ipad") || ua.contains("tablet")) {
            return "tablet";
        }
        if (ua.contains("mobile") || ua.contains("iphone") || ua.contains("android")) {
            return "mobile";
        }
        return "desktop";
    }

    private static String browser(String ua) {
        if (ua.contains("edg/")) {
            return "edge";
        }
        if (ua.contains("opr/") || ua.contains("opera")) {
            return "opera";
        }
        if (ua.contains("chrome/") || ua.contains("crios/")) {
            return "chrome";
        }
        if (ua.contains("firefox/") || ua.contains("fxios/")) {
            return "firefox";
        }
        if (ua.contains("safari/")) {
            return "safari";
        }
        return "other";
    }

    private static String operatingSystem(String ua) {
        if (ua.contains("windows")) {
            return "windows";
        }
        if (ua.contains("mac os") || ua.contains("macos")) {
            return "macos";
        }
        if (ua.contains("android")) {
            return "android";
        }
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains(" ios")) {
            return "ios";
        }
        if (ua.contains("linux")) {
            return "linux";
        }
        return "other";
    }

    private UserAgentParser() {
    }
}
