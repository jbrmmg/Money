package com.jbr.middletier.money.util;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class FinancialAmountDeserializer extends JsonDeserializer<FinancialAmount> {
    @Override
    public FinancialAmount deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        double value = node.get("value").asDouble();

        // If the type does not match the value, then correct it.
        String type = node.get("type").asText();
        if(type != null) {
            if( (type.equalsIgnoreCase("db") && (value > 0)) ||
                    (type.equalsIgnoreCase("cr") && (value < 0)) ){
                value *= -1;
            }
        }

        return new FinancialAmount(value);
    }
}
