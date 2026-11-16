package com.lifehub.identityworkspace.service;

import static com.lifehub.starter.membership.WorkspaceRole.ADMIN;
import static com.lifehub.starter.membership.WorkspaceRole.MEMBER;
import static com.lifehub.starter.membership.WorkspaceRole.OWNER;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.lifehub.starter.membership.WorkspaceRole;

class MemberPolicyTest {

    @ParameterizedTest(name = "{0} invites {1}: {2}")
    @CsvSource({
            "OWNER,  ADMIN,  true",
            "OWNER,  MEMBER, true",
            "OWNER,  OWNER,  false",
            "ADMIN,  MEMBER, true",
            "ADMIN,  ADMIN,  false",
            "MEMBER, MEMBER, false",
    })
    void invite(WorkspaceRole actor, WorkspaceRole invited, boolean allowed) {
        assertThat(MemberPolicy.invite(actor, invited).allowed()).isEqualTo(allowed);
    }

    @ParameterizedTest(name = "{0} sets someone else to {1}: {2}")
    @CsvSource({
            "OWNER,  ADMIN,  true",
            "OWNER,  OWNER,  true",
            "ADMIN,  MEMBER, false",
            "MEMBER, ADMIN,  false",
    })
    void changeOthersRole(WorkspaceRole actor, WorkspaceRole newRole, boolean allowed) {
        assertThat(MemberPolicy.changeRole(actor, false, newRole).allowed()).isEqualTo(allowed);
    }

    @ParameterizedTest(name = "{0} changes own role to {1}")
    @CsvSource({ "OWNER, MEMBER", "ADMIN, OWNER", "MEMBER, ADMIN" })
    void nobodyChangesTheirOwnRole(WorkspaceRole actor, WorkspaceRole newRole) {
        assertThat(MemberPolicy.changeRole(actor, true, newRole).allowed()).isFalse();
    }

    @ParameterizedTest(name = "{0} removes {1}: {2}")
    @CsvSource({
            "OWNER,  ADMIN,  true",
            "OWNER,  MEMBER, true",
            "ADMIN,  MEMBER, true",
            "ADMIN,  ADMIN,  false",
            "ADMIN,  OWNER,  false",
            "MEMBER, MEMBER, false",
    })
    void removeOthers(WorkspaceRole actor, WorkspaceRole target, boolean allowed) {
        assertThat(MemberPolicy.remove(actor, false, target).allowed()).isEqualTo(allowed);
    }

    @ParameterizedTest(name = "{0} leaves: {1}")
    @CsvSource({ "OWNER, false", "ADMIN, true", "MEMBER, true" })
    void leave(WorkspaceRole actor, boolean allowed) {
        assertThat(MemberPolicy.remove(actor, true, actor).allowed()).isEqualTo(allowed);
    }

    @org.junit.jupiter.api.Test
    void ownerCanNeverEndUpWithoutASuccessor() {
        // The only path away from OWNER is changeRole by the owner on someone else, which promotes them.
        assertThat(MemberPolicy.remove(OWNER, true, OWNER).allowed()).isFalse();
        assertThat(MemberPolicy.remove(ADMIN, false, OWNER).allowed()).isFalse();
        assertThat(MemberPolicy.changeRole(OWNER, true, MEMBER).allowed()).isFalse();
        assertThat(MemberPolicy.invite(OWNER, OWNER).allowed()).isFalse();
        assertThat(MemberPolicy.changeRole(ADMIN, false, OWNER).allowed()).isFalse();
        assertThat(MemberPolicy.remove(MEMBER, false, ADMIN).allowed()).isFalse();
    }
}
