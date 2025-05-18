package com.jbr.middletier.money.manager;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public class ReconcileFileLine {
    private final String line;
    private final List<String> columns;
    private final int lineNumber;

    public ReconcileFileLine(int lineNumber, String line) {
        this.line = line;
        this.columns = Arrays.asList(line.split(",(?=(?:[^\"]*+\"[^\"]*\")*+[^\"]*$)", -1));
        this.lineNumber = lineNumber;
    }

}
