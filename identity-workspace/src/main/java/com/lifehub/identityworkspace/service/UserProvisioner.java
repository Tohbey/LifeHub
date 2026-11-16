package com.lifehub.identityworkspace.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.identityworkspace.model.event.IdentityEvents;
import com.lifehub.identityworkspace.model.entity.User;
import com.lifehub.identityworkspace.model.entity.Workspace;
import com.lifehub.identityworkspace.model.entity.WorkspaceMember;
import com.lifehub.identityworkspace.model.entity.WorkspaceType;
import com.lifehub.identityworkspace.repository.UserRepository;
import com.lifehub.identityworkspace.repository.WorkspaceMemberRepository;
import com.lifehub.identityworkspace.repository.WorkspaceRepository;
import com.lifehub.starter.events.DomainEventPublisher;
import com.lifehub.starter.id.Ids;
import com.lifehub.starter.membership.WorkspaceRole;

import lombok.RequiredArgsConstructor;

/** Creates a user, their personal workspace and their owner membership, in one new transaction. */
@Component
@RequiredArgsConstructor
class UserProvisioner {

    static final String PERSONAL_WORKSPACE_NAME = "Personal";

    private final UserRepository users;
    private final WorkspaceRepository workspaces;
    private final WorkspaceMemberRepository members;
    private final DomainEventPublisher events;
    private final Clock clock;

    /** Throws {@code DataIntegrityViolationException} if another request provisioned this subject first. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User provision(String logtoSubject) {
        Instant now = clock.instant();
        // Flush straight away so a concurrent first login fails here, on the unique subject, before anything else.
        User user = users.saveAndFlush(new User(Ids.newId(), logtoSubject, now));
        Workspace personal = workspaces.save(new Workspace(Ids.newId(), WorkspaceType.PERSONAL, PERSONAL_WORKSPACE_NAME, now));
        members.save(new WorkspaceMember(Ids.newId(), personal.getId(), user.getId(), WorkspaceRole.OWNER, now));
        events.publish(IdentityEvents.USER_CREATED, null, user.getId(),
                new IdentityEvents.UserCreated(user.getId(), personal.getId(), user.getDisplayName()),
                user.getId().toString());
        return user;
    }
}
