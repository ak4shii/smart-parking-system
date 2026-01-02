-- ============================================================================
-- COMPLETE DEMO DATA FOR SMART PARKING SYSTEM - USER ID = 1
-- ============================================================================
-- This script creates ALL demo data for user_id = 1 in one file
-- Includes: Parking spaces, Microcontrollers, Slots, RFIDs, Sensors,
--           Entry logs, Doors, LCDs, and realistic parking activity
-- 
-- PREREQUISITES:
-- 1. Run schema.sql first to create tables
-- 2. User with user_id = 1 must exist (create via registration)
-- 3. Then run this demo.sql
-- ============================================================================
-- User 1 with TWO parking spaces in Hanoi:
-- 1. "Hoan Kiem Parking" (ps_id=1): 3 slots, 1 microcontroller, 3 RFIDs, 3 sensors
-- 2. "Ba Dinh Parking" (ps_id=2): 6 slots, 2 microcontrollers, 8 RFIDs, 6 sensors
-- ============================================================================

-- ============================================================================
-- PREREQUISITE CHECK: Verify user exists
-- ============================================================================
DO $$
BEGIN
    -- Check if user_id = 1 exists
    IF NOT EXISTS (SELECT 1 FROM "users" WHERE user_id = 1) THEN
        RAISE EXCEPTION 'User with user_id = 1 does not exist. Please create the user via registration first.';
    END IF;
    
    RAISE NOTICE 'User verified. Proceeding with demo data creation...';
END $$;

-- ============================================================================
-- CLEANUP: Delete existing data (if re-running) to prevent duplicates
-- ============================================================================
-- Delete in reverse order of foreign key dependencies
DELETE FROM "entry_log" WHERE log_id >= 1;
DELETE FROM "sensor" WHERE sensor_id >= 1;
DELETE FROM "lcd" WHERE lcd_id >= 1;
DELETE FROM "door" WHERE door_id >= 1;
DELETE FROM "rfid" WHERE rfid_id >= 1;
DELETE FROM "slot" WHERE slot_id >= 1;
DELETE FROM "microcontroller" WHERE mc_id >= 1;
DELETE FROM "user_parking_space" WHERE user_id = 1;
DELETE FROM "parking_space" WHERE ps_id >= 1;

-- ============================================================================
-- PARKING SPACES
-- ============================================================================

-- First parking space (Hoan Kiem District)
INSERT INTO "parking_space" ("ps_id","name","location","owner") VALUES
(1, 'Hoan Kiem Parking', '36 Hang Bai, Hoan Kiem, Ha Noi', 'user1@example.com');

-- Second parking space (Ba Dinh District - Premium)
INSERT INTO "parking_space" ("ps_id","name","location","owner") VALUES
(2, 'Ba Dinh Parking', '19 Hoang Dieu, Ba Dinh, Ha Noi', 'user1@example.com');

-- Link user to both parking spaces
INSERT INTO "user_parking_space" ("user_id","ps_id") VALUES 
(1, 1),
(1, 2);

-- ============================================================================
-- MICROCONTROLLERS
-- ============================================================================

-- Microcontroller for Hoan Kiem Parking
INSERT INTO "microcontroller" ("mc_id","mc_code","name","online","uptime_sec","last_seen","ps_id") VALUES
(1, 'MC-HK-001', 'Hoan Kiem Controller', true, 0, now(), 1);

-- Two microcontrollers for Ba Dinh Parking (distributed control)
INSERT INTO "microcontroller" ("mc_id","mc_code","name","online","uptime_sec","last_seen","ps_id") VALUES
(2, 'MC-BD-01', 'Ba Dinh Controller 1', true, 3600, now(), 2),
(3, 'MC-BD-02', 'Ba Dinh Controller 2', true, 3600, now(), 2);

-- ============================================================================
-- PARKING SLOTS
-- ============================================================================

-- Three slots for Hoan Kiem Parking
INSERT INTO "slot" ("slot_id","ps_id","is_occupied") VALUES
(1, 1, false),  -- Slot 1
(2, 1, false),  -- Slot 2
(3, 1, true);   -- Slot 3 (occupied)

-- Six slots for Ba Dinh Parking (more capacity)
INSERT INTO "slot" ("slot_id","ps_id","is_occupied") VALUES
(4, 2, false),  -- Slot 1
(5, 2, true),   -- Slot 2 (occupied)
(6, 2, false),  -- Slot 3
(7, 2, true),   -- Slot 4 (occupied)
(8, 2, false),  -- Slot 5
(9, 2, false);  -- Slot 6

-- ============================================================================
-- RFID CARDS
-- ============================================================================

