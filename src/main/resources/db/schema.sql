CREATE TABLE IF NOT EXISTS app_users (
    id VARCHAR(36) PRIMARY KEY,
    phone VARCHAR(32) NOT NULL UNIQUE,
    display_name VARCHAR(128) NOT NULL,
    avatar_url VARCHAR(512) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS auth_sessions (
    token VARCHAR(512) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS friend_relations (
    id VARCHAR(36) PRIMARY KEY,
    first_user_id VARCHAR(36) NOT NULL,
    second_user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_friend_pair UNIQUE (first_user_id, second_user_id)
);

CREATE TABLE IF NOT EXISTS friend_requests (
    id VARCHAR(36) PRIMARY KEY,
    requester_user_id VARCHAR(36) NOT NULL,
    receiver_user_id VARCHAR(36) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    responded_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS friend_remarks (
    user_id VARCHAR(36) NOT NULL,
    friend_user_id VARCHAR(36) NOT NULL,
    remark VARCHAR(128) NOT NULL,
    PRIMARY KEY (user_id, friend_user_id)
);

CREATE TABLE IF NOT EXISTS system_messages (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    message_type VARCHAR(64) NOT NULL,
    title VARCHAR(256) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    is_read BOOLEAN NOT NULL,
    payload_json CLOB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    read_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS membership_status (
    user_id VARCHAR(36) PRIMARY KEY,
    expires_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS membership_orders (
    order_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(128) NOT NULL,
    payment_provider VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    price_cents BIGINT NOT NULL,
    currency VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NULL,
    transaction_id VARCHAR(256) NULL UNIQUE,
    paid_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS tracks (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    track_date DATE NOT NULL,
    points_json CLOB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_track_user_date UNIQUE (user_id, track_date)
);

CREATE TABLE IF NOT EXISTS places (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(256) NOT NULL,
    category VARCHAR(128) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    description VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
