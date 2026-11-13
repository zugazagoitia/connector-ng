/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.persistence.adapter.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.ConnectorUserTestFixtures;
import eu.ecodex.connector.application.exception.user.ConnectorUserNotFoundException;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.entity.user.ConnectorUserEntity;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.repository.auth.ConnectorUserJpaRepository;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.repository.auth.ConnectorUserRefreshTokenJpaRepository;
import eu.ecodex.connector.infrastructure.adapter.out.persistence.db.repository.auth.ConnectorUserRoleJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConnectorDBUserRepositoryTest {
    @Mock
    private ConnectorUserJpaRepository jpaRepository;
    @Mock
    private ConnectorUserRoleJpaRepository roleJpaRepository;
    @Mock
    private ConnectorUserRefreshTokenJpaRepository refreshTokenJpaRepository;

    @InjectMocks
    private ConnectorDBUserRepository userRepository;

    private void assertNoMoreInteractions() {
        verifyNoMoreInteractions(
            jpaRepository,
            roleJpaRepository,
            refreshTokenJpaRepository
        );
    }

    @Nested
    class Save {
        @Test
        void should_create_a_new_user() {
            // Given
            var uuid = "0ecd850c-3f8e-47a8-b95d-d56d336bb83a";
            var user = ConnectorUserTestFixtures.createDefaultUserWithRoles();
            var userEntity = ConnectorUserTestFixtures.createDefaultUserWithRolesEntity();
            var roleEntity = ConnectorUserTestFixtures.createDefaultAdminRoleEntity();

            when(roleJpaRepository.findByNameIn(any())).thenReturn(Set.of(roleEntity));
            when(jpaRepository.findByUuid(any())).thenReturn(Optional.empty());
            when(jpaRepository.save(any())).thenReturn(userEntity);

            // When
            var actual = userRepository.save(user);

            // Then
            assertThat(actual)
                .isNotNull()
                .usingRecursiveComparison()
                .isEqualTo(user);

            verify(jpaRepository).findByUuid(uuid);
            verify(roleJpaRepository).findByNameIn(Set.of("ROLE_ADMIN"));

            var captor = ArgumentCaptor.forClass(ConnectorUserEntity.class);
            verify(jpaRepository).save(captor.capture());

            assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(userEntity);

            assertNoMoreInteractions();
        }

        @Test
        void should_update_an_existing_user() {
            // Given
            var uuid = "0ecd850c-3f8e-47a8-b95d-d56d336bb83a";
            var user = ConnectorUserTestFixtures.createDefaultUserWithRoles();
            var userEntity = ConnectorUserTestFixtures.createDefaultUserWithRolesEntity();
            var existingUserEntity = ConnectorUserTestFixtures.createDefaultUserEntity();
            var roleEntity = ConnectorUserTestFixtures.createDefaultAdminRoleEntity();

            when(roleJpaRepository.findByNameIn(any())).thenReturn(Set.of(roleEntity));
            when(jpaRepository.findByUuid(any())).thenReturn(Optional.of(existingUserEntity));
            when(jpaRepository.save(any())).thenReturn(userEntity);

            // When
            var actual = userRepository.save(user);

            // Then
            assertThat(actual)
                .isNotNull()
                .usingRecursiveComparison()
                .isEqualTo(user);

            verify(jpaRepository).findByUuid(uuid);
            verify(roleJpaRepository).findByNameIn(Set.of("ROLE_ADMIN"));

            var captor = ArgumentCaptor.forClass(ConnectorUserEntity.class);
            verify(jpaRepository).save(captor.capture());

            assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(userEntity);

            assertNoMoreInteractions();
        }
    }

    @Nested
    class FindByUuid {
        @Test
        void should_return_found_user() {
            // Given
            var uuid = "0ecd850c-3f8e-47a8-b95d-d56d336bb83a";
            var user = ConnectorUserTestFixtures.createDefaultUserWithRoles();
            var userEntity = ConnectorUserTestFixtures.createDefaultUserWithRolesEntity();

            when(jpaRepository.findByUuid(any())).thenReturn(Optional.of(userEntity));

            // When
            var actual = userRepository.findByUuid(uuid);

            // Then
            assertThat(actual).isPresent().contains(user);
            verify(jpaRepository).findByUuid(uuid);

            assertNoMoreInteractions();
        }

        @Test
        void should_return_empty_when_user_not_found() {
            // Given
            var uuid = "0ecd850c-3f8e-47a8-b95d-d56d336bb83a";

            when(jpaRepository.findByUuid(any())).thenReturn(Optional.empty());

            // When
            var actual = userRepository.findByUuid(uuid);

            // Then
            assertThat(actual).isEmpty();
            verify(jpaRepository).findByUuid(uuid);

            assertNoMoreInteractions();
        }
    }

    @Nested
    class FindByUsername {
        @Test
        void should_return_found_user() {
            // Given
            var username = "test_user";
            var user = ConnectorUserTestFixtures.createDefaultUserWithRoles();
            var userEntity = ConnectorUserTestFixtures.createDefaultUserWithRolesEntity();

            when(jpaRepository.findByUsername(any())).thenReturn(Optional.of(userEntity));

            // When
            var actual = userRepository.findByUsername(username);

            // Then
            assertThat(actual).isPresent().contains(user);
            verify(jpaRepository).findByUsername(username);

            assertNoMoreInteractions();
        }

        @Test
        void should_return_empty_when_user_not_found() {
            // Given
            var username = "test_user";

            when(jpaRepository.findByUsername(any())).thenReturn(Optional.empty());

            // When
            var actual = userRepository.findByUsername(username);

            // Then
            assertThat(actual).isEmpty();
            verify(jpaRepository).findByUsername(username);

            assertNoMoreInteractions();
        }
    }

    @Nested
    class FindByUsernameAndActive {
        @Test
        void should_return_found_user() {
            // Given
            var username = "test_user";
            var user = ConnectorUserTestFixtures.createDefaultUserWithRoles();
            var userEntity = ConnectorUserTestFixtures.createDefaultUserWithRolesEntity();

            when(jpaRepository.findByUsernameAndEnabled(any(), anyBoolean()))
                .thenReturn(Optional.of(userEntity));

            // When
            var actual = userRepository.findByUsernameAndActive(username, true);

            // Then
            assertThat(actual).isPresent().contains(user);
            verify(jpaRepository).findByUsernameAndEnabled(username, true);

            assertNoMoreInteractions();
        }

        @Test
        void should_return_empty_when_user_not_found() {
            // Given
            var username = "test_user";

            when(jpaRepository.findByUsernameAndEnabled(any(), anyBoolean()))
                .thenReturn(Optional.empty());

            // When
            var actual = userRepository.findByUsernameAndActive(username, true);

            // Then
            assertThat(actual).isEmpty();
            verify(jpaRepository).findByUsernameAndEnabled(username, true);

            assertNoMoreInteractions();
        }
    }

    @Nested
    class FindAllUsers {
        @Test
        void should_return_all_users() {
            // Given
            var user = ConnectorUserTestFixtures.createDefaultUserWithRoles();
            var userEntity = ConnectorUserTestFixtures.createDefaultUserWithRolesEntity();

            when(jpaRepository.findAll()).thenReturn(List.of(userEntity));

            // When
            var actual = userRepository.findAllUsers();

            // Then
            assertThat(actual).isNotEmpty().hasSize(1).contains(user);
            verify(jpaRepository).findAll();

            assertNoMoreInteractions();
        }
    }

    @Nested
    class DeleteByUuid {
        @Test
        void should_delete_user_when_found() {
            // Given
            var uuid = "0ecd850c-3f8e-47a8-b95d-d56d336bb83a";
            var userEntity = ConnectorUserTestFixtures.createDefaultUserWithRolesEntity();

            when(jpaRepository.findByUuid(any())).thenReturn(Optional.of(userEntity));
            when(refreshTokenJpaRepository.deleteByUser_Uuid(any())).thenReturn(1);

            // When
            userRepository.deleteByUuid(uuid);

            // Then
            verify(jpaRepository).findByUuid(uuid);
            verify(refreshTokenJpaRepository).deleteByUser_Uuid(uuid);
            verify(jpaRepository).delete(userEntity);

            assertNoMoreInteractions();
        }

        @Test
        void should_throw_exception_when_user_not_found() {
            // Given
            var uuid = "0ecd850c-3f8e-47a8-b95d-d56d336bb83a";

            when(jpaRepository.findByUuid(any())).thenReturn(Optional.empty());

            // When
            assertThrows(
                ConnectorUserNotFoundException.class,
                () -> userRepository.deleteByUuid(uuid)
            );

            // Then
            verify(jpaRepository).findByUuid(uuid);

            assertNoMoreInteractions();
        }
    }

    @Nested
    class ExistsByUuid {
        @Test
        void should_return_true_when_user_found() {
            // Given
            var uuid = "0ecd850c-3f8e-47a8-b95d-d56d336bb83a";

            when(jpaRepository.existsByUuid(any())).thenReturn(Boolean.TRUE);

            // When
            var actual = userRepository.existsByUuid(uuid);

            // Then
            assertThat(actual).isTrue();
            verify(jpaRepository).existsByUuid(uuid);

            assertNoMoreInteractions();
        }

        @Test
        void should_return_false_when_user_not_found() {
            // Given
            var uuid = "0ecd850c-3f8e-47a8-b95d-d56d336bb83a";

            when(jpaRepository.existsByUuid(any())).thenReturn(Boolean.FALSE);

            // When
            var actual = userRepository.existsByUuid(uuid);

            // Then
            assertThat(actual).isFalse();
            verify(jpaRepository).existsByUuid(uuid);

            assertNoMoreInteractions();
        }
    }

    @Nested
    class ExistsByUsername {
        @Test
        void should_return_true_when_user_found() {
            // Given
            var username = "test_user";

            when(jpaRepository.existsByUsername(any())).thenReturn(Boolean.TRUE);

            // When
            var actual = userRepository.existsByUsername(username);

            // Then
            assertThat(actual).isTrue();
            verify(jpaRepository).existsByUsername(username);

            assertNoMoreInteractions();
        }

        @Test
        void should_return_false_when_user_not_found() {
            // Given
            var username = "test_user";

            when(jpaRepository.existsByUsername(any())).thenReturn(Boolean.FALSE);

            // When
            var actual = userRepository.existsByUsername(username);

            // Then
            assertThat(actual).isFalse();
            verify(jpaRepository).existsByUsername(username);

            assertNoMoreInteractions();
        }
    }

    @Nested
    class ExistsByEmail {
        @Test
        void should_return_true_when_user_found() {
            // Given
            var email = "test_user@email.com";

            when(jpaRepository.existsByEmail(any())).thenReturn(Boolean.TRUE);

            // When
            var actual = userRepository.existsByEmail(email);

            // Then
            assertThat(actual).isTrue();
            verify(jpaRepository).existsByEmail(email);

            assertNoMoreInteractions();
        }

        @Test
        void should_return_false_when_user_not_found() {
            // Given
            var email = "test_user@email.com";

            when(jpaRepository.existsByEmail(any())).thenReturn(Boolean.FALSE);

            // When
            var actual = userRepository.existsByEmail(email);

            // Then
            assertThat(actual).isFalse();
            verify(jpaRepository).existsByEmail(email);

            assertNoMoreInteractions();
        }
    }

    @Nested
    class ExistsByUsernameAndUuidNot {
        @Test
        void should_return_true_when_user_found() {
            // Given
            var username = "test_user";
            var uuid = "uuid";

            when(jpaRepository.existsByUsernameAndUuidNot(any(), any()))
                .thenReturn(Boolean.TRUE);

            // When
            var actual = userRepository.existsByUsernameAndUuidNot(username, uuid);

            // Then
            assertThat(actual).isTrue();
            verify(jpaRepository).existsByUsernameAndUuidNot(username, uuid);

            assertNoMoreInteractions();
        }

        @Test
        void should_return_false_when_user_not_found() {
            // Given
            var username = "test_user";
            var uuid = "uuid";

            when(jpaRepository.existsByUsernameAndUuidNot(any(), any()))
                .thenReturn(Boolean.FALSE);

            // When
            var actual = userRepository.existsByUsernameAndUuidNot(username, uuid);

            // Then
            assertThat(actual).isFalse();
            verify(jpaRepository).existsByUsernameAndUuidNot(username, uuid);

            assertNoMoreInteractions();
        }
    }

    @Nested
    class ExistsByEmailAndUuidNot {
        @Test
        void should_return_true_when_user_found() {
            // Given
            var email = "test_user@email.com";
            var uuid = "uuid";

            when(jpaRepository.existsByEmailAndUuidNot(any(), any()))
                .thenReturn(Boolean.TRUE);

            // When
            var actual = userRepository.existsByEmailAndUuidNot(email, uuid);

            // Then
            assertThat(actual).isTrue();
            verify(jpaRepository).existsByEmailAndUuidNot(email, uuid);

            assertNoMoreInteractions();
        }

        @Test
        void should_return_false_when_user_not_found() {
            // Given
            var email = "test_user@email.com";
            var uuid = "uuid";

            when(jpaRepository.existsByEmailAndUuidNot(any(), any()))
                .thenReturn(Boolean.FALSE);

            // When
            var actual = userRepository.existsByEmailAndUuidNot(email, uuid);

            // Then
            assertThat(actual).isFalse();
            verify(jpaRepository).existsByEmailAndUuidNot(email, uuid);

            assertNoMoreInteractions();
        }
    }
}
