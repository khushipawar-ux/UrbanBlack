package com.urbanblack.rideservice.controller;

import com.urbanblack.rideservice.entity.RewardTreeNode;
import com.urbanblack.rideservice.repository.RewardTreeRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tree")
@RequiredArgsConstructor
public class TreeController {

    private final RewardTreeRepository treeRepo;

    @GetMapping("/me")
    public ResponseEntity<TreeInfoResponse> getMyTreeInfo(@RequestParam Long userId) {
        return treeRepo.findByUserId(userId)
                .map(node -> ResponseEntity.ok(TreeInfoResponse.builder()
                        .userId(node.getUserId())
                        .bfsPosition(node.getBfsPosition())
                        .depthLevel(node.getDepthLevel())
                        .parentUserId(resolveParentUserId(node))
                        .childrenCount(node.getChildrenCount())
                        .build()))
                .orElse(ResponseEntity.notFound().build());
    }

    private Long resolveParentUserId(RewardTreeNode node) {
        if (node.getParentNodeId() == null) return null;
        return treeRepo.findById(node.getParentNodeId())
                .map(RewardTreeNode::getUserId)
                .orElse(null);
    }

    @Data
    @Builder
    public static class TreeInfoResponse {
        private Long userId;
        private Long bfsPosition;
        private Integer depthLevel;
        private Long parentUserId;
        private Integer childrenCount;
    }
}
