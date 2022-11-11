package com.openrec.graph.config;


import lombok.Data;

@Data
public class EmbeddingConfig {

    private int size;
    private String vectorField;
    private String idField;
}
