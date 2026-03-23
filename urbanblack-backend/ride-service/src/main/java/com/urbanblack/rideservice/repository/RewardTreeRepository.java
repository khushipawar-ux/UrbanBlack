package com.urbanblack.rideservice.repository;

import com.urbanblack.rideservice.entity.RewardTreeNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface RewardTreeRepository extends JpaRepository<RewardTreeNode, Long> {
    
    @Query("SELECT n FROM RewardTreeNode n WHERE n.bfsPosition = :pos")
    Optional<RewardTreeNode> findByBfsPosition(@Param("pos") Long pos);

    default Optional<RewardTreeNode> findRoot() {
        return findByBfsPosition(0L);
    }

    @Query("SELECT MAX(n.bfsPosition) FROM RewardTreeNode n")
    java.util.Optional<Long> findMaxBfsPosition();

    @Query("SELECT MAX(n.depthLevel) FROM RewardTreeNode n")
    Integer findMaxDepth();

    @Query("SELECT MAX(n.depthLevel) FROM RewardTreeNode n WHERE n.active = true")
    Integer findMaxActiveDepth();
    
    java.util.List<RewardTreeNode> findAllByUserId(Long userId);
    java.util.Optional<RewardTreeNode> findByUserId(Long userId); // keep for single lookups

    java.util.List<RewardTreeNode> findByParentNodeId(Long parentNodeId);

    java.util.List<RewardTreeNode> findByDepthLevelLessThanEqualOrderByBfsPositionAsc(Integer depthLevel);

    @Query("SELECT n FROM RewardTreeNode n WHERE n.childrenCount < 2 ORDER BY n.bfsPosition ASC LIMIT 1")
    Optional<RewardTreeNode> findNextAvailableParent();

    @Query(value = "WITH RECURSIVE uplines AS (" +
            "    SELECT node_id, user_id, parent_node_id, 1 as level " +
            "    FROM reward_tree_nodes " +
            "    WHERE node_id = :startNodeId " +
            "    UNION ALL " +
            "    SELECT t.node_id, t.user_id, t.parent_node_id, u.level + 1 " +
            "    FROM reward_tree_nodes t " +
            "    INNER JOIN uplines u ON t.node_id = u.parent_node_id " +
            "    WHERE u.level < :maxLevels" +
            ") " +
            "SELECT * FROM reward_tree_nodes " +
            "WHERE node_id IN (SELECT node_id FROM uplines WHERE node_id != :startNodeId)", nativeQuery = true)
    java.util.List<RewardTreeNode> findUplinesRecursive(@Param("startNodeId") Long startNodeId, @Param("maxLevels") int maxLevels);
}
