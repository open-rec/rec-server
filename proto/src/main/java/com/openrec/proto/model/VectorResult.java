package com.openrec.proto.model;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class VectorResult implements Serializable {

    private String id;
    private List<Double> vector;
    public VectorResult() {}
}