-- Three RFIDs for Hoan Kiem Parking
INSERT INTO "rfid" ("rfid_id","rfid_code","ps_id","currently_used") VALUES
(1, 'RFID-HK-001', 1, false),
(2, 'RFID-HK-002', 1, false),
(3, 'RFID-HK-003', 1, true);

-- Eight RFIDs for Ba Dinh Parking (more access cards)
INSERT INTO "rfid" ("rfid_id","rfid_code","ps_id","currently_used") VALUES
(4, 'RFID-BD-001', 2, false),
(5, 'RFID-BD-002', 2, true),
(6, 'RFID-BD-003', 2, false),
(7, 'RFID-BD-004', 2, true),
(8, 'RFID-BD-005', 2, false),
(9, 'RFID-BD-006', 2, false),
(10, 'RFID-BD-007', 2, false),
(11, 'RFID-BD-008', 2, false);

-- ============================================================================
-- SENSORS
-- ============================================================================

-- Three sensors for Hoan Kiem Parking (one per slot)
INSERT INTO "sensor" ("sensor_id","name","type","slot_id","mc_id") VALUES
(1, 'HK-Sensor-1', 'ultrasonic', 1, 1),
(2, 'HK-Sensor-2', 'ultrasonic', 2, 1),
(3, 'HK-Sensor-3', 'ultrasonic', 3, 1);

-- Six sensors for Ba Dinh Parking (distributed across two microcontrollers)
INSERT INTO "sensor" ("sensor_id","name","type","slot_id","mc_id") VALUES
(4, 'BD-Sensor-1', 'ultrasonic', 4, 2),
(5, 'BD-Sensor-2', 'ultrasonic', 5, 2),
(6, 'BD-Sensor-3', 'ultrasonic', 6, 2),
(7, 'BD-Sensor-4', 'infrared', 7, 3),
(8, 'BD-Sensor-5', 'infrared', 8, 3),
(9, 'BD-Sensor-6', 'infrared', 9, 3);

-- ============================================================================
-- DOORS FOR PARKING SPACES
-- ============================================================================

-- Doors for Hoan Kiem Parking (ps_id=1, mc_id=1)
INSERT INTO "door" ("name","is_opened","mc_id") VALUES
('HK Entry Gate', false, 1),
('HK Exit Gate', false, 1);

-- Doors for Ba Dinh Parking (ps_id=2, mc_id=2 and mc_id=3)
INSERT INTO "door" ("name","is_opened","mc_id") VALUES
('BD Main Entrance', false, 2),
('BD Main Exit', false, 2),
('BD Side Entrance', false, 3),
('BD Emergency Exit', false, 3);

-- ============================================================================
-- LCD DISPLAYS FOR PARKING SPACES
-- ============================================================================

-- LCDs for Hoan Kiem Parking (mc_id=1)
INSERT INTO "lcd" ("name","display","mc_id") VALUES
('HK Entrance Display', 'Welcome! 1/3 Available', 1),
('HK Status Board', 'Slots: [Free][Free][Occupied]', 1);

-- LCDs for Ba Dinh Parking (mc_id=2 and mc_id=3)
INSERT INTO "lcd" ("name","display","mc_id") VALUES
('BD Main Display', 'Welcome! 4/6 Slots Free', 2),
('BD Section A Board', 'Section A: 2/3 Available', 2),
('BD Section B Board', 'Section B: 2/3 Available', 3),
('BD Exit Display', 'Thank you! Drive safely', 3);

-- ============================================================================
-- ENTRY LOGS - HISTORICAL DATA (Completed Parking Sessions)
-- ============================================================================
-- Past completed sessions from the last 30 days

-- Hoan Kiem Parking - Completed sessions
INSERT INTO "entry_log" ("rfid_id","license_plate","in_time","out_time") VALUES
-- 30 days ago
(1, '29A-12345', now() - interval '30 days' - interval '3 hours', now() - interval '30 days' - interval '1 hour'),
(2, '30B-67890', now() - interval '29 days' - interval '5 hours', now() - interval '29 days' - interval '3 hours'),
(3, '29C-11111', now() - interval '28 days' - interval '2 hours', now() - interval '28 days' - interval '30 minutes'),

-- 25 days ago
(1, '30A-98765', now() - interval '25 days' - interval '4 hours', now() - interval '25 days' - interval '1 hour'),
(2, '29B-22222', now() - interval '24 days' - interval '6 hours', now() - interval '24 days' - interval '2 hours'),

-- 20 days ago
(1, '29D-33333', now() - interval '20 days' - interval '8 hours', now() - interval '20 days' - interval '4 hours'),
(3, '30C-44444', now() - interval '19 days' - interval '3 hours', now() - interval '19 days' - interval '1 hour'),

