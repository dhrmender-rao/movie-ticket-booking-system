-- ============================================================
--  Movie Ticket Booking System — PostgreSQL Schema
--  Database : movieticket
--  Dialect  : PostgreSQL 14+
--
--  Usage:
--    psql -U postgres -c "CREATE DATABASE movieticket;"
--    psql -U postgres -d movieticket -f schema.sql
--
--  NOTE: The application seeds the admin user and default
--  refund policies automatically on first startup via
--  DataInitializer. The INSERT blocks below are provided
--  for manual / CI setup where the app may not run first.
-- ============================================================


-- ============================================================
--  DROP (reverse FK order for clean re-runs)
-- ============================================================
DROP TABLE IF EXISTS notifications    CASCADE;
DROP TABLE IF EXISTS payments         CASCADE;
DROP TABLE IF EXISTS booking_items    CASCADE;
DROP TABLE IF EXISTS bookings         CASCADE;
DROP TABLE IF EXISTS show_seats       CASCADE;
DROP TABLE IF EXISTS pricing_tiers    CASCADE;
DROP TABLE IF EXISTS discount_codes   CASCADE;
DROP TABLE IF EXISTS shows            CASCADE;
DROP TABLE IF EXISTS seats            CASCADE;
DROP TABLE IF EXISTS screens          CASCADE;
DROP TABLE IF EXISTS movies           CASCADE;
DROP TABLE IF EXISTS theaters         CASCADE;
DROP TABLE IF EXISTS cities           CASCADE;
DROP TABLE IF EXISTS refund_policies  CASCADE;
DROP TABLE IF EXISTS users            CASCADE;


-- ============================================================
--  ENUM-like CHECK constraints  (stored as VARCHAR in Hibernate)
-- ============================================================

