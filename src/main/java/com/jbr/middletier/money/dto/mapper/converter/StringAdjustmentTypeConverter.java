package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.schedule.AdjustmentType;
import org.modelmapper.AbstractConverter;

public class StringAdjustmentTypeConverter extends AbstractConverter<String, AdjustmentType> {
    @Override
    protected AdjustmentType convert(String s) {
        if(s == null) return null;

        return AdjustmentType.getAdjustmentType(s);
    }
}
