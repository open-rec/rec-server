package com.openrec.proto.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@AllArgsConstructor
@Data
public class ScoreResult implements Serializable {

    private String id;
    private double score;
}
