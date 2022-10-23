package com.openrec.proto.biz.recommend;

import com.openrec.proto.model.RankResult;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class RecommendRes<T> {

    private List<RankResult> results;

    private List<T> detailInfos;

    public RecommendRes() {
        this(new ArrayList<>(0));
    }

    public RecommendRes(List<RankResult> results) {
        this(results, null);
    }
    public RecommendRes(List<RankResult> results, List<T> detailInfos) {
        this.results = results;
        this.detailInfos = detailInfos;
    }
}
