package com.jbr.middletier.money.data;

@SuppressWarnings("unused")
public class FileResponse {
    private String file;

    public FileResponse (String file) {
        this.file = file;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
