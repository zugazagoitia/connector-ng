-- connector 7.x uses XA transactions (Atomikos); MySQL 8 needs this privilege for XA recovery
GRANT XA_RECOVER_ADMIN ON *.* TO 'connector'@'%';
FLUSH PRIVILEGES;
