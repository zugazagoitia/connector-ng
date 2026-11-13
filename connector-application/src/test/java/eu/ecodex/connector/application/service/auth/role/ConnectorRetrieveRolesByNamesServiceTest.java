/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.application.service.auth.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.application.port.spi.auth.role.ConnectorRoleRepository;
import eu.ecodex.connector.domain.model.user.ConnectorRole;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConnectorRetrieveRolesByNamesServiceTest {
    @Mock
    private ConnectorRoleRepository roleRepository;

    @InjectMocks
    private ConnectorRetrieveRolesByNamesService rolesByNamesService;

    @Test
    void find_roles_by_names_should_return_role_found() {
        // Given
        var identifier = "uuid";
        var roleName = "ROLE_NAME";
        var expected = ConnectorRole.builder()
            .uuid(identifier)
            .name(roleName)
            .build();

        when(roleRepository.findByNameIn(any())).thenReturn(Set.of(expected));

        // When
        var found = rolesByNamesService.execute(Set.of(roleName));

        // Then
        assertThat(found).isNotEmpty().containsOnlyOnce(expected);

        verify(roleRepository).findByNameIn(Set.of(roleName));
        verifyNoMoreInteractions(roleRepository);
    }

    @Test
    void find_roles_by_names_should_return_empty_when_no_role_found() {
        // Given
        var roleName = "ROLE_NAME";
        when(roleRepository.findByNameIn(any())).thenReturn(Set.of());

        // When
        var found = rolesByNamesService.execute(Set.of(roleName));

        // Then
        assertThat(found).isEmpty();
        verify(roleRepository).findByNameIn(Set.of(roleName));
        verifyNoMoreInteractions(roleRepository);
    }
}
