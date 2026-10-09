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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;

public record ProgressRequest(
        @NotNull(message = "请填写进度") @Min(0) @Max(100)
        @JsonDeserialize(using = ProgressRequest.StrictIntegerDeserializer.class) Integer progress,
        @NotBlank(message = "进度说明不能为空") @Size(max = 2000, message = "进度说明不能超过2000字") String progressNote) {
    public ProgressRequest {
        progressNote = progressNote == null ? null : progressNote.trim();
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