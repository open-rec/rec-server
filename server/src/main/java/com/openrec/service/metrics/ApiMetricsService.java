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
        RecommendRes<T> response = record("recommend", "recommend", action);
        int resultSize = response == null || response.getResults() == null ? 0 : response.getResults().size();
        summary("openrec_recommend_result_items", "recommend").record(resultSize);
        return response;
    }

    private <T> T record(String api, String type, Supplier<T> action) {
        Counter.builder("openrec_api_requests")
            .description("Total OpenRec API requests")
            .tags("api", api, "type", type)
            .register(meterRegistry).increment();
        long started = System.nanoTime();
        try {
            return action.get();
        } catch (RuntimeException | Error error) {
            Counter.builder("openrec_api_errors")
                .description("Total failed OpenRec API requests")
                .tags("api", api, "type", type)
                .register(meterRegistry).increment();
            throw error;
        } finally {
            Timer.builder("openrec_api_latency")
                .description("OpenRec API processing latency")
                .tags("api", api, "type", type)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(System.nanoTime() - started, TimeUnit.NANOSECONDS);
        }
    }

    private DistributionSummary summary(String name, String type) {
        return DistributionSummary.builder(name)
            .tags("api", name.startsWith("openrec_push") ? "push" : "recommend", "type", type)
            .register(meterRegistry);
    }
}
