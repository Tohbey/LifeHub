package com.lifehub.audit.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.lifehub.audit.model.dto.AuditEntryDTO;
import com.lifehub.starter.web.PageResponse;

/** Reading the audit log. Only owners and admins of the workspace may read it. */
public interface AuditService {

    /** Pages through the workspace's events, newest first, optionally by type and time range. */
    PageResponse<AuditEntryDTO> getEntries(UUID workspaceId, String type, Instant from, Instant to, Pageable pageable);
}
