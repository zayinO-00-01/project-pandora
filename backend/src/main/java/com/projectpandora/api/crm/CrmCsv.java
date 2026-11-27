package com.projectpandora.api.crm;

import static com.projectpandora.api.crm.CrmStore.bad;
import java.util.ArrayList;
import java.util.List;

final class CrmCsv {
    static final List<String> HEADER = List.of("source", "memberNumber", "orderNumber", "product", "status",
        "amount", "refundAmount", "paidOn", "refundedOn", "name", "phone", "ownerId");

    private CrmCsv() {}

    static List<List<String>> read(String csv) {
        if (csv == null || csv.isBlank() || csv.length() > 128_000) throw bad("请选择不超过 128 KB 的 CSV");
        if (csv.charAt(0) == '\uFEFF') csv = csv.substring(1);
        List<List<String>> rows = new ArrayList<>();
        List<String> line = new ArrayList<>();
        var field = new StringBuilder();
        boolean quoted = false, closed = false;
        for (int i = 0; i <= csv.length(); i++) {
            char ch = i == csv.length() ? '\n' : csv.charAt(i);
            if (quoted) {
                if (ch != '"') field.append(ch);
                else if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') { field.append('"'); i++; }
                else { quoted = false; closed = true; }
            } else if (ch == ',' || ch == '\r' || ch == '\n') {
                line.add(field.toString().strip());
                field.setLength(0);
                closed = false;
                if (ch != ',') {
                    if (line.size() > 1 || !line.get(0).isEmpty()) rows.add(line);
                    line = new ArrayList<>();
                    if (ch == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') i++;
                }
                if (rows.size() > 201 || line.size() > 12) throw bad("CSV 最多 200 条订单，每条 12 列");
            } else if (ch == '"' && !closed && field.isEmpty()) quoted = true;
            else if (ch == '"' || closed) throw bad("CSV 引号格式不正确");
            else field.append(ch);
        }
        if (quoted || rows.size() < 2 || !rows.get(0).equals(HEADER)) throw bad("请按商城订单模板填写 CSV");
        return rows.subList(1, rows.size());
    }
}
