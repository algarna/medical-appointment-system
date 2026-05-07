CREATE TABLE IF NOT EXISTS patients (
    id                  BIGSERIAL           NOT NULL,
    first_name          VARCHAR(100)        NOT NULL,
    last_name           VARCHAR(100)        NOT NULL,
    identity_number     VARCHAR(20)         NOT NULL,
    email               VARCHAR(255)        NOT NULL,
    phone               VARCHAR(20),
    date_of_birth       DATE,
    active              BOOLEAN             NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP           NOT NULL,
    updated_at          TIMESTAMP           NOT NULL,
    created_by          VARCHAR(100),
    last_modified_by    VARCHAR(100),
    deleted_at          TIMESTAMP,
    deleted_by          VARCHAR(100),

    CONSTRAINT pk_patients PRIMARY KEY (id),
    CONSTRAINT uq_patients_email UNIQUE (email),
    CONSTRAINT uq_patients_identity_number UNIQUE (identity_number)
    );

CREATE INDEX IF NOT EXISTS idx_patients_email ON patients (email);
CREATE INDEX IF NOT EXISTS idx_patients_identity_number ON patients (identity_number);
CREATE INDEX IF NOT EXISTS idx_patients_active ON patients (active);

COMMENT ON TABLE patients IS 'Stores patient demographic and contact information';
COMMENT ON COLUMN patients.active IS 'Soft delete flag — false means the patient has been deactivated';
COMMENT ON COLUMN patients.identity_number IS 'National identity document number, unique per patient';
COMMENT ON COLUMN patients.deleted_at IS 'Timestamp of soft deletion, null if active';
COMMENT ON COLUMN patients.deleted_by IS 'User who performed the soft deletion, null if active';
