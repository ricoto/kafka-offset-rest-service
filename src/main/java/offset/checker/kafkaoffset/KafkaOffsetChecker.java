package offset.checker.kafkaoffset;

import offset.checker.model.GroupOffset;
import offset.checker.model.TopicInfo;
import offset.checker.model.TopicOffset;
import offset.checker.model.TopicPartitionOffset;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupsResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Component
public class KafkaOffsetChecker {

    private static final Logger logger = LoggerFactory.getLogger(KafkaOffsetChecker.class);

    @Value("${kafka.bootstrap.server}")
    private String bootstrapServer;

    @Value("${kafka.sasl.enabled}")
    private String saslEnabled;

    @Value("${kafka.ssl.endpoint.identification.algorithm}")
    private String sslIdentificationAlgorithm;

    @Value("${kafka.security.protocol}")
    private String securityProtocol;

    @Value("${kafka.sasl.mechanism}")
    private String saslMechanism;

    @Value("${kafka.api.key}")
    private String apiKey;

    @Value("${kafka.api.secret}")
    private String apiSecret;

    @Value("${kafka.poll.timeout}")
    private long timeout;

    private KafkaConsumer<byte[], byte[]> kafkaConsumer;

    private AdminClient adminClient;

    @PostConstruct
    public void init() {
        adminClient = createAdminClient(bootstrapServer);
        initConsumer();
    }

    private void initConsumer() {
        Map<String, Object> params = new HashMap<>();
        params.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        params.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        params.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        params.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        params.putAll(getSaslConfig(saslEnabled, sslIdentificationAlgorithm, securityProtocol, saslMechanism, apiKey, apiSecret));

        kafkaConsumer = new KafkaConsumer<>(params);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> kafkaConsumer.wakeup()));
    }

    public List<GroupOffset> collectAllGroupsOffset() {
        return collectAllGroupsffsetsByTopic(null);
    }

    public GroupOffset collectOffsetByGroup(String groupId) {
        return collectOffsetByTopicGroup(null, groupId);
    }

    public List<GroupOffset> collectOffsetByTopic(String topic) {
        return collectAllGroupsffsetsByTopic(topic).stream().filter((group) -> group.getTopicInfo().stream().filter((topicInfo) -> topicInfo.getName().equals(topic)).count() > 0).collect(Collectors.toList());
    }

    public List<GroupOffset> collectAllGroupsffsetsByTopic(String topic) {
        List<GroupOffset> groupsOffset = new ArrayList<>();
        try {
            ListConsumerGroupsResult res = adminClient.listConsumerGroups();
            for (ConsumerGroupListing groupListing : res.valid().get()) {
                groupsOffset.add(collectOffsetByTopicGroup(topic, groupListing.groupId()));
            }
        } catch (ExecutionException | InterruptedException e) {
            logger.error(e.getMessage(), e);
        }

        Collections.sort(groupsOffset, Comparator.comparing(GroupOffset::getMaxOffsetLag).reversed());
        return groupsOffset;
    }

    public GroupOffset collectOffsetByTopicGroup(String topic, String groupId) {
        ListConsumerGroupOffsetsResult res = adminClient.listConsumerGroupOffsets(groupId);
        Map<String, TopicOffset> topicMap = new HashMap<>();

        try {
            Map<TopicPartition, OffsetAndMetadata> offsetMetadata =
                    res.partitionsToOffsetAndMetadata().get(timeout, TimeUnit.MILLISECONDS);
            Set<TopicPartition> topicPartitions = offsetMetadata.keySet();

            Map<TopicPartition, Long> endOffsetsPerPartition =
                    getEndOffsetsPerPartition(topicPartitions);

            for (TopicPartition partition : topicPartitions) {

                if (topic != null && !topic.equals(partition.topic())) {
                    continue;
                }

                TopicOffset topicOffset = topicMap.get(partition.topic());
                if (topicOffset == null) {
                    topicOffset = new TopicOffset(partition.topic(), 0L, new ArrayList<>());
                    topicMap.put(partition.topic(), topicOffset);
                }

                OffsetAndMetadata offsetAndMetadata = offsetMetadata.get(partition);
                long currentOffset = offsetAndMetadata.offset();

                Long endOffset = endOffsetsPerPartition.get(partition);
                long lag = endOffset - currentOffset;

                topicOffset.setTopicOffsetLag(topicOffset.getTopicOffsetLag() + lag);
                topicOffset.getPartitionOffsets().add(new TopicPartitionOffset(partition.partition(), currentOffset, endOffset, lag));
                logger.info("group: {}, topic: {}, partition: {}, offset: {}, end offset: {},  topicOffsetLag: {}",
                        groupId, partition.topic(), partition.partition(), currentOffset, endOffset, lag);
            }

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Error occurred while fetching partition info", e);
        }

        List<TopicOffset> lagSortedTopics = new ArrayList<>(topicMap.values());
        Collections.sort(lagSortedTopics, Comparator.comparing(TopicOffset::getTopicOffsetLag).reversed());
        lagSortedTopics.stream().forEach(topicOffset -> Collections.sort(topicOffset.getPartitionOffsets(), Comparator.comparing(TopicPartitionOffset::getOffsetLag).reversed()));
        long maxLag = 0;
        if (!lagSortedTopics.isEmpty()) {
            maxLag = lagSortedTopics.get(0).getTopicOffsetLag();
        }

        return new GroupOffset(groupId, maxLag, lagSortedTopics.stream().map(a -> new TopicInfo(a.getTopic(), a.getTopicOffsetLag())).collect(Collectors.toList()), lagSortedTopics);
    }

    private Map<TopicPartition, Long> getEndOffsetsPerPartition(Set<TopicPartition> topicPartitions) {
        try {
            return kafkaConsumer.endOffsets(topicPartitions, Duration.of(timeout, ChronoUnit.MILLIS));
        } catch (IllegalStateException e) {
            logger.warn(e.getMessage(), e);
            synchronized (this) {
                initConsumer();
            }
            return kafkaConsumer.endOffsets(topicPartitions, Duration.of(timeout, ChronoUnit.MILLIS));
        }
    }

    private AdminClient createAdminClient(String bootstrapServer) {
        Properties properties = new Properties();
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        properties.putAll(getSaslConfig(saslEnabled, sslIdentificationAlgorithm, securityProtocol, saslMechanism, apiKey, apiSecret));
        return AdminClient.create(properties);
    }

    private Map<String, Object> getSaslConfig(String saslEnabled, String sslIdentificationAlgorithm, String securityProtocol, String saslMechanism, String apiKey, String apiSecret) {
        Map<String, Object> configMap = new HashMap<>();
        if (saslEnabled.equals("true")) {
            configMap.put("ssl.endpoint.identification.algorithm", sslIdentificationAlgorithm);
            configMap.put("security.protocol", securityProtocol);
            configMap.put("sasl.mechanism", saslMechanism);
            configMap.put("sasl.jaas.config", String.format("org.apache.kafka.common.security.plain.PlainLoginModule " +
                    "required username=\"%s\" password=\"%s\";", apiKey, apiSecret));
        }
        return configMap;
    }

}
