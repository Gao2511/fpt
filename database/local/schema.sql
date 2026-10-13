-- Empty local development database only. Never apply to Supabase.
BEGIN;
CREATE TABLE users(id serial PRIMARY KEY, username text UNIQUE NOT NULL, password text NOT NULL,
 role text NOT NULL DEFAULT 'customer', is_active boolean NOT NULL DEFAULT true, created_at timestamp DEFAULT NOW(),
 avatar_url text, email text, phone text, full_name text, google_id text, facebook_id text,
 auth_provider text DEFAULT 'local', email_verified boolean DEFAULT false);
CREATE TABLE packages(id serial PRIMARY KEY, package_code text UNIQUE NOT NULL, name text NOT NULL,
 price bigint NOT NULL, speed_mbps integer NOT NULL, description text, long_description text,
 is_hot boolean DEFAULT false, badge_type text DEFAULT '', created_at timestamp DEFAULT NOW());
CREATE TABLE settings(id serial PRIMARY KEY, setting_key text UNIQUE NOT NULL, setting_value text,
 description text, updated_at timestamp DEFAULT NOW());
CREATE TABLE customers(id serial PRIMARY KEY, full_name text, phone text, address text, email text, note text,
 user_id integer REFERENCES users(id), status text DEFAULT 'Mới', package_interest text,
 created_at timestamp DEFAULT NOW(), updated_at timestamp DEFAULT NOW());
CREATE TABLE email_logs(id serial PRIMARY KEY, customer_id integer REFERENCES customers(id) ON DELETE SET NULL,
 recipient_email text, subject text, status text, error_message text, sent_at timestamp DEFAULT NOW());
CREATE TABLE api_key_history(id serial PRIMARY KEY, api_key text, model text, changed_by integer REFERENCES users(id) ON DELETE SET NULL,
 changed_by_name text, action text, note text, created_at timestamp DEFAULT NOW());
CREATE TABLE password_reset_token(id serial PRIMARY KEY, user_id integer REFERENCES users(id) ON DELETE CASCADE,
 token text UNIQUE, expires_at timestamp, used boolean DEFAULT false, created_at timestamp DEFAULT NOW());
INSERT INTO packages(package_code,name,price,speed_mbps,description,long_description) VALUES
 ('195K','Gói 195K',195000,300,'Internet 300Mbps',E'WiFi 6 modem'),
 ('205K','Gói 205K',205000,300,'Internet 300Mbps và Camera',E'WiFi 6 modem\n1 Camera'),
 ('220K','Gói 220K',220000,300,'Internet 300Mbps và truyền hình',E'WiFi 6 modem\nTV 180 kênh\nNội dung Premier League'),
 ('230K','Gói 230K',230000,300,'Internet 300Mbps, Camera và truyền hình',E'WiFi 6 modem\n1 Camera\nTV 180 kênh\nNội dung Premier League'),
 ('239K','Gói 239K',239000,1000,'Internet 1000Mbps và truyền hình',E'WiFi 6 modem\nTV 180 kênh\nNội dung Premier League'),
 ('249K','Gói 249K',249000,1000,'Internet 1000Mbps, Camera và truyền hình',E'WiFi 6 modem\n1 Camera\nTV 180 kênh\nNội dung Premier League');
INSERT INTO settings(setting_key,setting_value) VALUES ('ai_chatbot_enabled','false');
COMMIT;
