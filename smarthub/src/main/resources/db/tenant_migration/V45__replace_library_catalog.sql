-- Clear existing clinical library data so TenantLibrarySeedService can load legacy ClientHub defaults.
TRUNCATE library_entry_tags, library_entry_connections, library_entries, library_tags, library_categories
    RESTART IDENTITY CASCADE;
