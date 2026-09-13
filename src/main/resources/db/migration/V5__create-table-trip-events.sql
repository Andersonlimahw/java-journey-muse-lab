CREATE TABLE trip_events (
                                id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                                title VARCHAR(255) NOT NULL,
                                description VARCHAR(1024),
                                location VARCHAR(255),
                                starts_at TIMESTAMP NOT NULL,
                                ends_at TIMESTAMP NOT NULL,
                                trip_id UUID,
                                FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE
);
