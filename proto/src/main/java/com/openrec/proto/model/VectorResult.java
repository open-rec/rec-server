package com.openrec.proto.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@AllArgsConstructor
@Data
public class VectorResult implements Serializable {

    private String id;
    private List<Double> vector;
}
