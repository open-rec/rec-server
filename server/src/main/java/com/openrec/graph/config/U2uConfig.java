package com.openrec.graph.config;

/** Configuration for a precomputed user-to-user recall channel. */
public class U2uConfig extends RecallConfig {
    private int size;
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
