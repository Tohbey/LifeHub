package com.lifehub.shoppingmeals.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lifehub.shoppingmeals.model.entity.Recipe;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, UUID> {

    /** Finds a recipe only if it belongs to this workspace. */
    Optional<Recipe> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    /** Pages through a workspace's recipes. */
    Page<Recipe> findByWorkspaceId(UUID workspaceId, Pageable pageable);

    /** Pages through a workspace's recipes whose name contains the text, ignoring case. */
    Page<Recipe> findByWorkspaceIdAndNameContainingIgnoreCase(UUID workspaceId, String name, Pageable pageable);
}
