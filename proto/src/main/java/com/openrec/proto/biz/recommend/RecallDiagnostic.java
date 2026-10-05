package com.openrec.proto.biz.recommend;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request-scoped recall output before filtering, ranking and final truncation. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecallDiagnostic {
    private String node;
    private String channel;
    private String status;
    private int candidateCount;
}
