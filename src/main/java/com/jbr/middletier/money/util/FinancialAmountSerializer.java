package com.jbr.middletier.money.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class FinancialAmountSerializer extends JsonSerializer<FinancialAmount> {
    @Override
    public void serialize(FinancialAmount financialAmount, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("value", financialAmount.getValue());
        jsonGenerator.writeStringField("type", financialAmount.getType().toString());
        jsonGenerator.writeEndObject();
    }
}