-- 15 days ago
(2, '29E-55555', now() - interval '15 days' - interval '5 hours', now() - interval '15 days' - interval '3 hours'),
(1, '30D-66666', now() - interval '14 days' - interval '7 hours', now() - interval '14 days' - interval '5 hours'),

-- 10 days ago
(3, '29F-77777', now() - interval '10 days' - interval '2 hours', now() - interval '10 days' - interval '45 minutes'),
(2, '30E-88888', now() - interval '9 days' - interval '4 hours', now() - interval '9 days' - interval '2 hours'),

-- Ba Dinh Parking - Completed sessions
-- 28 days ago
(4, '29G-10001', now() - interval '28 days' - interval '6 hours', now() - interval '28 days' - interval '3 hours'),
(5, '30F-20002', now() - interval '27 days' - interval '8 hours', now() - interval '27 days' - interval '4 hours'),
(6, '29H-30003', now() - interval '26 days' - interval '5 hours', now() - interval '26 days' - interval '2 hours'),

-- 22 days ago
(7, '30G-40004', now() - interval '22 days' - interval '7 hours', now() - interval '22 days' - interval '3 hours'),
(8, '29I-50005', now() - interval '21 days' - interval '4 hours', now() - interval '21 days' - interval '1 hour'),
(9, '30H-60006', now() - interval '20 days' - interval '9 hours', now() - interval '20 days' - interval '5 hours'),

-- 18 days ago
(4, '29J-70007', now() - interval '18 days' - interval '3 hours', now() - interval '18 days' - interval '1 hour'),
(5, '30I-80008', now() - interval '17 days' - interval '6 hours', now() - interval '17 days' - interval '3 hours'),

-- 12 days ago
(10, '29K-90009', now() - interval '12 days' - interval '5 hours', now() - interval '12 days' - interval '2 hours'),
(11, '30J-10010', now() - interval '11 days' - interval '7 hours', now() - interval '11 days' - interval '4 hours'),
(4, '29L-11011', now() - interval '10 days' - interval '4 hours', now() - interval '10 days' - interval '2 hours'),

-- ============================================================================
-- ENTRY LOGS - RECENT COMPLETED SESSIONS (Last 7 days)
-- ============================================================================

-- 7 days ago
(1, '29M-12012', now() - interval '7 days' - interval '5 hours', now() - interval '7 days' - interval '2 hours'),
(2, '30K-13013', now() - interval '7 days' - interval '8 hours', now() - interval '7 days' - interval '4 hours'),
(6, '29N-14014', now() - interval '7 days' - interval '3 hours', now() - interval '7 days' - interval '1 hour'),

-- 6 days ago
(3, '30L-15015', now() - interval '6 days' - interval '6 hours', now() - interval '6 days' - interval '3 hours'),
(7, '29O-16016', now() - interval '6 days' - interval '4 hours', now() - interval '6 days' - interval '2 hours'),

-- 5 days ago
(8, '30M-17017', now() - interval '5 days' - interval '7 hours', now() - interval '5 days' - interval '4 hours'),
(1, '29P-18018', now() - interval '5 days' - interval '2 hours', now() - interval '5 days' - interval '45 minutes'),

-- 4 days ago
(9, '30N-19019', now() - interval '4 days' - interval '5 hours', now() - interval '4 days' - interval '2 hours'),
(2, '29Q-20020', now() - interval '4 days' - interval '9 hours', now() - interval '4 days' - interval '5 hours'),
(10, '30O-21021', now() - interval '4 days' - interval '3 hours', now() - interval '4 days' - interval '1 hour'),

-- 3 days ago
(4, '29R-22022', now() - interval '3 days' - interval '6 hours', now() - interval '3 days' - interval '3 hours'),
(11, '30P-23023', now() - interval '3 days' - interval '4 hours', now() - interval '3 days' - interval '2 hours'),

-- 2 days ago
(3, '29S-24024', now() - interval '2 days' - interval '8 hours', now() - interval '2 days' - interval '4 hours'),
(5, '30Q-25025', now() - interval '2 days' - interval '5 hours', now() - interval '2 days' - interval '3 hours'),
(6, '29T-26026', now() - interval '2 days' - interval '7 hours', now() - interval '2 days' - interval '4 hours'),

-- Yesterday
(7, '30R-27027', now() - interval '1 day' - interval '6 hours', now() - interval '1 day' - interval '3 hours'),
(1, '29U-28028', now() - interval '1 day' - interval '4 hours', now() - interval '1 day' - interval '2 hours'),
(8, '30S-29029', now() - interval '1 day' - interval '9 hours', now() - interval '1 day' - interval '5 hours'),
(2, '29V-30030', now() - interval '1 day' - interval '3 hours', now() - interval '1 day' - interval '1 hour'),

