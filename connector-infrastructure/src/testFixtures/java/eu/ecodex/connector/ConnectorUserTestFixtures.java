/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector;


import eu.ecodex.connector.domain.model.user.ConnectorRole;
import eu.ecodex.connector.domain.model.user.ConnectorRoleName;
import eu.ecodex.connector.domain.model.user.ConnectorUser;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.user.ConnectorUserDto;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.login.ConnectorUpdateUserPasswordRequest;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.user.ConnectorCreateUserRequest;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.user.ConnectorEditSelfRequest;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.user.ConnectorEditUserRequest;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.user.ConnectorEditUserRolesRequest;
import eu.ecodex.connector.infrastructure.adapter.out.auth.identity.ConnectorUserDetails;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.user.ConnectorRoleEntity;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.user.ConnectorUserEntity;
import java.util.Set;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class ConnectorUserTestFixtures {
    public static ConnectorUser createDefaultUser() {
        return ConnectorUser.builder()
            .uuid("0ecd850c-3f8e-47a8-b95d-d56d336bb83a")
            .username("test_user")
            .email("test_user@email.com")
            .password("encoded")
            .enabled(true)
            .mustChangePassword(false)
            .build();
    }

    public static ConnectorUserEntity createDefaultUserEntity() {
        return ConnectorUserEntity.builder()
            .uuid("0ecd850c-3f8e-47a8-b95d-d56d336bb83a")
            .username("test_user")
            .email("test_user@email.com")
            .password("encoded")
            .enabled(true)
            .mustChangePassword(false)
            .build();
    }

    public static ConnectorUser createDefaultUserWithRoles() {
        return ConnectorUser.builder()
            .uuid("0ecd850c-3f8e-47a8-b95d-d56d336bb83a")
            .username("test_user")
            .email("test_user@email.com")
            .password("encoded")
            .enabled(true)
            .mustChangePassword(false)
            .roles(Set.of(createDefaultAdminRole()))
            .build();
    }

    public static ConnectorRole createDefaultAdminRole() {
        return ConnectorRole
            .builder()
            .name("ROLE_".concat(ConnectorRoleName.ADMIN.name()))
            .build();
    }

    public static ConnectorRoleEntity createDefaultAdminRoleEntity() {
        return ConnectorRoleEntity
            .builder()
            .name("ROLE_".concat(ConnectorRoleName.ADMIN.name()))
            .build();
    }

    public static ConnectorUserEntity createDefaultUserWithRolesEntity() {
        return ConnectorUserEntity.builder()
            .uuid("0ecd850c-3f8e-47a8-b95d-d56d336bb83a")
            .username("test_user")
            .email("test_user@email.com")
            .password("encoded")
            .enabled(true)
            .mustChangePassword(false)
            .roles(Set.of(createDefaultAdminRoleEntity()))
            .build();
    }

    public static ConnectorUserDto createUserDto() {
        return ConnectorUserDto.builder()
            .username("test_user")
            .uuid("0ecd850c-3f8e-47a8-b95d-d56d336bb83a")
            .email("test_user@email.com")
            .enabled(true)
            .mustChangePassword(false)
            .build();
    }

    public static ConnectorUserDto createUserDtoWithRoles() {
        return ConnectorUserDto.builder()
            .username("test_user")
            .uuid("0ecd850c-3f8e-47a8-b95d-d56d336bb83a")
            .email("test_user@email.com")
            .enabled(true)
            .mustChangePassword(false)
            .roles(Set.of("ROLE_ADMIN"))
            .build();
    }

    public static ConnectorCreateUserRequest createDefaultUserRequest() {
        return ConnectorCreateUserRequest.builder()
            .username("test_user")
            .email("test_user@email.com")
            .password("test_password")
            .enabled(true)
            .build();
    }

    public static ConnectorUpdateUserPasswordRequest createDefaultUpdateUserPasswordRequest() {
        return ConnectorUpdateUserPasswordRequest.builder()
            .refreshToken("refresh-token")
            .newPassword("test_password")
            .currentPassword("password")
            .build();
    }

    public static ConnectorUpdateUserPasswordRequest createDefaultUpdateUserShortPasswordRequest() {
        return ConnectorUpdateUserPasswordRequest.builder()
            .refreshToken("refresh-token")
            .newPassword("test")
            .currentPassword("password")
            .build();
    }

    public static ConnectorEditUserRequest createDefaultPatchUserRequest() {
        return ConnectorEditUserRequest.builder()
            .email("test_user@email.com")
            .enabled(Boolean.TRUE)
            .build();
    }

    public static ConnectorEditSelfRequest createDefaultPatchMeUserRequest() {
        return ConnectorEditSelfRequest.builder()
            .email("test_user_new@email.com")
            .build();
    }

    public static ConnectorEditUserRolesRequest createEditUserRolesRequest() {
        return ConnectorEditUserRolesRequest.builder()
            .roles(Set.of("ROLE_ADMIN", "ROLE_USER"))
            .build();
    }

    public static ConnectorEditUserRolesRequest createInvalidEditUserRolesRequest() {
        return ConnectorEditUserRolesRequest.builder()
            .roles(Set.of("INVALID_ROLE_ADMIN"))
            .build();
    }

    public static ConnectorCreateUserRequest createDefaultUserRequestWithRoles() {
        return ConnectorCreateUserRequest.builder()
            .username("test_user")
            .email("test_user@email.com")
            .password("test_password")
            .enabled(true)
            .roles(Set.of("ROLE_ADMIN"))
            .build();
    }

    public static ConnectorCreateUserRequest createUserRequest(String username,
                                                               String email,
                                                               String password) {
        return ConnectorCreateUserRequest.builder()
            .username(username)
            .email(email)
            .password(password)
            .enabled(true)
            .build();
    }

    public static ConnectorUserDetails createUserDetails() {
        return createUserDetails(null);
    }

    public static ConnectorUserDetails createUserDetails(String accessToken) {
        return new ConnectorUserDetails(createDefaultUserWithRoles(), accessToken);
    }
}

