package com.jbr.middletier.money.manager;

import java.util.Arrays;
import java.util.List;

public class ReconcileFileLine {
    private final String line;
    private final List<String> columns;
    private final int lineNumber;

    public ReconcileFileLine(int lineNumber, String line) {
        this.line = line;
        this.columns = Arrays.asList(line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1));
        this.lineNumber = lineNumber;
    }

    public String getLine() {
        return this.line;
    }

    public List<String> getColumns() {
        return this.columns;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }
}
