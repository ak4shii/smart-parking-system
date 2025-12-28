-- ============================================================================
-- SEED DATA FOR SMART PARKING SYSTEM - HANOI
-- ============================================================================
-- User 1 with TWO parking spaces in Hanoi:
-- 1. "Hoan Kiem Parking" (ps_id=1): 3 slots, 1 microcontroller, 3 RFIDs, 3 sensors
-- 2. "Ba Dinh Parking" (ps_id=2): 6 slots, 2 microcontrollers, 8 RFIDs, 6 sensors
-- ============================================================================

-- ============================================================================
-- CLEANUP: Delete existing seed data (if any) to prevent duplicate key errors
-- ============================================================================
-- Delete in reverse order of foreign key dependencies
DELETE FROM "entry_log" WHERE log_id IN (1,2);
DELETE FROM "sensor" WHERE sensor_id BETWEEN 1 AND 9;
DELETE FROM "rfid" WHERE rfid_id BETWEEN 1 AND 11;
DELETE FROM "slot" WHERE slot_id BETWEEN 1 AND 9;
DELETE FROM "microcontroller" WHERE mc_id BETWEEN 1 AND 3;
DELETE FROM "user_parking_space" WHERE user_id = 1 AND ps_id IN (1,2);
DELETE FROM "parking_space" WHERE ps_id IN (1,2);

-- ============================================================================
-- FIRST PARKING SPACE FOR USER 1 (Hoan Kiem District)
-- ============================================================================
-- Note: Assumes user_id=1 already exists in the database

-- First parking space owned/managed by user1
INSERT INTO "parking_space" ("ps_id","name","location","owner") VALUES
(1, 'Hoan Kiem Parking', '36 Hang Bai, Hoan Kiem, Ha Noi', 'user1@example.com');

-- Link user to first parking space
INSERT INTO "user_parking_space" ("user_id","ps_id") VALUES (1,1);

-- One microcontroller for the parking space
INSERT INTO "microcontroller" ("mc_id","mc_code","name","online","uptime_sec","last_seen","ps_id") VALUES
(1, 'MC-HK-001', 'Hoan Kiem Controller', true, 0, now(), 1);

-- Three slots for the parking space
INSERT INTO "slot" ("slot_id","ps_id","is_occupied") VALUES
(1,1,false),  -- Slot 1
(2,1,false),  -- Slot 2
(3,1,true);   -- Slot 3 (occupied)

-- Three RFIDs for the parking space
INSERT INTO "rfid" ("rfid_id","rfid_code","ps_id","currently_used") VALUES
(1,'RFID-HK-001',1,false),
(2,'RFID-HK-002',1,false),
(3,'RFID-HK-003',1,true);

-- Three sensors, one per slot, attached to the same microcontroller
INSERT INTO "sensor" ("sensor_id","name","type","slot_id","mc_id") VALUES
(1,'HK-Sensor-1','ultrasonic',1,1),
(2,'HK-Sensor-2','ultrasonic',2,1),
(3,'HK-Sensor-3','ultrasonic',3,1);

-- ============================================================================
-- SECOND PARKING SPACE FOR USER 1 (Ba Dinh District - Premium)
-- ============================================================================

-- Second parking space owned/managed by user1
INSERT INTO "parking_space" ("ps_id","name","location","owner") VALUES
(2, 'Ba Dinh Parking', '19 Hoang Dieu, Ba Dinh, Ha Noi', 'user1@example.com');

-- Link user to second parking space
INSERT INTO "user_parking_space" ("user_id","ps_id") VALUES (1,2);

-- Two microcontrollers for the Ba Dinh parking space (distributed control)
INSERT INTO "microcontroller" ("mc_id","mc_code","name","online","uptime_sec","last_seen","ps_id") VALUES
(2, 'MC-BD-01', 'Ba Dinh Controller 1', true, 3600, now(), 2),
(3, 'MC-BD-02', 'Ba Dinh Controller 2', true, 3600, now(), 2);

-- Six slots for the Ba Dinh parking space (more capacity)
INSERT INTO "slot" ("slot_id","ps_id","is_occupied") VALUES
(4,2,false),  -- Slot 1
(5,2,true),   -- Slot 2 (occupied)
(6,2,false),  -- Slot 3
(7,2,true),   -- Slot 4 (occupied)
(8,2,false),  -- Slot 5
(9,2,false);  -- Slot 6

-- Eight RFIDs for the Ba Dinh parking space (more access cards)
INSERT INTO "rfid" ("rfid_id","rfid_code","ps_id","currently_used") VALUES
(4,'RFID-BD-001',2,false),
(5,'RFID-BD-002',2,true),
(6,'RFID-BD-003',2,false),
(7,'RFID-BD-004',2,true),
(8,'RFID-BD-005',2,false),
(9,'RFID-BD-006',2,false),
(10,'RFID-BD-007',2,false),
(11,'RFID-BD-008',2,false);

-- Six sensors, one per slot (distributed across two microcontrollers)
-- Sensors 1-3 on MC-BD-01, Sensors 4-6 on MC-BD-02
INSERT INTO "sensor" ("sensor_id","name","type","slot_id","mc_id") VALUES
(4,'BD-Sensor-1','ultrasonic',4,2),
(5,'BD-Sensor-2','ultrasonic',5,2),
(6,'BD-Sensor-3','ultrasonic',6,2),
(7,'BD-Sensor-4','infrared',7,3),
(8,'BD-Sensor-5','infrared',8,3),
(9,'BD-Sensor-6','infrared',9,3);

-- Example entry logs for Ba Dinh parking
INSERT INTO "entry_log" ("rfid_id","slot_id","license_plate","in_time") VALUES 
(5,5,'29A-12345', now() - interval '2 hours'),  -- Current occupancy (Hanoi plate)
(7,7,'30A-67890', now() - interval '1 hour');   -- Current occupancy (Hanoi plate)

-- ============================================================================
-- Adjust identity sequences (Postgres) so future inserts do not conflict
-- ============================================================================
ALTER SEQUENCE IF EXISTS "users_user_id_seq" RESTART WITH 2;
ALTER SEQUENCE IF EXISTS "parking_space_ps_id_seq" RESTART WITH 3;
ALTER SEQUENCE IF EXISTS "microcontroller_mc_id_seq" RESTART WITH 4;
ALTER SEQUENCE IF EXISTS "slot_slot_id_seq" RESTART WITH 10;
ALTER SEQUENCE IF EXISTS "rfid_rfid_id_seq" RESTART WITH 12;
ALTER SEQUENCE IF EXISTS "sensor_sensor_id_seq" RESTART WITH 10;
ALTER SEQUENCE IF EXISTS "entry_log_log_id_seq" RESTART WITH 3;
