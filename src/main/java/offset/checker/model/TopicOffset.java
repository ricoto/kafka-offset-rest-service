package offset.checker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicOffset {

    private String topic;

    private Long topicOffsetLag;

    private List<TopicPartitionOffset> partitionOffsets;

}
