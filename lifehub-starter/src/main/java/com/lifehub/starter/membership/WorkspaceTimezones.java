package com.lifehub.starter.membership;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The timezone a workspace lives in: the household's own, or {@code lifehub.membership.default-timezone} for a
 * personal workspace. Used for anything measured in local days or months, such as budget periods and recurring
 * events. Never fails: if identity-workspace cannot answer, the default is used and a warning is logged, because
 * a slightly wrong month boundary is better than refusing the request.
 */
@RequiredArgsConstructor
@Slf4j
public class WorkspaceTimezones {

    private final MembershipClient membershipClient;
    private final ZoneId defaultZone;

    /** The workspace's timezone, or the default. */
    public ZoneId zoneOf(UUID workspaceId) {
        try {
            return membershipClient.findWorkspace(workspaceId)
                    .map(InternalMembershipApi.WorkspaceInfoResponse::timezone)
                    .filter(id -> id != null && !id.isBlank())
                    .map(ZoneId::of)
                    .orElse(defaultZone);
        } catch (MembershipUnavailableException | DateTimeException ex) {
            log.warn("Using {} for workspace {}: its timezone could not be read", defaultZone, workspaceId, ex);
            return defaultZone;
        }
    }

    /** The zone used when a workspace has none. */
    public ZoneId defaultZone() {
        return defaultZone;
    }
}
