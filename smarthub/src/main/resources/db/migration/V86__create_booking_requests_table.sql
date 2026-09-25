CREATE TABLE booking_requests (
    id BIGSERIAL PRIMARY KEY,
    createdat TIMESTAMP NOT NULL,
    updatedat TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    version BIGINT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NOT NULL,
    practice_name VARCHAR(255) NOT NULL,
    services_offered VARCHAR(255) NOT NULL,
    practice_size VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    agree_to_terms BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_booking_req_status ON booking_requests(status);
CREATE INDEX idx_booking_req_email ON booking_requests(email);
