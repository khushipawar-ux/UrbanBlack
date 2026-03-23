package com.urbanblack.rideservice.service;

import com.urbanblack.rideservice.entity.RewardTreeNode;
import com.urbanblack.rideservice.repository.RewardTreeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BfsParentCache {

    private final StringRedisTemplate redisTemplate;
    private final RewardTreeRepository treeRepo;
    private static final String PARENT_ID_KEY = "reward_tree:next_parent_id";

    public Optional<RewardTreeNode> getNextParent() {
        String cachedId = redisTemplate.opsForValue().get(PARENT_ID_KEY);
        if (cachedId != null) {
            return treeRepo.findById(Long.parseLong(cachedId));
        }
        return Optional.empty();
    }

    public void invalidate() {
        redisTemplate.delete(PARENT_ID_KEY);
    }
    
    public void setParent(Long nodeId) {
        redisTemplate.opsForValue().set(PARENT_ID_KEY, nodeId.toString());
    }
}
