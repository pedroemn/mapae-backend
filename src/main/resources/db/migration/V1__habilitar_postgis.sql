-- O PostGIS fica no schema extensions para nao expor spatial_ref_sys no public (AD-017).
CREATE SCHEMA IF NOT EXISTS extensions;
CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA extensions;
