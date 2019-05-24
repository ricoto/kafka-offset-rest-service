package offset.checker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicPartitionOffset {

    private int partition;

    private long currentOffset;

    private long newestOffset;

    private long offsetLag;

}
