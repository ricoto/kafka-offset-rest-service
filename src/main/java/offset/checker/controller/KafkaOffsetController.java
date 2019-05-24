package offset.checker.controller;

import offset.checker.kafkaoffset.KafkaOffsetChecker;
import offset.checker.model.GroupOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/offset")
public class KafkaOffsetController {

    @Autowired
    private KafkaOffsetChecker kafkaOffsetChecker;

    @GetMapping(value = "/topic/{topic}/group/{groupId}", produces = {"application/json"})
    public GroupOffset retrieveByTopicAndGroup(@PathVariable String topic, @PathVariable String groupId) {
        return kafkaOffsetChecker.collectOffsetByTopicGroup(topic, groupId);
    }

    @GetMapping(value = "/group/{groupId}/topic/{topic}", produces = {"application/json"})
    public GroupOffset retrieveByGroupAndTopic(@PathVariable String groupId, @PathVariable String topic) {
        return kafkaOffsetChecker.collectOffsetByTopicGroup(topic, groupId);
    }

    @GetMapping(value = "/group/{groupId}", produces = {"application/json"})
    public GroupOffset retrieveByGroup(@PathVariable String groupId) {
        return kafkaOffsetChecker.collectOffsetByGroup(groupId);
    }

    @GetMapping(value = "/topic/{topic}", produces = {"application/json"})
    public List<GroupOffset> retrieveByTopic(@PathVariable String topic) {
        return kafkaOffsetChecker.collectOffsetByTopic(topic);
    }

    @GetMapping (value="/", produces = {"application/json"})
    public List<GroupOffset> retrieveAll() {
        return kafkaOffsetChecker.collectAllGroupsOffset();
    }


}
