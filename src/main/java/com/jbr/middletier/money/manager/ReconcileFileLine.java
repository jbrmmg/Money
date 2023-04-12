package com.jbr.middletier.money.manager;

import java.util.Arrays;
import java.util.List;

public class ReconcileFileLine {
    private final String line;
    private final List<String> columns;

    public ReconcileFileLine(String line) {
        this.line = line;
        this.columns = Arrays.asList(line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1));
    }

    public String getLine() {
        return this.line;
    }

    public List<String> getColumns() {
        return this.columns;
    }
}
