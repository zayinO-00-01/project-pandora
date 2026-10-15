package com.projectpandora.api.task;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import com.projectpandora.api.common.ApiException;
import jakarta.validation.constraints.Size;
import java.io.IOException;

public record ProgressRequest(
        @Min(value = 0, message = "百分比不能小于0") @Max(value = 100, message = "百分比不能超过100")
        @JsonDeserialize(using = ProgressRequest.StrictIntegerDeserializer.class) Integer progress,
        @NotBlank(message = "进度说明不能为空") @Size(max = 2000, message = "进度说明不能超过2000字") String progressNote,
        @Pattern(regexp = "todo|doing|done", message = "请选择待开始、进行中或已完成") String status) {
    public ProgressRequest {
        progressNote = progressNote == null ? null : progressNote.trim();
        status = status == null ? null : status.trim();
    }

    public String resolvedStatus() {
        // Transitional compatibility for already-installed percentage-only APKs.
        String state = status;
        if (state == null) {
            if (progress == null) throw new ApiException(400, "请选择工作状态");
            state = progress == 0 ? "todo" : progress == 100 ? "done" : "doing";
        }
        if (progress != null && ((state.equals("todo") && progress != 0)
                || (state.equals("done") && progress != 100)
                || (state.equals("doing") && progress == 100)))
            throw new ApiException(400, "百分比与工作状态不一致，可以清空百分比后提交");
        return state;
    }

    public static class StrictIntegerDeserializer extends JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT))
                throw JsonMappingException.from(parser, "进度必须为0到100之间的整数");
            return parser.getIntValue();
        }
    }
}
