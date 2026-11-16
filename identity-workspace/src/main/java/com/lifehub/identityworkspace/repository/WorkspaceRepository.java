package com.lifehub.identityworkspace.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lifehub.identityworkspace.model.entity.Workspace;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
}
