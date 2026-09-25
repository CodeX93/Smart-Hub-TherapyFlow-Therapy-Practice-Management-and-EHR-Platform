-- Clear existing system option catalog so TenantSystemOptionSeedService can load legacy ClientHub defaults.
DELETE FROM system_options;
DELETE FROM option_categories;
