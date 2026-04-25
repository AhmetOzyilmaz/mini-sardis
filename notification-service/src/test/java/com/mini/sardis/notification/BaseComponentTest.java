package com.mini.sardis.notification;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("componentTest")
@SuppressFBWarnings({"RV_RETURN_VALUE_IGNORED"})
@ComponentScan(excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {KafkaTestConsumerImpl.class}))
@SuppressWarnings({"PMD"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CustomTestWatcher.class)
@EmbeddedKafka(partitions = 1, topics = {
        "subscription.activated.v1", "subscription.cancelled.v1",
        "subscription.failed.v1", "subscription.renewed.v1", "subscription.suspended.v1"
})
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@Slf4j
public abstract class BaseComponentTest {

    @Autowired
    protected TestRestTemplate restTemplate;
}
