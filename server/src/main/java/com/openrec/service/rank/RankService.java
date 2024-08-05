package com.openrec.service.rank;

import com.google.common.collect.Maps;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class RankService {

    @Value("${rank.host}")
    private String rankHost;

    @Value("${rank.port}")
    private String rankPort;

    private static final String SCORE_PATH = "/model/score";

    @Autowired
    private RestTemplate restTemplate;

    @Data
    public static class RankUserItems {
        private String userId;
        private List<String> itemIds;

        public RankUserItems(String userId, List<String> itemIds) {
            this.userId = userId;
            this.itemIds = itemIds;
        }
    }

    @Data
    public static class RankItemScores {
        private int code;
        private String status;
        private String message;
        private Map<String, Double> data;
    }

    public Map<String, Double> score(String userId, List<String> itemIds) {
        String scoreUrl = String.format("http://%s:%s%s", rankHost, rankPort, SCORE_PATH);
        RankUserItems rUserItems = new RankUserItems(userId, itemIds);
        RankItemScores rankItemScores = restTemplate.postForObject(scoreUrl, rUserItems, RankItemScores.class);
        if(rankItemScores.getCode() == 0) {
            rankItemScores.getData();
        }
        return Maps.newHashMap();
    }
}
