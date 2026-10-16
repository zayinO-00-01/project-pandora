package com.projectpandora.api.user;

import com.projectpandora.api.common.ApiException;
import java.time.LocalDate;
import java.util.Set;
import java.util.regex.Pattern;

/** Shared validation and masking rules for the enriched user profile. */
public final class ProfileRules {

    private static final Pattern PHONE = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern ID_CARD = Pattern.compile("(\\d{15}|\\d{17}[0-9Xx])");
    private static final Set<String> GENDERS = Set.of("MALE", "FEMALE", "UNSPECIFIED");

    private ProfileRules() {}

    public static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new ApiException(400, "手机号不能为空");
        }
        String value = phone.trim();
        if (!PHONE.matcher(value).matches()) {
            throw new ApiException(400, "手机号格式不正确");
        }
        return value;
    }

    public static String normalizeIdCard(String idCard) {
        if (idCard == null || idCard.isBlank()) {
            return null;
        }
        String value = idCard.trim().toUpperCase();
        if (!ID_CARD.matcher(value).matches()) {
            throw new ApiException(400, "身份证号格式不正确");
        }
        return value;
    }

    public static String normalizeGender(String gender) {
        if (gender == null || gender.isBlank()) {
            return null;
        }
        String value = gender.trim().toUpperCase();
        if (!GENDERS.contains(value)) {
            throw new ApiException(400, "性别取值不正确");
        }
        return value;
    }

    public static LocalDate validateBirthDate(LocalDate birthDate) {
        if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
            throw new ApiException(400, "出生日期不能晚于今天");
        }
        return birthDate;
    }

    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 4) {
            return null;
        }
        return "****" + idCard.substring(idCard.length() - 4);
    }
}
