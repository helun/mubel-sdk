CREATE TABLE IF NOT EXISTS subscription_state (
    consumer_group VARCHAR(255) NOT NULL,
    sequence_no BIGINT NOT NULL,
    version INT NOT NULL,
    PRIMARY KEY (consumer_group)
    );
