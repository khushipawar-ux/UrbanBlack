package com.urbanblack.rideservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "reward_tree_nodes",
        indexes = {
                @Index(name = "idx_reward_tree_bfs", columnList = "bfs_position"),
                @Index(name = "idx_reward_tree_parent", columnList = "parent_node_id"),
                @Index(name = "idx_reward_tree_user", columnList = "user_id"),
                @Index(name = "idx_reward_tree_active_depth", columnList = "active,depth_level")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardTreeNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long nodeId;

    @Column(nullable = false)
    private Long userId;

    @Column(name = "parent_node_id")
    private Long parentNodeId;

    @Column(name = "left_child_id")
    private Long leftChildId;

    @Column(name = "right_child_id")
    private Long rightChildId;

    @Builder.Default
    @Column(name = "children_count")
    private Integer childrenCount = 0;

    @Builder.Default
    @Column(name = "depth_level")
    private Integer depthLevel = 0;

    @Column(name = "bfs_position", unique = true, nullable = false)
    private Long bfsPosition;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;
}
