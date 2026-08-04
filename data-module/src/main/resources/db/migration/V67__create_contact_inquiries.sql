CREATE TABLE contact_inquiries (
    id BIGSERIAL PRIMARY KEY,
    inquiry_type VARCHAR(30) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    company VARCHAR(150),
    phone VARCHAR(30),
    subject VARCHAR(200),
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    reviewed_by VARCHAR(100),
    reviewed_date TIMESTAMP,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_contact_inquiries_type CHECK (inquiry_type IN ('CONTACT', 'DEMO_REQUEST')),
    CONSTRAINT chk_contact_inquiries_status CHECK (status IN ('NEW', 'REVIEWED', 'CLOSED'))
);

CREATE INDEX idx_contact_inquiries_status ON contact_inquiries(status);
CREATE INDEX idx_contact_inquiries_created_date ON contact_inquiries(created_date DESC);