-- ============================================================
--  users
-- ============================================================
CREATE TABLE users (
    id            BIGSERIAL       PRIMARY KEY,
    name          VARCHAR(255)    NOT NULL,
    email         VARCHAR(255)    NOT NULL UNIQUE,
    password_hash VARCHAR(255)    NOT NULL,
    role          VARCHAR(20)     NOT NULL CHECK (role IN ('ADMIN', 'CUSTOMER')),
    created_at    TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- ============================================================
--  cities
-- ============================================================
CREATE TABLE cities (
    id      BIGSERIAL    PRIMARY KEY,
    name    VARCHAR(255) NOT NULL,
    state   VARCHAR(255),
    country VARCHAR(255)
);

-- ============================================================
--  theaters
-- ============================================================
CREATE TABLE theaters (
    id      BIGSERIAL    PRIMARY KEY,
    name    VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    city_id BIGINT       NOT NULL REFERENCES cities(id) ON DELETE CASCADE
);

-- ============================================================
--  screens
-- ============================================================
CREATE TABLE screens (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    total_rows INT          NOT NULL CHECK (total_rows > 0),
    total_cols INT          NOT NULL CHECK (total_cols > 0),
    theater_id BIGINT       NOT NULL REFERENCES theaters(id) ON DELETE CASCADE
);

-- ============================================================
--  seats
-- ============================================================
CREATE TABLE seats (
    id         BIGSERIAL   PRIMARY KEY,
    row_label  VARCHAR(5)  NOT NULL,
    col_number INT         NOT NULL CHECK (col_number > 0),
    seat_type  VARCHAR(20) NOT NULL CHECK (seat_type IN ('REGULAR', 'PREMIUM')),
    screen_id  BIGINT      NOT NULL REFERENCES screens(id) ON DELETE CASCADE,
    UNIQUE (screen_id, row_label, col_number)
);

-- ============================================================
--  movies
-- ============================================================
CREATE TABLE movies (
    id               BIGSERIAL    PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    duration_minutes INT          NOT NULL CHECK (duration_minutes > 0),
    language         VARCHAR(100),
    genre            VARCHAR(100),
    rating           VARCHAR(20),
    description      TEXT
);

-- ============================================================
--  shows
-- ============================================================
CREATE TABLE shows (
    id         BIGSERIAL   PRIMARY KEY,
    movie_id   BIGINT      NOT NULL REFERENCES movies(id),
    screen_id  BIGINT      NOT NULL REFERENCES screens(id),
    start_time TIMESTAMP   NOT NULL,
    end_time   TIMESTAMP   NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                           CHECK (status IN ('ACTIVE', 'CANCELLED')),
    CHECK (end_time > start_time)
);

-- ============================================================
--  discount_codes
-- ============================================================
CREATE TABLE discount_codes (
    id            BIGSERIAL      PRIMARY KEY,
    code          VARCHAR(100)   NOT NULL UNIQUE,
    discount_type VARCHAR(20)    NOT NULL CHECK (discount_type IN ('PERCENTAGE', 'FLAT')),
    value         NUMERIC(10, 2) NOT NULL CHECK (value > 0),
    max_uses      INT            NOT NULL CHECK (max_uses > 0),
    used_count    INT            NOT NULL DEFAULT 0 CHECK (used_count >= 0),
    valid_from    TIMESTAMP,
    valid_to      TIMESTAMP,
    active        BOOLEAN        NOT NULL DEFAULT TRUE,
    version       BIGINT         NOT NULL DEFAULT 0,  -- optimistic locking
    CHECK (valid_to IS NULL OR valid_from IS NULL OR valid_to > valid_from)
);

-- ============================================================
--  show_seats
-- ============================================================
CREATE TABLE show_seats (
    id               BIGSERIAL   PRIMARY KEY,
    show_id          BIGINT      NOT NULL REFERENCES shows(id) ON DELETE CASCADE,
    seat_id          BIGINT      NOT NULL REFERENCES seats(id),
    status           VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
                                 CHECK (status IN ('AVAILABLE', 'HELD', 'BOOKED')),
    hold_expires_at  TIMESTAMP,
    held_by_user_id  BIGINT      REFERENCES users(id),
    version          BIGINT      NOT NULL DEFAULT 0,  -- optimistic locking
    UNIQUE (show_id, seat_id)
);

-- ============================================================
--  pricing_tiers
-- ============================================================
CREATE TABLE pricing_tiers (
    id         BIGSERIAL      PRIMARY KEY,
    show_id    BIGINT         NOT NULL REFERENCES shows(id) ON DELETE CASCADE,
    seat_type  VARCHAR(20)    NOT NULL CHECK (seat_type IN ('REGULAR', 'PREMIUM')),
    day_type   VARCHAR(20)    NOT NULL CHECK (day_type IN ('WEEKDAY', 'WEEKEND')),
    base_price NUMERIC(10, 2) NOT NULL CHECK (base_price >= 0),
    UNIQUE (show_id, seat_type, day_type)
);

-- ============================================================
--  bookings
-- ============================================================
CREATE TABLE bookings (
    id               BIGSERIAL      PRIMARY KEY,
    user_id          BIGINT         NOT NULL REFERENCES users(id),
    show_id          BIGINT         NOT NULL REFERENCES shows(id),
    discount_code_id BIGINT         REFERENCES discount_codes(id),
    total_amount     NUMERIC(10, 2) NOT NULL CHECK (total_amount >= 0),
    discount_amount  NUMERIC(10, 2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    final_amount     NUMERIC(10, 2) NOT NULL CHECK (final_amount >= 0),
    status           VARCHAR(20)    NOT NULL DEFAULT 'PENDING'
                                    CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW()
);

-- ============================================================
--  booking_items
-- ============================================================
CREATE TABLE booking_items (
    id               BIGSERIAL      PRIMARY KEY,
    booking_id       BIGINT         NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    show_seat_id     BIGINT         NOT NULL REFERENCES show_seats(id),
    price_at_booking NUMERIC(10, 2) NOT NULL CHECK (price_at_booking >= 0)
);

-- ============================================================
--  payments
-- ============================================================
CREATE TABLE payments (
    id             BIGSERIAL      PRIMARY KEY,
    booking_id     BIGINT         NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
    amount         NUMERIC(10, 2) NOT NULL CHECK (amount >= 0),
    status         VARCHAR(20)    NOT NULL DEFAULT 'PENDING'
                                  CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED')),
    payment_method VARCHAR(50),
    transaction_id VARCHAR(100),
    paid_at        TIMESTAMP,
    refunded_at    TIMESTAMP,
    refund_amount  NUMERIC(10, 2) CHECK (refund_amount >= 0)
);

-- ============================================================
--  refund_policies
-- ============================================================
CREATE TABLE refund_policies (
    id                 BIGSERIAL      PRIMARY KEY,
    hours_before_show  INT            NOT NULL CHECK (hours_before_show >= 0),
    refund_percentage  NUMERIC(5, 2)  NOT NULL CHECK (refund_percentage BETWEEN 0 AND 100),
    description        VARCHAR(500)
);

-- ============================================================
--  notifications
-- ============================================================
CREATE TABLE notifications (
    id           BIGSERIAL   PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users(id),
    booking_id   BIGINT      REFERENCES bookings(id),
    type         VARCHAR(50) NOT NULL CHECK (type IN (
                     'BOOKING_CONFIRMATION',
                     'SHOW_REMINDER',
                     'BOOKING_CANCELLATION',
                     'SHOW_CANCELLATION'
                 )),
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                             CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    message      TEXT,
    scheduled_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    sent_at      TIMESTAMP
);


-- ============================================================
--  INDEXES
-- ============================================================

-- users
CREATE INDEX idx_users_email          ON users(email);

-- theaters
CREATE INDEX idx_theaters_city        ON theaters(city_id);

-- screens
CREATE INDEX idx_screens_theater      ON screens(theater_id);

-- seats
CREATE INDEX idx_seats_screen         ON seats(screen_id);

-- shows
CREATE INDEX idx_shows_movie          ON shows(movie_id);
CREATE INDEX idx_shows_screen         ON shows(screen_id);
CREATE INDEX idx_shows_status         ON shows(status);
CREATE INDEX idx_shows_start_time     ON shows(start_time);

-- show_seats
CREATE INDEX idx_show_seats_show      ON show_seats(show_id);
CREATE INDEX idx_show_seats_status    ON show_seats(show_id, status);
CREATE INDEX idx_show_seats_hold_exp  ON show_seats(hold_expires_at) WHERE status = 'HELD';

-- bookings
CREATE INDEX idx_bookings_user        ON bookings(user_id);
CREATE INDEX idx_bookings_show        ON bookings(show_id);
CREATE INDEX idx_bookings_status      ON bookings(show_id, status);
CREATE INDEX idx_bookings_created     ON bookings(created_at DESC);

-- booking_items
CREATE INDEX idx_booking_items_bk     ON booking_items(booking_id);

-- payments
CREATE INDEX idx_payments_booking     ON payments(booking_id);

-- notifications
CREATE INDEX idx_notifications_user   ON notifications(user_id);
CREATE INDEX idx_notifications_bk     ON notifications(booking_id);
CREATE INDEX idx_notifications_sched  ON notifications(scheduled_at) WHERE status = 'PENDING';


-- ============================================================
--  SEED DATA
-- ============================================================

-- ----------------------------------------------------------------
--  Admin user
--  email   : admin@movieticket.com
--  password: admin123  (BCrypt 10 rounds)
--
--  To regenerate the hash in psql using pgcrypto:
--    SELECT crypt('admin123', gen_salt('bf', 10));
-- ----------------------------------------------------------------
INSERT INTO users (name, email, password_hash, role)
VALUES (
    'System Admin',
    'admin@movieticket.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ADMIN'
)
ON CONFLICT (email) DO NOTHING;

-- ----------------------------------------------------------------
--  Default refund policies (mirrors DataInitializer.java)
-- ----------------------------------------------------------------
INSERT INTO refund_policies (hours_before_show, refund_percentage, description)
VALUES
    (48,  100.00, 'Full refund if cancelled 48+ hours before show'),
    (24,   50.00, '50% refund if cancelled 24–48 hours before show'),
    (2,    25.00, '25% refund if cancelled 2–24 hours before show'),
    (0,     0.00, 'No refund if cancelled less than 2 hours before show')
ON CONFLICT DO NOTHING;

-- ----------------------------------------------------------------
--  Sample city, theater, screen, movie, show, pricing, discount
--  (optional — remove if you prefer to use the API / Postman)
-- ----------------------------------------------------------------

-- City
INSERT INTO cities (name, state, country)
VALUES ('Mumbai', 'Maharashtra', 'India')
ON CONFLICT DO NOTHING;

-- Theater (references city id=1)
INSERT INTO theaters (name, address, city_id)
VALUES ('PVR Cinemas', 'Phoenix Mall, Lower Parel', 1)
ON CONFLICT DO NOTHING;

-- Screen (references theater id=1)
INSERT INTO screens (name, total_rows, total_cols, theater_id)
VALUES ('Screen 1', 10, 15, 1)
ON CONFLICT DO NOTHING;

-- Seats for Screen 1
-- Rows A–C  → PREMIUM  (premiumRowsFromFront = 3)
-- Rows D–J  → REGULAR
DO $$
DECLARE
    r      INT;
    c      INT;
    lbl    CHAR(1);
    stype  VARCHAR(20);
BEGIN
    FOR r IN 0..9 LOOP
        lbl   := CHR(65 + r);            -- A=65
        stype := CASE WHEN r < 3 THEN 'PREMIUM' ELSE 'REGULAR' END;
        FOR c IN 1..15 LOOP
            INSERT INTO seats (row_label, col_number, seat_type, screen_id)
            VALUES (lbl, c, stype, 1)
            ON CONFLICT (screen_id, row_label, col_number) DO NOTHING;
        END LOOP;
    END LOOP;
END;
$$;

-- Movie
INSERT INTO movies (title, duration_minutes, language, genre, rating, description)
VALUES (
    'Interstellar',
    169,
    'English',
    'Sci-Fi',
    'U/A',
    'A team of explorers travel through a wormhole in space in an attempt to ensure humanity''s survival.'
)
ON CONFLICT DO NOTHING;

-- Show (movie id=1, screen id=1) — weekday show
INSERT INTO shows (movie_id, screen_id, start_time, end_time, status)
VALUES (
    1, 1,
    '2026-07-02 18:00:00',  -- Thursday (WEEKDAY)
    '2026-07-02 20:49:00',
    'ACTIVE'
)
ON CONFLICT DO NOTHING;

-- Show seats for show id=1 (one row per seat in screen 1)
INSERT INTO show_seats (show_id, seat_id, status, version)
SELECT 1, s.id, 'AVAILABLE', 0
FROM seats s
WHERE s.screen_id = 1
ON CONFLICT (show_id, seat_id) DO NOTHING;

-- Pricing tiers for show id=1
INSERT INTO pricing_tiers (show_id, seat_type, day_type, base_price)
VALUES
    (1, 'REGULAR',  'WEEKDAY', 200.00),
    (1, 'REGULAR',  'WEEKEND', 250.00),
    (1, 'PREMIUM',  'WEEKDAY', 350.00),
    (1, 'PREMIUM',  'WEEKEND', 450.00)
ON CONFLICT (show_id, seat_type, day_type) DO NOTHING;

-- Discount codes
INSERT INTO discount_codes (code, discount_type, value, max_uses, used_count, valid_from, valid_to, active, version)
VALUES
    ('SAVE20', 'PERCENTAGE', 20.00, 100, 0, '2026-01-01 00:00:00', '2026-12-31 23:59:59', TRUE, 0),
    ('FLAT50', 'FLAT',       50.00,  50, 0, '2026-01-01 00:00:00', '2026-12-31 23:59:59', TRUE, 0)
ON CONFLICT (code) DO NOTHING;


-- ============================================================
--  USEFUL QUERIES (reference — not executed)
-- ============================================================

/*
-- Check seat availability for a show
SELECT
    s.row_label,
    s.col_number,
    s.seat_type,
    ss.status,
    ss.hold_expires_at
FROM show_seats ss
JOIN seats s ON s.id = ss.seat_id
WHERE ss.show_id = 1
ORDER BY s.row_label, s.col_number;

-- Confirmed bookings for a show with payment status
SELECT
    b.id            AS booking_id,
    u.email         AS customer,
    b.final_amount,
    b.status        AS booking_status,
    p.status        AS payment_status,
    p.transaction_id,
    p.paid_at
FROM bookings b
JOIN users    u ON u.id = b.user_id
LEFT JOIN payments p ON p.booking_id = b.id
WHERE b.show_id = 1
  AND b.status  = 'CONFIRMED'
ORDER BY b.created_at;

-- Revenue summary per show
SELECT
    m.title,
    sh.start_time,
    COUNT(b.id)         AS total_bookings,
    SUM(b.final_amount) AS total_revenue
FROM shows sh
JOIN movies   m  ON m.id  = sh.movie_id
JOIN bookings b  ON b.show_id = sh.id AND b.status = 'CONFIRMED'
GROUP BY m.title, sh.start_time
ORDER BY sh.start_time;

-- Manually release expired holds (the scheduler does this automatically)
UPDATE show_seats
SET    status          = 'AVAILABLE',
       hold_expires_at = NULL,
       held_by_user_id = NULL
WHERE  status          = 'HELD'
  AND  hold_expires_at < NOW();
*/
