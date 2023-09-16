package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.schedule.AdjustmentType;
import org.modelmapper.AbstractConverter;

public class AdjustmentTypeStringConverter extends AbstractConverter<AdjustmentType,String> {
    @Override
    protected String convert(AdjustmentType adjustmentType) {
        if(adjustmentType == null) return null;

        return adjustmentType.toString();
    }
}
