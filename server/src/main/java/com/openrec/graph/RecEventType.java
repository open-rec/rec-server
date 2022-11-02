package com.openrec.graph;

public enum RecEventType {

    CLICK,
    EXPOSE,
    COLLECT,
    LIKE,
    COMMENT,
    BUY,
    DISLIKE;

    RecEventType() {}

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
