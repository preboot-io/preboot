CREATE TABLE IF NOT EXISTS secure_documents
(
    id        BIGSERIAL PRIMARY KEY,
    tenant_id UUID         NOT NULL,
    title     VARCHAR(255) NOT NULL
);

-- Test data for tenant 1
INSERT INTO secure_documents (tenant_id, title)
VALUES ( '11111111-1111-1111-1111-111111111111', 'Tenant 1 Document 1'),
       ( '11111111-1111-1111-1111-111111111111', 'Tenant 1 Document 2');

-- Test data for tenant 2
INSERT INTO secure_documents (tenant_id, title)
VALUES ('22222222-2222-2222-2222-222222222222', 'Tenant 2 Document 1'),
       ('22222222-2222-2222-2222-222222222222', 'Tenant 2 Document 2');


CREATE TABLE IF NOT EXISTS secure_notes
(
    id        BIGSERIAL PRIMARY KEY,
    tenant_id UUID         NOT NULL,
    content   TEXT         NOT NULL
);

-- Test data for tenant 1
INSERT INTO secure_notes (tenant_id, content)
VALUES ('11111111-1111-1111-1111-111111111111', 'Tenant 1 Note 1'),
       ('11111111-1111-1111-1111-111111111111', 'Tenant 1 Note 2');

-- Test data for tenant 2
INSERT INTO secure_notes (tenant_id, content)
VALUES ('22222222-2222-2222-2222-222222222222', 'Tenant 2 Note 1'),
       ('22222222-2222-2222-2222-222222222222', 'Tenant 2 Note 2');

CREATE TABLE IF NOT EXISTS secure_uuid_documents
(
    id        BIGSERIAL PRIMARY KEY,
    uuid      UUID         NOT NULL,
    tenant_id UUID         NOT NULL,
    title     VARCHAR(255) NOT NULL
);

-- Test data for tenant 1
INSERT INTO secure_uuid_documents (uuid, tenant_id, title)
VALUES ('a1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'Tenant 1 UUID Document 1'),
       ('a2222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'Tenant 1 UUID Document 2');

-- Test data for tenant 2
INSERT INTO secure_uuid_documents (uuid, tenant_id, title)
VALUES ('b1111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'Tenant 2 UUID Document 1'),
       ('b2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'Tenant 2 UUID Document 2');
