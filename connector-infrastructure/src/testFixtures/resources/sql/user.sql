/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

INSERT IGNORE INTO connector_roles (id, uuid, name, created_at, updated_at)
VALUES (2, 'd43bfa931-3c25-47e4-b377-bf4ce7b0d04c_fake_role_test', 'ROLE_TEST', now(), now()),
       (3, '2c7f58331-be5f-463a-8b17-bf4ce7b0d04d_fake_role_load_testor', 'ROLE_LOAD_TESTER', now(), now()),
       (4, 'd43bfa931-3c25-47e4-b377-bf4ce7b0d04d_fake_role_admin', 'ROLE_ADMIN_IT', now(), now()),
       (5, 'd43bfa931-3c25-47e4-b377-bf4ce7b0d04d_fake_role_user', 'ROLE_USER', now(), now()),
       (6, 'd43bfa931-3c25-47e4-b377-bf4ce7b0d04d_fake_role_user_2', 'ROLE_USER_2', now(), now()),
       (1, 'd43bfa931-3c25-47e4-b377-bf4ce7b0d04c_default_role_admin', 'ROLE_ADMIN', now(), now())
ON DUPLICATE KEY UPDATE name = VALUES(name),
                        uuid = VALUES(uuid);

INSERT INTO connector_users (id, uuid, username, password, email, enabled, must_change_password, created_at, updated_at)
VALUES (2, 'd43bfa931-3c25-47e4-b377-bf4ce7b0d04c_fake_user_admin', 'test-admin-it', '$2a$12$pwdAdminIt', 'admin-it@email.com', true, false, now(), now()),
       (3, 'd43bfa931-3c25-47e4-b377-bf4ce7b0d04c_fake_user_test', 'test-user-it','$2a$12$Z4jT8Cvg/CcmNxJ3aNXeleQ/upt3LIla4e2mghCvUyvhj9P3BpSjS', 'user-it@email.com', true, false,now(), now()),
       (4, 'd43bfa931-3c25-47e4-b377-bf4ce7b0d04c_fake_user_test2', 'test-user2-it','$2a$12$pwdUser2It', 'user2-it@email.com', true, false, now(), now()),
       (5, 'd43bfa931-3c25-47e4-b377-bf4ce7b0d04c_fake_user_test3', 'test-user3-it','$2b$12$pyjrPNksvg6iIZzDoouZF.hH5ZdFTRgl8wZT4d9iOc0sF5XVc2EPS', 'user3-it@email.com', true, true,now(), now()),
       (1, 'd43bfa931-3c25-47e4-b377-bf4ce7b0d04c_default_admin', 'admin', '$2a$12$5jl8Wz2LwcMLc7.yarZT8eOnWtu4tz1IvScxxSPZyGTtrKBTdTt3W', 'admin@example.org', true, false,now(), now())
ON DUPLICATE KEY UPDATE uuid     = VALUES(uuid),
                        username = VALUES(username),
                        password = VALUES(password),
                        email    = VALUES(email),
                        enabled  = VALUES(enabled);

INSERT IGNORE INTO connector_users_roles(user_id, role_id)
VALUES (1, 1),
       (2, 1),
       (2, 5),
       (3, 2),
       (3, 6);

INSERT IGNORE INTO connector_refresh_tokens(id, token, user_id, expires_at, revoked, revoked_at, created_at, updated_at)
VALUES(1,SHA2('test-refresh-token-1', 256), 1, '2026-08-01 15:37:03.160645', true, now(),now(), now()),
      (2,SHA2('test-refresh-token-2', 256), 1, '2026-08-03 15:37:03.160645', true, now(), now(), now()),
      (3,SHA2('test-refresh-token-3', 256), 1, '2026-08-04 15:37:03.160645', true, now(), now(), now()),
      (4,SHA2('test-refresh-token-4', 256), 1, '2026-08-18 10:30:03.160645', false, null,now(), now()),
      (5,SHA2('test-refresh-token-5', 256), 3, '2026-10-01 15:37:03.160645', true, now(),now(), now()),
      (6,SHA2('test-refresh-token-6', 256), 3, '2026-10-10 15:37:03.160645', false, null, now(), now());
