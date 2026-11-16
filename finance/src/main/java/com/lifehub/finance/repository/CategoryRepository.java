package com.lifehub.finance.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lifehub.finance.model.entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /** Finds a category only if it belongs to this workspace. */
    Optional<Category> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    /** Pages through a workspace's categories. */
    Page<Category> findByWorkspaceId(UUID workspaceId, Pageable pageable);
}
