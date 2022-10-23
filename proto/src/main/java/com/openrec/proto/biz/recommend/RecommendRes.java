package com.openrec.proto.biz.recommend;

import com.openrec.proto.model.RankResult;
import lombok.Data;

import java.util.List;


@Data
public class RecommendRes<T> {

    private List<RankResult> results;

    private List<T> detailInfos;
}
