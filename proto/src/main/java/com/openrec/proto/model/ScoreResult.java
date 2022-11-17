package com.openrec.proto.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ScoreResult implements Serializable {

    private String id;
    private double score;
    public ScoreResult() {}
}
