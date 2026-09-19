package com.meridiantrust.sentinel.security;

/** Tiny, dependency-free PII masking helper used by DTO mappers. */
public final class PiiMasker {

    private PiiMasker() {
    }

    public static String maskName(String fullName) {
        if (fullName == null || fullName.isBlank()) return fullName;
        StringBuilder sb = new StringBuilder();
        for (String part : fullName.split(" ")) {
            if (part.isEmpty()) continue;
            sb.append(part.charAt(0)).append("*".repeat(Math.max(1, part.length() - 1))).append(" ");
        }
        return sb.toString().trim();
    }

    public static String maskIdNumber(String idNumber) {
        if (idNumber == null || idNumber.length() < 4) return "****";
        return "****" + idNumber.substring(idNumber.length() - 4);
    }
}
