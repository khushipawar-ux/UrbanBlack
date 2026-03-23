package com.urbanblack.rideservice.service;

import com.urbanblack.common.dto.UplineRecord;
import com.urbanblack.rideservice.dto.RewardResult;
import com.urbanblack.rideservice.entity.RewardTreeNode;
import com.urbanblack.rideservice.repository.RewardTreeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RewardTreeService {

    private final RewardTreeRepository treeRepo;
    private final RewardEventPublisher eventPublisher;
    private final BfsParentCache bfsCache;
    private final EntityManager em;

    /**
     * Business rules:
     * - Each new node contributes ₹10 total pool (handled in wallet-service via event payload)
     * - Universal Sliding Window: Reward the nearest 9 ancestors based purely on position in the tree.
     * - If fewer than 9 ancestors exist (Levels 1 to 9), "all upperlines" receive ₹1 each.
     * - Admin always receives the exact remainder: ₹10 - (Ancestors actually found in the 9-level window).
     * - Starting from Level 10, Admin always receives a fixed ₹1 (as 9 ancestors will always be present).
     * - Example: Level 5 node has 4 ancestors. Payout = ₹4. Admin share = ₹6.
     * - Example: Level 10 node has 9 ancestors. Payout = ₹9. Admin share = ₹1.
     * - Example: Level 11 node has 10 ancestors. We take nearest 9. Payout = ₹9. Admin share = ₹1.
     * - Deterministic: EVERY node in the window gets ₹1 regardless of active/inactive status.
     * - Root (Level 1) naturally stops receiving rewards once any node joins at Level 11 (10 levels away).
     */
    private static final int MAX_REWARD_WINDOW = 9;

    public RewardResult insertNode(Long userId) {

        // 1. Find parent
        RewardTreeNode parent = bfsCache.getNextParent()
                .orElseGet(() -> treeRepo.findNextAvailableParent().orElse(null));

        // 2. BFS Position (Node 1 = Level 1)
        long newBfsPos = treeRepo.findMaxBfsPosition().map(pos -> pos + 1).orElse(0L);
        int depthLevel = (parent == null) ? 1 : parent.getDepthLevel() + 1;

        // 3. Create Node
        RewardTreeNode newNode = RewardTreeNode.builder()
                .userId(userId)
                .parentNodeId(parent != null ? parent.getNodeId() : null)
                .depthLevel(depthLevel)
                .bfsPosition(newBfsPos)
                .build();

        newNode = repository.save(newNode);
        
        // Fetch uplines (1 level for Uber-like system: ₹1 to parent, ₹9 to admin)
        List<UplineRecord> uplines = fetchUplines(newNode, 1);
        
        // Publish event
        RewardEvent event = RewardEvent.builder()
                .triggeringNodeId(newNode.getNodeId().toString())
                .uplines(uplines)
                .build();
        
        rabbitTemplate.convertAndSend("reward.queue", event);
    }

    private List<UplineRecord> fetchUplines(RewardTreeNode node, int levels) {
        List<UplineRecord> uplines = new ArrayList<>();
        RewardTreeNode current = node;
        for (int i = 1; i <= levels; i++) {
            if (current.getParentNodeId() == null) break;
            Optional<RewardTreeNode> optionalParent = repository.findById(current.getParentNodeId());
            if (optionalParent.isPresent()) {
                current = optionalParent.get();
                uplines.add(UplineRecord.builder()
                        .nodeId(current.getNodeId().toString())
                        .userId(current.getUserId().toString())
                        .level(i)
                        .build());
            } else {
                // Parent still has space, keep it in cache
                bfsCache.setParent(parent.getNodeId());
            }
        } else {
            // New node is the root or first child of some node
            bfsCache.invalidate();
        }

        // 5. Sliding Window: Always fetch nearest 9 ancestors (or all of them if fewer exist)
        List<UplineRecord> uplines = fetchUplinesRecursive(newNode.getNodeId(), MAX_REWARD_WINDOW);

        eventPublisher.publishRewardEvent(newNode.getNodeId(), uplines);

        log.info("Node inserted: bfsPos={}, depth={}, paidUplines={}",
                newBfsPos, depthLevel, uplines.size());

        // Construct required result structure: Always total 10. Admin gets whatever is not paid to uplines.
        double adminAmount = 10.0 - uplines.size();
        return RewardResult.builder()
                .uplines(uplines.stream()
                        .map(u -> new RewardResult.UplinePayout(u.getUserId().toString(), 1.0))
                        .toList())
                .adminAmount(adminAmount)
                .total(10.0)
                .build();
    }

    private List<UplineRecord> fetchUplinesRecursive(Long nodeId, int maxLevels) {

        // Strictly walk distance (maxLevels) up the hierarchy. 
        // No filtering for active/inactive nodes - they are treated equally for distribution.
        String sql = """
            WITH RECURSIVE ancestors AS (
                SELECT
                    p.node_id AS node_id,
                    1 AS lvl,
                    p.parent_node_id AS next_parent_id
                FROM reward_tree_nodes c
                JOIN reward_tree_nodes p ON p.node_id = c.parent_node_id
                WHERE c.node_id = :nodeId
            
                UNION ALL
            
                SELECT
                    p.node_id AS node_id,
                    u.lvl + 1 AS lvl,
                    p.parent_node_id AS next_parent_id
                FROM ancestors u
                JOIN reward_tree_nodes p ON p.node_id = u.next_parent_id
                WHERE u.next_parent_id IS NOT NULL AND u.lvl < :maxLvl
            )
            SELECT u.node_id, u.lvl, n.user_id, n.active
            FROM ancestors u
            JOIN reward_tree_nodes n ON n.node_id = u.node_id
            ORDER BY u.lvl ASC
        """;

        return em.createNativeQuery(sql, Tuple.class)
                .setParameter("nodeId", nodeId)
                .setParameter("maxLvl", maxLevels)
                .getResultList()
                .stream()
                .map(t -> new UplineRecord(
                        ((Tuple) t).get(0, Long.class),
                        ((Tuple) t).get(2, Long.class),
                        ((Tuple) t).get(1, Integer.class),
                        ((Tuple) t).get(3, Boolean.class)
                ))
                .toList();
    }
}
