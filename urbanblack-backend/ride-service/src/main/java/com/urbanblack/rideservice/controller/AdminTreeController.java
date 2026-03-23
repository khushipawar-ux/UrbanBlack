package com.urbanblack.rideservice.controller;

import com.urbanblack.rideservice.repository.RewardTreeRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/tree")
@RequiredArgsConstructor
public class AdminTreeController {

    private final RewardTreeRepository treeRepo;

    @GetMapping("/stats")
    public ResponseEntity<TreeStatsResponse> getStats() {
        return ResponseEntity.ok(TreeStatsResponse.builder()
                .totalUsers(treeRepo.count())
                .maxDepth(treeRepo.findMaxDepth()) // I'll need to add this method to repo
                .build());
    }

    @PatchMapping("/users/{userId}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable Long userId) {
        java.util.List<com.urbanblack.rideservice.entity.RewardTreeNode> nodes = treeRepo.findAllByUserId(userId);
        if (nodes.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        for (com.urbanblack.rideservice.entity.RewardTreeNode node : nodes) {
            node.setActive(false);
            node.setDeactivatedAt(java.time.LocalDateTime.now());
        }
        treeRepo.saveAll(nodes);
        return ResponseEntity.ok().build();
    }

    @Data
    @Builder
    public static class TreeStatsResponse {
        private Long totalUsers;
        private Integer maxDepth;
    }
}
