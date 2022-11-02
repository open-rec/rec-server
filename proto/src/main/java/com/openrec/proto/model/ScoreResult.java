package com.openrec.proto.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ScoreResult {

    private String id;
    private double score;
}
