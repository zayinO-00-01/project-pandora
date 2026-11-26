package com.projectpandora.api.user;

import com.projectpandora.api.common.ApiException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Shared validation and masking rules for the enriched user profile. */
public final class ProfileRules {

    private static final Pattern PHONE = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern ID_CARD = Pattern.compile("[0-9]{17}[0-9X]");
    private static final Pattern LANDLINE = Pattern.compile("0[1-9]\\d{1,2}[- ]?[1-9]\\d{6,7}");
    private static final int[] ID_WEIGHTS = {7,9,10,5,8,4,2,1,6,3,7,9,10,5,8,4,2};
    private static final String ID_CHECKSUMS = "10X98765432";
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
            throw new ApiException(400, "身份证号不能为空");
        }
        String value = idCard.trim().toUpperCase(Locale.ROOT);
        if (!ID_CARD.matcher(value).matches() || value.startsWith("000000")) {
            throw new ApiException(400, "请输入18位身份证号，末位可为数字或X");
        }
        int sum=0;
        for(int i=0;i<ID_WEIGHTS.length;i++) sum += (value.charAt(i)-'0') * ID_WEIGHTS[i];
        if(value.charAt(17)!=ID_CHECKSUMS.charAt(sum%11))throw new ApiException(400,"身份证号校验位不正确");
        birthDateFromId(value);
        return value;
    }

    public static LocalDate birthDateFromId(String normalizedId) {
        if (normalizedId == null) return null;
        String encoded=normalizedId.substring(6,14);
        try {
            LocalDate date=LocalDate.parse(encoded,DateTimeFormatter.BASIC_ISO_DATE);
            if(date.getYear()<1 || date.isAfter(LocalDate.now()))throw new ApiException(400,"身份证中的出生日期无效");
            return date;
        } catch(DateTimeParseException e) {
            throw new ApiException(400,"身份证中的出生日期无效");
        }
    }

    public static String normalizeEmergencyPhone(String supplied,String previous) {
        String phone=supplied==null || supplied.isBlank() ? null : supplied.trim();
        if (phone==null || Objects.equals(phone, previous==null?null:previous.trim())) return phone;
        if (!PHONE.matcher(phone).matches() && !LANDLINE.matcher(phone).matches())
            throw new ApiException(400,"紧急联系电话请输入有效的 11 位手机号或带区号的座机");
        return phone;
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
