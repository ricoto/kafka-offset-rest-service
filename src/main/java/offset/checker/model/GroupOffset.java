package offset.checker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupOffset {

    private String groupId;

    private Long maxOffsetLag;

    private List<TopicInfo> topicInfo;

    private List<TopicOffset> topicOffsets;

}
