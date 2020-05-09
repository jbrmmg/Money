package com.jbr.middletier.money.data;

@SuppressWarnings("unused")
public class LoadFileRequest {
    private String path;
    private String type;

    public LoadFileRequest() {
        this.path = "";
        this.type = "";
    }

    public LoadFileRequest(String path, String type) {
        this.path = path;
        this.type = type;
    }

    public String getPath() {
        return this.path;
    }

    public String getType() {
        return this.type;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setType(String type) {
        this.type = type;
    }
}
