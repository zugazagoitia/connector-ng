/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.persistence.adapter.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.domain.model.auth.ConnectorRefreshToken;
import eu.ecodex.connector.domain.model.user.ConnectorUser;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.user.ConnectorRefreshTokenEntity;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.user.ConnectorUserEntity;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.repository.auth.ConnectorUserJpaRepository;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.repository.auth.ConnectorUserRefreshTokenJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("ALL")
@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorDBRefreshTokenRepository")
class ConnectorDBRefreshTokenRepositoryTest {
    @Mock
    private ConnectorUserRefreshTokenJpaRepository refreshTokenJpaRepository;
    @Mock
    private ConnectorUserJpaRepository userJpaRepository;

    @InjectMocks
    private ConnectorDBRefreshTokenRepository repository;

    private static ConnectorRefreshToken getExpectedRefreshToken() {
        var user = ConnectorUser.builder()
                                .uuid("userId")
                                .enabled(Boolean.FALSE)
                                .mustChangePassword(Boolean.FALSE)
                                .roles(Set.of()).build();
        return ConnectorRefreshToken.builder().user(user).build();
    }

    private static ConnectorRefreshTokenEntity getRefreshTokenEntity() {
        var userEntity = ConnectorUserEntity.builder().uuid("userId").build();
        return ConnectorRefreshTokenEntity.builder()
                                          .user(userEntity).build();
    }

    @Nested
    @DisplayName("delete by user uuid")
    class DeleteByUserUuid {
        @Test
        void should_delete_all_the_refresh_tokens_of_the_user() {
            // Given
            when(refreshTokenJpaRepository.deleteByUser_Uuid(any())).thenReturn(Integer.MIN_VALUE);

            // When
            var deleted = repository.deleteByUserUuid("userId");

            // Then
            assertThat(deleted).isEqualTo(Integer.MIN_VALUE);

            verify(refreshTokenJpaRepository).deleteByUser_Uuid("userId");
            verifyNoMoreInteractions(refreshTokenJpaRepository, userJpaRepository);
        }

        @Test
        void should_fail_when_the_uuid_is_null() {
            assertThrows(NullPointerException.class, () -> repository.deleteByUserUuid(null));

            verifyNoInteractions(refreshTokenJpaRepository, userJpaRepository);
        }
    }

    @Nested
    @DisplayName("delete by expiry date before")
    class DeleteByExpiryDateBefore {
        @Test
        void should_delete_all_the_refresh_tokens_before_a_specified_date() {
            // Given
            Instant now = Instant.now();
            when(refreshTokenJpaRepository.deleteByExpiresAtBefore(any())).thenReturn(
                Integer.MIN_VALUE);

            // When
            var deleted = repository.deleteByExpiryDateBefore(now);

            // Then
            assertThat(deleted).isEqualTo(Integer.MIN_VALUE);

            verify(refreshTokenJpaRepository).deleteByExpiresAtBefore(now);
            verifyNoMoreInteractions(refreshTokenJpaRepository, userJpaRepository);
        }

        @Test
        void should_fail_when_the_expiry_date_is_null() {
            assertThrows(NullPointerException.class, () -> repository.deleteByExpiryDateBefore(null));

            verifyNoInteractions(refreshTokenJpaRepository, userJpaRepository);
        }
    }

    @Nested
    @DisplayName("delete by revoked and expiry date before")
    class DeleteByRevokedAndExpiryDateBefore {
        @Test
        void should_delete_all_the_revoked_refresh_tokens_before_a_specified_date() {
            // Given
            Instant now = Instant.now();
            when(refreshTokenJpaRepository.deleteByRevokedTrueAndExpiresAtBefore(any())).thenReturn(
                Integer.MIN_VALUE);

            // When
            var deleted = repository.deleteByRevokedAndExpiryDateBefore(now);

            // Then
            assertThat(deleted).isEqualTo(Integer.MIN_VALUE);

            verify(refreshTokenJpaRepository).deleteByRevokedTrueAndExpiresAtBefore(now);
            verifyNoMoreInteractions(refreshTokenJpaRepository, userJpaRepository);
        }

        @Test
        void should_fail_when_the_expiry_date_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.deleteByRevokedAndExpiryDateBefore(null)
            );

