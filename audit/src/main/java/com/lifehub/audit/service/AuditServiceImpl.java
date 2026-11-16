package com.lifehub.audit.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.lifehub.audit.model.dto.AuditEntryDTO;
import com.lifehub.audit.repository.AuditLogRepository;
import com.lifehub.starter.events.EventReader;
import com.lifehub.starter.membership.WorkspaceAccess;
import com.lifehub.starter.membership.WorkspaceRole;
import com.lifehub.starter.web.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class AuditServiceImpl implements AuditService {

    private final WorkspaceAccess access;
    private final AuditLogRepository auditLogs;
    private final EventReader reader;

    /** Requires owner or admin, then reads one page and returns each event's payload. */
    @Override
    public PageResponse<AuditEntryDTO> getEntries(UUID workspaceId, String type, Instant from, Instant to,
            Pageable pageable) {
        access.require(workspaceId, WorkspaceRole.ADMIN);
        List<AuditEntryDTO> entries = auditLogs.find(workspaceId, type, from, to, pageable.getPageSize(), pageable.getOffset())
                .stream()
                .map(row -> new AuditEntryDTO(row.eventId(), row.type(), row.version(), row.occurredAt(), row.actorUserId(),
                        reader.read(row.envelope()).payload()))
                .toList();
        long total = auditLogs.count(workspaceId, type, from, to);
        return PageResponse.of(new PageImpl<>(entries, pageable, total), entry -> entry);
    }
}