-- ============================================================================
-- ENTRY LOGS - TODAY'S COMPLETED SESSIONS
-- ============================================================================

-- Early morning (completed)
(9, '30T-31031', now() - interval '10 hours', now() - interval '8 hours'),
(10, '29W-32032', now() - interval '9 hours', now() - interval '7 hours'),

-- Mid morning (completed)
(4, '30U-33033', now() - interval '7 hours', now() - interval '5 hours'),
(11, '29X-34034', now() - interval '6 hours', now() - interval '4 hours'),

-- Lunch time (completed)
(1, '30V-35035', now() - interval '5 hours', now() - interval '3 hours'),
(3, '29Y-36036', now() - interval '4 hours 30 minutes', now() - interval '3 hours 30 minutes'),

-- ============================================================================
-- ENTRY LOGS - CURRENTLY PARKED VEHICLES (Active Sessions)
-- ============================================================================
-- These match the occupied slots in data.sql

-- Hoan Kiem Parking - 1 vehicle currently parked (slot 3 occupied)
(3, '30W-99001', now() - interval '2 hours', null),

-- Ba Dinh Parking - 2 vehicles currently parked (slots 5 and 7 occupied)
(5, '29A-12345', now() - interval '3 hours 15 minutes', null),
(7, '30A-67890', now() - interval '1 hour 45 minutes', null);

-- ============================================================================
-- ENTRY LOGS - RECENT ENTRY (Just arrived)
-- ============================================================================

-- Just entered in the last 30 minutes
INSERT INTO "entry_log" ("rfid_id","license_plate","in_time","out_time") VALUES
(2, '29Z-88888', now() - interval '25 minutes', null),
(6, '30X-77777', now() - interval '15 minutes', null),
(8, '29AA-66666', now() - interval '10 minutes', null);

-- ============================================================================
-- COMMENTS AND STATISTICS
-- ============================================================================
-- COMPLETE DEMO DATA CREATED:
--
-- Parking Spaces: 2 (Hoan Kiem: 3 slots, Ba Dinh: 6 slots)
-- Microcontrollers: 3 (1 for Hoan Kiem, 2 for Ba Dinh)
-- Parking Slots: 9 total (3 occupied, 6 available)
-- RFID Cards: 11 (3 for Hoan Kiem, 8 for Ba Dinh)
-- Sensors: 9 (3 ultrasonic for HK, 6 mixed for BD)
-- Doors: 6 (2 for Hoan Kiem, 4 for Ba Dinh)
-- LCDs: 6 (2 for Hoan Kiem, 4 for Ba Dinh)
-- Entry Logs: ~58 records
--   - Completed sessions (historical): ~45 records
--   - Active sessions (currently parked): 6 records
--   - Recent entries: 3 records
--
-- Time Range: Last 30 days to present
-- License Plates: Realistic Vietnamese format (29A, 30A, etc.)
-- ============================================================================

-- Update sequences to avoid conflicts with future inserts
ALTER SEQUENCE IF EXISTS "parking_space_ps_id_seq" RESTART WITH 10;
ALTER SEQUENCE IF EXISTS "microcontroller_mc_id_seq" RESTART WITH 10;
ALTER SEQUENCE IF EXISTS "slot_slot_id_seq" RESTART WITH 20;
ALTER SEQUENCE IF EXISTS "rfid_rfid_id_seq" RESTART WITH 20;
ALTER SEQUENCE IF EXISTS "sensor_sensor_id_seq" RESTART WITH 20;
ALTER SEQUENCE IF EXISTS "door_door_id_seq" RESTART WITH 20;
ALTER SEQUENCE IF EXISTS "lcd_lcd_id_seq" RESTART WITH 20;
ALTER SEQUENCE IF EXISTS "entry_log_log_id_seq" RESTART WITH 200;

-- Success message
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE 'DEMO DATA SUCCESSFULLY LOADED!';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Parking Spaces: 2 (Hoan Kiem, Ba Dinh)';
    RAISE NOTICE 'Microcontrollers: 3';
    RAISE NOTICE 'Parking Slots: 9 (3 occupied)';
    RAISE NOTICE 'RFID Cards: 11';
    RAISE NOTICE 'Sensors: 9';
    RAISE NOTICE 'Doors: 6';
    RAISE NOTICE 'LCDs: 6';
    RAISE NOTICE 'Entry Logs: ~58 (spanning 30 days)';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'System ready for user_id = 1';
    RAISE NOTICE '========================================';
END $$;