            verifyNoInteractions(refreshTokenJpaRepository, userJpaRepository);
        }
    }

    @Nested
    @DisplayName("find by token hashed")
    class FindByTokenHashed {
        @Test
        void should_return_the_refresh_token() {
            // Given
            var expected = getExpectedRefreshToken();
            var expectedEntity = Optional.of(getRefreshTokenEntity());
            when(refreshTokenJpaRepository.findByToken(any())).thenReturn(expectedEntity);

            // When
            var found = repository.findByTokenHashed("token");

            // Then
            assertThat(found).isPresent().contains(expected);
            verify(refreshTokenJpaRepository).findByToken("token");
            verifyNoMoreInteractions(refreshTokenJpaRepository, userJpaRepository);
        }

        @Test
        void should_map_all_the_fields_of_the_entity() {
            // Given
            var expiresAt = Instant.parse("2030-01-01T00:00:00Z");
            var revokedAt = Instant.parse("2029-01-01T00:00:00Z");
            var entity = ConnectorRefreshTokenEntity
                .builder()
                .token("hashed-token")
                .revoked(true)
                .expiresAt(expiresAt)
                .revokedAt(revokedAt)
                .user(ConnectorUserEntity.builder().uuid("userId").build())
                .build();
            when(refreshTokenJpaRepository.findByToken(any())).thenReturn(Optional.of(entity));

            // When
            var found = repository.findByTokenHashed("hashed-token");

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().token()).isEqualTo("hashed-token");
            assertThat(found.get().revoked()).isTrue();
            assertThat(found.get().expiresAt()).isEqualTo(expiresAt);
            assertThat(found.get().revokedAt()).isEqualTo(revokedAt);
            assertThat(found.get().user().uuid()).isEqualTo("userId");
        }

        @Test
        void should_return_empty_when_the_token_is_unknown() {
            // Given
            when(refreshTokenJpaRepository.findByToken(any())).thenReturn(Optional.empty());

            // When
            var found = repository.findByTokenHashed("unknown");

            // Then
            assertThat(found).isEmpty();
            verify(refreshTokenJpaRepository).findByToken("unknown");
            verifyNoMoreInteractions(refreshTokenJpaRepository, userJpaRepository);
        }

        @Test
        void should_fail_when_the_token_is_null() {
            assertThrows(NullPointerException.class, () -> repository.findByTokenHashed(null));

            verifyNoInteractions(refreshTokenJpaRepository, userJpaRepository);
        }
    }

    @Nested
    @DisplayName("save")
    class Save {
        @Test
        void should_save_the_refresh_token() {
            // Given
            var token = getExpectedRefreshToken();
            var expectedEntity = getRefreshTokenEntity();

            when(userJpaRepository.findByUuid(any())).thenReturn(
                Optional.of(expectedEntity.getUser()));
            when(refreshTokenJpaRepository.save(any())).thenReturn(expectedEntity);

            // When
            var found = repository.save(token);

            // Then
            assertThat(found).isNotNull().isEqualTo(token);

            var captor = ArgumentCaptor.forClass(ConnectorRefreshTokenEntity.class);
            verify(refreshTokenJpaRepository).save(captor.capture());
            assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(expectedEntity);

            verify(userJpaRepository).findByUuid(token.user().uuid());
            verifyNoMoreInteractions(refreshTokenJpaRepository, userJpaRepository);
        }

        @Test
        void should_map_all_the_token_fields_to_the_entity() {
            // Given
            var expiresAt = Instant.parse("2030-01-01T00:00:00Z");
            var revokedAt = Instant.parse("2029-01-01T00:00:00Z");
            var user = getExpectedRefreshToken().user();
            var token = ConnectorRefreshToken
                .builder()
                .token("hashed-token")
                .revoked(true)
                .expiresAt(expiresAt)
                .revokedAt(revokedAt)
                .user(user)
                .build();
            var userEntity = getRefreshTokenEntity().getUser();

            when(userJpaRepository.findByUuid(any())).thenReturn(Optional.of(userEntity));
            when(refreshTokenJpaRepository.save(any())).thenAnswer(
                invocation -> invocation.getArgument(0)
            );

            // When
            repository.save(token);

            // Then
            var captor = ArgumentCaptor.forClass(ConnectorRefreshTokenEntity.class);
            verify(refreshTokenJpaRepository).save(captor.capture());
            var saved = captor.getValue();
            assertThat(saved.getToken()).isEqualTo("hashed-token");
            assertThat(saved.isRevoked()).isTrue();
            assertThat(saved.getExpiresAt()).isEqualTo(expiresAt);
            assertThat(saved.getRevokedAt()).isEqualTo(revokedAt);
            assertThat(saved.getUser()).isSameAs(userEntity);
        }

        @Test
        void should_fail_when_the_user_is_unknown() {
            // Given
            var token = getExpectedRefreshToken();
            when(userJpaRepository.findByUuid(any())).thenReturn(Optional.empty());

            // When / Then
            assertThrows(NoSuchElementException.class, () -> repository.save(token));

            verify(userJpaRepository).findByUuid("userId");
            verify(refreshTokenJpaRepository, never()).save(any());
            verifyNoMoreInteractions(refreshTokenJpaRepository, userJpaRepository);
        }

        @Test
        void should_fail_when_the_refresh_token_is_null() {
            assertThrows(NullPointerException.class, () -> repository.save(null));

            verifyNoInteractions(refreshTokenJpaRepository, userJpaRepository);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {
        @Test
        void should_delete_the_refresh_token() {
            // Given
            var token = getExpectedRefreshToken();
            var expected = Optional.of(getRefreshTokenEntity());

            when(userJpaRepository.findByUuid(any())).thenReturn(
                Optional.of(expected.get().getUser()));
            doNothing().when(refreshTokenJpaRepository).delete(any());

            // When
            repository.delete(token);

            // Then
            var captor = ArgumentCaptor.forClass(ConnectorRefreshTokenEntity.class);
            verify(refreshTokenJpaRepository).delete(captor.capture());
            assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(expected.get());

            var userId = "userId";
            verify(userJpaRepository).findByUuid(userId);
            verifyNoMoreInteractions(refreshTokenJpaRepository, userJpaRepository);
        }

        @Test
        void should_fail_when_the_user_is_unknown() {
            // Given
            var token = getExpectedRefreshToken();
            when(userJpaRepository.findByUuid(any())).thenReturn(Optional.empty());

            // When / Then
            assertThrows(NoSuchElementException.class, () -> repository.delete(token));

            verify(userJpaRepository).findByUuid("userId");
            verify(refreshTokenJpaRepository, never()).delete(any());
            verifyNoMoreInteractions(refreshTokenJpaRepository, userJpaRepository);
        }

        @Test
        void should_fail_when_the_refresh_token_is_null() {
            assertThrows(NullPointerException.class, () -> repository.delete(null));

            verifyNoInteractions(refreshTokenJpaRepository, userJpaRepository);
        }
    }

    @Nested
    @DisplayName("find by user uuid and revoked")
    class FindByUserUuidAndRevoked {
        @Test
        void should_return_the_found_refresh_tokens() {
            // Given
            var userId = "uuid";
            var token = getExpectedRefreshToken();
            var expectedEntity = List.of(getRefreshTokenEntity());

            when(refreshTokenJpaRepository.findByUser_UuidAndRevoked(any(), anyBoolean()))
                .thenReturn(expectedEntity);

            // When
            var found = repository.findByUserUuidAndRevoked(userId, Boolean.TRUE);

            // Then
            assertThat(found).isNotEmpty().isEqualTo(List.of(token));
            verify(refreshTokenJpaRepository).findByUser_UuidAndRevoked(userId, Boolean.TRUE);
            verifyNoMoreInteractions(refreshTokenJpaRepository, userJpaRepository);
        }

        @Test
        void should_pass_the_revoked_flag_to_the_jpa_repository() {
            // Given
            when(refreshTokenJpaRepository.findByUser_UuidAndRevoked(any(), anyBoolean()))
                .thenReturn(List.of());

            // When
            repository.findByUserUuidAndRevoked("uuid", Boolean.FALSE);

            // Then
            verify(refreshTokenJpaRepository).findByUser_UuidAndRevoked("uuid", Boolean.FALSE);
            verifyNoMoreInteractions(refreshTokenJpaRepository, userJpaRepository);
        }

        @Test
        void should_return_an_empty_list_when_the_user_has_no_matching_tokens() {
            // Given
            when(refreshTokenJpaRepository.findByUser_UuidAndRevoked(any(), anyBoolean()))
                .thenReturn(List.of());

            // When
            var found = repository.findByUserUuidAndRevoked("uuid", Boolean.TRUE);

            // Then
            assertThat(found).isEmpty();
        }

        @Test
        void should_fail_when_the_uuid_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.findByUserUuidAndRevoked(null, Boolean.TRUE)
            );

            verifyNoInteractions(refreshTokenJpaRepository, userJpaRepository);
        }
    }

    @Nested
    @DisplayName("revoke by user uuid")
    class RevokeByUserUuid {
        @Test
        void should_revoke_all_the_active_refresh_tokens_of_the_user() {
            // Given
            var userId = "uuid";
            var revokedAt = Instant.now();
            when(refreshTokenJpaRepository.revokeAllActiveByUserId(any(), any()))
                .thenReturn(Integer.MIN_VALUE);

            // When
            var revoked = repository.revokeByUserUuid(userId, revokedAt);

            // Then
            assertThat(revoked).isEqualTo(Integer.MIN_VALUE);
            verify(refreshTokenJpaRepository).revokeAllActiveByUserId(userId, revokedAt);
            verifyNoMoreInteractions(refreshTokenJpaRepository, userJpaRepository);
        }

        @Test
        void should_fail_when_the_uuid_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.revokeByUserUuid(null, Instant.now())
            );

            verifyNoInteractions(refreshTokenJpaRepository, userJpaRepository);
        }

        @Test
        void should_fail_when_the_revocation_date_is_null() {
            assertThrows(
                NullPointerException.class,
                () -> repository.revokeByUserUuid("uuid", null)
            );

            verifyNoInteractions(refreshTokenJpaRepository, userJpaRepository);
        }
    }
}
