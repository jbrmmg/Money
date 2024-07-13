package com.jbr.middletier.money.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.math.BigDecimal;

public class FinancialAmountDeserializer extends JsonDeserializer<FinancialAmount> {
    @Override
    public FinancialAmount deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        BigDecimal value = BigDecimal.valueOf(node.get("value").asDouble());

        // If the type does not match the value, then correct it.
        String type = node.get("type").asText();
        if(type != null &&
                (   (type.equalsIgnoreCase("db") && (FinancialAmount.positive(value))) ||
                    (type.equalsIgnoreCase("cr") && (FinancialAmount.negative(value))) )) {
            value = FinancialAmount.flipSign(value);
        }

        return new FinancialAmount(value);
    }
}
