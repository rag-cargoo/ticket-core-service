package com.ticketrush.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/system/runtime")
@RequiredArgsConstructor
public class RuntimeInfraStatusController {

    private static final long KAFKA_TIMEOUT_MILLIS = 1_200L;

    private final StringRedisTemplate redisTemplate;

    @Value("${spring.kafka.bootstrap-servers:}")
    private String kafkaBootstrapServers;

    @GetMapping
    public RuntimeInfraStatusResponse getRuntimeInfraStatus() {
        return new RuntimeInfraStatusResponse(
                Instant.now().toString(),
                probeRedis(),
                probeKafka()
        );
    }

    private InfraStatus probeRedis() {
        try {
            String ping = redisTemplate.execute(RedisConnection::ping);
            boolean up = "PONG".equalsIgnoreCase(ping);
            String detail = StringUtils.hasText(ping) ? ping : "no ping response";
            return new InfraStatus(up ? "UP" : "DOWN", up, detail);
        } catch (Exception exception) {
            return new InfraStatus("DOWN", false, toShortErrorMessage(exception));
        }
    }

    private InfraStatus probeKafka() {
        if (!StringUtils.hasText(kafkaBootstrapServers)) {
            return new InfraStatus("DOWN", false, "kafka bootstrap servers not configured");
        }

        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        properties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "1000");
        properties.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "1000");

        try (AdminClient adminClient = AdminClient.create(properties)) {
            var cluster = adminClient.describeCluster();
            int nodeCount = cluster.nodes().get(KAFKA_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).size();
            String clusterId = cluster.clusterId().get(KAFKA_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            boolean up = nodeCount > 0;
            String detail = "nodes=" + nodeCount + ", clusterId=" + (StringUtils.hasText(clusterId) ? clusterId : "n/a");
            return new InfraStatus(up ? "UP" : "DOWN", up, detail);
        } catch (Exception exception) {
            return new InfraStatus("DOWN", false, toShortErrorMessage(exception));
        }
    }

    private String toShortErrorMessage(Exception exception) {
        String simpleName = exception.getClass().getSimpleName();
        String message = exception.getMessage();
        if (!StringUtils.hasText(message)) {
            return simpleName;
        }
        String trimmed = message.trim().replaceAll("\\s+", " ");
        if (trimmed.length() > 180) {
            return simpleName + ": " + trimmed.substring(0, 177) + "...";
        }
        return simpleName + ": " + trimmed;
    }

    public record RuntimeInfraStatusResponse(
            String checkedAt,
            InfraStatus redis,
            InfraStatus kafka
    ) {}

    public record InfraStatus(
            String status,
            boolean up,
            String detail
    ) {}
}
