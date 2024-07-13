package com.jbr.middletier.money.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class FinancialAmountSerializer extends JsonSerializer<FinancialAmount> {
    @Override
    public void serialize(FinancialAmount financialAmount, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();

        BigDecimal value = financialAmount.getValue();
        value = value.setScale(2, RoundingMode.HALF_UP);

        jsonGenerator.writeNumberField("value", value);
        jsonGenerator.writeStringField("type", financialAmount.getType().toString());
        jsonGenerator.writeEndObject();
    }
}
