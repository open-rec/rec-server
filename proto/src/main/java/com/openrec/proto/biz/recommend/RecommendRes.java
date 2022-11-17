package com.openrec.proto.biz.recommend;

import java.util.ArrayList;
import java.util.List;

import com.openrec.proto.model.ScoreResult;

import lombok.Data;

@Data
public class RecommendRes<T> {

    private List<ScoreResult> results;

    private List<T> detailInfos;

    public RecommendRes() {
        this(new ArrayList<>(0));
    }

    public RecommendRes(List<ScoreResult> results) {
        this(results, null);
    }

    public RecommendRes(List<ScoreResult> results, List<T> detailInfos) {
        this.results = results;
        this.detailInfos = detailInfos;
    }
}
