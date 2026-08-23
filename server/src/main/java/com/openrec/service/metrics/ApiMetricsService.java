package com.openrec.service.metrics;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import com.openrec.proto.biz.push.AbstractPushReq;
import com.openrec.proto.biz.recommend.RecommendRes;
import com.openrec.util.JsonUtil;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class ApiMetricsService {

    private final MeterRegistry meterRegistry;

    public ApiMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordPush(String type, AbstractPushReq<?> request, Runnable action) {
        record("push", type, () -> {
            List<?> data = request == null ? null : request.getData();
            int records = data == null ? 0 : data.size();
            long bytes = JsonUtil.objToJson(request).getBytes(StandardCharsets.UTF_8).length;
            summary("openrec_push_payload_records", type).record(records);
            summary("openrec_push_payload_bytes", type).record(bytes);
            action.run();
            return null;
        });
    }

    public <T> RecommendRes<T> recordRecommend(Supplier<RecommendRes<T>> action) {
        return recordRecommend("default", action);
    }

    public <T> RecommendRes<T> recordRecommend(String experiment, Supplier<RecommendRes<T>> action) {
        String experimentTag = experimentTag(experiment);
        RecommendRes<T> response = record("recommend", "recommend", experimentTag, action);
        int resultSize = response == null || response.getResults() == null ? 0 : response.getResults().size();
        summary("openrec_recommend_result_items", "recommend", experimentTag).record(resultSize);
        return response;
    }

    private <T> T record(String api, String type, Supplier<T> action) {
        return record(api, type, "default", action);
    }

    private <T> T record(String api, String type, String experiment, Supplier<T> action) {
        experiment = experimentTag(experiment);
        Counter.builder("openrec_api_requests")
            .description("Total OpenRec API requests")
            .tags("api", api, "type", type, "ab", experiment)
            .register(meterRegistry).increment();
        long started = System.nanoTime();
        try {
            return action.get();
        } catch (RuntimeException | Error error) {
            Counter.builder("openrec_api_errors")
                .description("Total failed OpenRec API requests")
                .tags("api", api, "type", type, "ab", experiment)
                .register(meterRegistry).increment();
            throw error;
        } finally {
            Timer.builder("openrec_api_latency")
                .description("OpenRec API processing latency")
                .tags("api", api, "type", type, "ab", experiment)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(System.nanoTime() - started, TimeUnit.NANOSECONDS);
        }
    }

    private DistributionSummary summary(String name, String type) {
        return summary(name, type, "default");
    }

    private DistributionSummary summary(String name, String type, String experiment) {
        DistributionSummary.Builder builder = DistributionSummary.builder(name)
            .tags("api", name.startsWith("openrec_push") ? "push" : "recommend", "type", type,
                "ab", experimentTag(experiment));
        if ("openrec_recommend_result_items".equals(name)) {
            // Result sizes are integers, so the 0.5 bucket contains only empty (zero-item) responses.
            builder.publishPercentileHistogram().serviceLevelObjectives(0.5, 1, 5, 10, 20, 50, 100);
        }
        return builder.register(meterRegistry);
    }

    private String experimentTag(String experiment) {
        return experiment == null || experiment.trim().isEmpty() ? "default" : experiment.trim();
    }
}
