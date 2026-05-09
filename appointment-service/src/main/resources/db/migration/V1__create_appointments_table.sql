CREATE TABLE IF NOT EXISTS appointments (
    id                    BIGSERIAL           NOT NULL,
    patient_id            BIGINT              NOT NULL,
    doctor_name           VARCHAR(100)        NOT NULL,
    specialty             VARCHAR(100)        NOT NULL,
    appointment_date      TIMESTAMP           NOT NULL,
    status                VARCHAR(20)         NOT NULL DEFAULT 'SCHEDULED',
    reason                VARCHAR(500),
    cancellation_reason   VARCHAR(500),
    active                BOOLEAN             NOT NULL DEFAULT TRUE,
    version               BIGINT              NOT NULL DEFAULT 0,
    created_at            TIMESTAMP           NOT NULL,
    updated_at            TIMESTAMP           NOT NULL,
    created_by            VARCHAR(100),
    last_modified_by      VARCHAR(100),
    cancelled_at          TIMESTAMP,
    cancelled_by          VARCHAR(100),

    CONSTRAINT pk_appointments PRIMARY KEY (id),
    CONSTRAINT chk_appointments_status CHECK (status IN ('SCHEDULED', 'CANCELLED', 'COMPLETED'))
    );

CREATE INDEX IF NOT EXISTS idx_appointments_patient_id ON appointments (patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_status ON appointments (status);
CREATE INDEX IF NOT EXISTS idx_appointments_active ON appointments (active);
CREATE INDEX IF NOT EXISTS idx_appointments_appointment_date ON appointments (appointment_date);

COMMENT ON TABLE appointments IS 'Stores medical appointment records';
COMMENT ON COLUMN appointments.patient_id IS 'References patient in patient-service — no FK across services';
COMMENT ON COLUMN appointments.status IS 'SCHEDULED, CANCELLED or COMPLETED';
COMMENT ON COLUMN appointments.version IS 'Optimistic locking version field';
COMMENT ON COLUMN appointments.active IS 'Soft delete flag';
COMMENT ON COLUMN appointments.cancellation_reason IS 'Required when status is CANCELLED';
