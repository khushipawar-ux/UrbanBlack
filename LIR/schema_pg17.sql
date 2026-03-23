-- ============================================================
-- Urban Taxi Ride · Maharashtra
-- Location Intelligence & Routing (LIR) — Full Schema SQL
-- Tables: ride_routes, rides, driver_locations, driver_shifts,
--         driver_km_log, shifts, Driver, depots, center_points,
--         lir_trip_gps_trail, lir_geo_anomaly_events,
--         lir_pickup_optimizations, lir_venue_pickup_points,
--         lir_traffic_zones
-- Dummy data: 30 rows per table  (model training dataset)
-- ============================================================

-- ============================================================
-- PostgreSQL 17 Compatibility Notes (applied fixes):
--   1. Primary key columns changed to TEXT (gen_random_uuid()::text)
--   2. Interval string concat uses explicit ::text cast
--   3. OFFSET subqueries have deterministic ORDER BY
--   4. Array subscript uses FLOOR(random()*N) to avoid edge case
--   5. Table "Driver" renamed to driver (lowercase, no quoting needed)
--   6. All TIMESTAMP columns changed to TIMESTAMPTZ (UTC storage)
--   7. BIGSERIAL replaced with GENERATED ALWAYS AS IDENTITY (PG10+ standard)
--   8. PostGIS extension required — install postgresql-17-postgis on server
-- ============================================================

SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

-- ============================================================
-- EXTENSIONS
-- ============================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS postgis;   -- for geometry if used later

-- ============================================================
-- ENUMS
-- ============================================================
DO $$ BEGIN
  CREATE TYPE ride_status         AS ENUM ('REQUESTED','DRIVER_ASSIGNED','IN_PROGRESS','COMPLETED','CANCELLED');
  CREATE TYPE driver_status_enum  AS ENUM ('ONLINE','OFFLINE','ON_TRIP','BREAK');
  CREATE TYPE km_category         AS ENUM ('RIDE','DEAD','FREE_ROAMING');
  CREATE TYPE shift_status_enum   AS ENUM ('ACTIVE','COMPLETED','ABANDONED');
  CREATE TYPE driver_avail        AS ENUM ('AVAILABLE','UNAVAILABLE','ON_TRIP');
  CREATE TYPE fuel_level_enum     AS ENUM ('EMPTY','LOW','HALF','THREE_QUARTER','FULL');
  CREATE TYPE vehicle_cond_enum   AS ENUM ('EXCELLENT','GOOD','FAIR','POOR');
  CREATE TYPE consistency_flag    AS ENUM ('CONSISTENT','MINOR_DEVIATION','MAJOR_DEVIATION','FRAUD_SUSPECT');
  CREATE TYPE anomaly_type_enum   AS ENUM ('DETOUR','PROLONGED_STOP','SPEED_ANOMALY','GEOFENCE_VIOLATION','GPS_SPOOF_SUSPECTED');
  CREATE TYPE anomaly_severity    AS ENUM ('LOW','MEDIUM','HIGH','CRITICAL');
  CREATE TYPE pin_source_enum     AS ENUM ('RAW_GEOCODE','MODEL_OPTIMIZED','VENUE_DATABASE','DRIVER_FEEDBACK');
  CREATE TYPE venue_type_enum     AS ENUM ('AIRPORT','MALL','HOSPITAL','STATION','HOTEL','OFFICE_PARK','OTHER');
  CREATE TYPE zone_type_enum      AS ENUM ('TOLL','POLICE_ZONE','SCHOOL_ZONE','HIGH_DEMAND','RESTRICTED');
  CREATE TYPE gps_source_enum     AS ENUM ('GPS','NETWORK','FUSED','IMU_DEAD_RECKONING');
  CREATE TYPE gps_mode_enum       AS ENUM ('ACTIVE_TRIP','IDLE','OFFLINE');
  CREATE TYPE pt_type_enum        AS ENUM ('PICKUP_ONLY','DROP_ONLY','BOTH','WAYPOINT');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- ============================================================
-- TABLE 1: depots
-- (created first — referenced by center_points, Driver)
-- ============================================================
DROP TABLE IF EXISTS depots CASCADE;
CREATE TABLE depots (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    depot_code        VARCHAR(20)      NOT NULL UNIQUE,
    depot_name        VARCHAR(100),
    city              VARCHAR(50),
    full_address      TEXT,
    latitude          DOUBLE PRECISION,
    longitude         DOUBLE PRECISION,
    zone              VARCHAR(50),
    capacity          INTEGER,
    operating_start   TIME,
    operating_end     TIME,
    registration_date DATE,
    is_active         BOOLEAN          DEFAULT TRUE,
    -- LIR additions
    geofence_radius_m INTEGER,
    zone_polygon_json TEXT,
    max_speed_kmh     INTEGER
);

INSERT INTO depots (depot_code,depot_name,city,full_address,latitude,longitude,zone,capacity,operating_start,operating_end,registration_date,is_active,geofence_radius_m,max_speed_kmh) VALUES
('DP-PUNE-01','Shivajinagar Depot','Pune','FC Road, Shivajinagar, Pune 411005',18.5314,73.8446,'Zone-A',120,'05:00','23:00','2022-01-10',true,800,50),
('DP-PUNE-02','Kothrud Depot','Pune','Paud Road, Kothrud, Pune 411038',18.5074,73.8077,'Zone-B',90,'05:30','22:30','2022-03-15',true,600,40),
('DP-PUNE-03','Hadapsar Depot','Pune','Magarpatta Road, Hadapsar, Pune 411028',18.5089,73.9260,'Zone-C',110,'05:00','23:00','2022-04-20',true,700,50),
('DP-PUNE-04','Hinjewadi Depot','Pune','Phase 1, Hinjewadi, Pune 411057',18.5912,73.7389,'Zone-D',80,'04:30','00:00','2022-06-01',true,500,60),
('DP-PUNE-05','Viman Nagar Depot','Pune','Viman Nagar, Pune 411014',18.5679,73.9143,'Zone-E',95,'05:00','23:30','2022-07-12',true,650,50),
('DP-PUNE-06','Wakad Depot','Pune','Wakad, Pimpri-Chinchwad 411057',18.5984,73.7610,'Zone-D',75,'05:00','23:00','2022-08-05',true,500,50),
('DP-PUNE-07','Kharadi Depot','Pune','EON IT Park Road, Kharadi, Pune 411014',18.5515,73.9481,'Zone-E',100,'05:00','23:00','2022-09-10',true,700,50),
('DP-PUNE-08','Baner Depot','Pune','Baner Road, Baner, Pune 411045',18.5590,73.7868,'Zone-B',85,'05:30','22:30','2022-10-01',true,600,40),
('DP-MUM-01','Andheri Depot','Mumbai','MIDC Andheri East, Mumbai 400093',19.1136,72.8697,'MUM-West',150,'04:00','00:00','2022-01-20',true,900,60),
('DP-MUM-02','Thane Depot','Mumbai','Ghodbunder Road, Thane West 400607',19.2183,72.9781,'MUM-North',120,'04:30','23:30','2022-03-08',true,800,60),
('DP-NGP-01','Sitabuldi Depot','Nagpur','Sitabuldi, Nagpur 440012',21.1493,79.0849,'NGP-Central',70,'05:00','22:00','2022-05-15',true,500,40),
('DP-NASH-01','Nashik Road Depot','Nashik','Nashik Road, Nashik 422101',19.9975,73.8244,'NASH-East',60,'05:30','21:30','2022-07-01',true,400,40),
('DP-PUNE-09','Pimpri Depot','Pune','Old Mumbai Road, Pimpri 411018',18.6186,73.8003,'Zone-F',90,'05:00','23:00','2023-01-05',true,600,50),
('DP-PUNE-10','Kondhwa Depot','Pune','NIBM Road, Kondhwa, Pune 411048',18.4636,73.8930,'Zone-G',80,'05:30','22:00','2023-02-20',true,550,50),
('DP-PUNE-11','Swargate Depot','Pune','Swargate, Pune 411042',18.5018,73.8636,'Zone-A',100,'05:00','23:00','2023-03-10',true,700,40),
('DP-PUNE-12','Aundh Depot','Pune','Aundh Road, Aundh, Pune 411007',18.5583,73.8081,'Zone-B',88,'05:00','22:30','2023-04-15',true,600,50),
('DP-PUNE-13','Wanowrie Depot','Pune','Wanowrie, Pune 411040',18.4977,73.8965,'Zone-G',72,'05:30','22:00','2023-05-20',true,500,50),
('DP-PUNE-14','Deccan Depot','Pune','FC Road, Deccan, Pune 411004',18.5178,73.8422,'Zone-A',95,'05:00','23:00','2023-06-01',true,650,40),
('DP-MUM-03','Powai Depot','Mumbai','Hiranandani Gardens, Powai 400076',19.1197,72.9050,'MUM-East',110,'04:30','23:30','2023-01-15',true,750,60),
('DP-PUNE-15','Katraj Depot','Pune','Katraj, Pune 411046',18.4525,73.8652,'Zone-H',70,'05:30','22:00','2023-07-10',true,480,50),
('DP-PUNE-16','Nibm Depot','Pune','NIBM Annexe, Pune 411048',18.4552,73.9001,'Zone-G',65,'05:30','22:00','2023-08-01',true,450,50),
('DP-PUNE-17','Magarpatta Depot','Pune','Magarpatta City, Hadapsar 411013',18.5158,73.9274,'Zone-C',105,'05:00','23:00','2023-09-05',true,700,50),
('DP-PUNE-18','Bhosari Depot','Pune','Bhosari MIDC, Pune 411026',18.6453,73.8520,'Zone-F',80,'05:00','23:00','2023-10-10',true,550,60),
('DP-PUNE-19','Talawade Depot','Pune','Talawade IT Park, Pune 411062',18.6321,73.7668,'Zone-D',75,'04:30','00:00','2023-11-01',true,500,60),
('DP-PUNE-20','Ambegaon Depot','Pune','Ambegaon BK, Pune 411046',18.4478,73.8496,'Zone-H',60,'05:30','22:00','2023-12-01',true,420,40),
('DP-MUM-04','Navi Mumbai Depot','Mumbai','Vashi Sector 17, Navi Mumbai 400703',19.0748,73.0159,'MUM-South',130,'04:30','23:30','2023-02-28',true,850,60),
('DP-PUNE-21','Pimple Saudagar Depot','Pune','Pimple Saudagar, Pune 411027',18.5870,73.7986,'Zone-B',85,'05:00','23:00','2024-01-15',true,580,50),
('DP-PUNE-22','Undri Depot','Pune','Undri, Pune 411060',18.4575,73.9253,'Zone-G',68,'05:30','22:00','2024-02-20',true,460,50),
('DP-PUNE-23','Mundhwa Depot','Pune','Mundhwa, Pune 411036',18.5330,73.9370,'Zone-C',90,'05:00','23:00','2024-03-10',true,620,50),
('DP-PUNE-24','Koregaon Park Depot','Pune','Koregaon Park, Pune 411001',18.5362,73.8938,'Zone-A',82,'05:30','22:30','2024-04-05',true,560,40);

-- ============================================================
-- TABLE 2: center_points
-- ============================================================
DROP TABLE IF EXISTS center_points CASCADE;
CREATE TABLE center_points (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    point_name   VARCHAR(100),
    latitude     DOUBLE PRECISION,
    longitude    DOUBLE PRECISION,
    depot_id     BIGINT           REFERENCES depots(id),
    -- LIR additions
    point_type   pt_type_enum     DEFAULT 'BOTH',
    avg_wait_sec INTEGER,
    is_active    BOOLEAN          DEFAULT TRUE
);

INSERT INTO center_points (point_name,latitude,longitude,depot_id,point_type,avg_wait_sec,is_active) VALUES
('FC Road Bus Stop',18.5314,73.8446,1,'BOTH',120,true),
('Shivajinagar Court Gate',18.5298,73.8468,1,'PICKUP_ONLY',95,true),
('Paud Road Junction',18.5074,73.8077,2,'BOTH',140,true),
('Kothrud Depot Gate',18.5060,73.8095,2,'PICKUP_ONLY',110,true),
('Magarpatta Main Gate',18.5089,73.9260,3,'BOTH',105,true),
('Hadapsar Fatima Nagar',18.5102,73.9241,3,'BOTH',130,true),
('Hinjewadi Phase 1 Gate',18.5912,73.7389,4,'PICKUP_ONLY',180,true),
('Hinjewadi Infosys Gate',18.5945,73.7420,4,'BOTH',165,true),
('Viman Nagar Fountain',18.5679,73.9143,5,'BOTH',115,true),
('Clover Park Stop',18.5661,73.9160,5,'PICKUP_ONLY',100,true),
('Wakad Bridge',18.5984,73.7610,6,'BOTH',145,true),
('Kharadi EON Gate',18.5515,73.9481,7,'BOTH',125,true),
('Kharadi Bypass',18.5498,73.9505,7,'PICKUP_ONLY',138,true),
('Baner Balewadi Stop',18.5590,73.7868,8,'BOTH',155,true),
('Andheri Metro Gate',19.1136,72.8697,9,'BOTH',200,true),
('Thane Station East',19.2183,72.9781,10,'BOTH',185,true),
('Sitabuldi Central',21.1493,79.0849,11,'BOTH',130,true),
('Nashik Road Station',19.9975,73.8244,12,'BOTH',150,true),
('Pimpri Bus Stand',18.6186,73.8003,13,'BOTH',145,true),
('NIBM Road T-point',18.4636,73.8930,14,'BOTH',160,true),
('Swargate Bus Terminal',18.5018,73.8636,15,'BOTH',135,true),
('Aundh D-Mart',18.5583,73.8081,16,'BOTH',120,true),
('Wanowrie Signal',18.4977,73.8965,17,'BOTH',155,true),
('Deccan Corner',18.5178,73.8422,18,'BOTH',110,true),
('Powai Hiranandani',19.1197,72.9050,19,'BOTH',210,true),
('Katraj Chowk',18.4525,73.8652,20,'BOTH',175,true),
('Magarpatta Cybercity Gate',18.5158,73.9274,22,'PICKUP_ONLY',118,true),
('Bhosari MIDC Gate',18.6453,73.8520,23,'PICKUP_ONLY',160,true),
('Talawade IT Parking',18.6321,73.7668,24,'BOTH',190,true),
('KP North Main Road',18.5362,73.8938,29,'BOTH',108,true);

-- ============================================================
-- TABLE 3: lir_venue_pickup_points
-- ============================================================
DROP TABLE IF EXISTS lir_venue_pickup_points CASCADE;
CREATE TABLE lir_venue_pickup_points (
    id              TEXT             PRIMARY KEY DEFAULT gen_random_uuid()::text,
    venue_name      VARCHAR(150)     NOT NULL,
    venue_type      venue_type_enum  NOT NULL,
    lat             DOUBLE PRECISION NOT NULL,
    lng             DOUBLE PRECISION NOT NULL,
    boundary_json   TEXT,
    label           VARCHAR(100),
    is_active       BOOLEAN          NOT NULL DEFAULT TRUE,
    avg_wait_sec    INTEGER,
    created_at      TIMESTAMPTZ        NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ        NOT NULL DEFAULT NOW()
);

INSERT INTO lir_venue_pickup_points (venue_name,venue_type,lat,lng,label,is_active,avg_wait_sec) VALUES
('Pune Airport - Departures','AIRPORT',18.5793,73.9089,'Departures Drop Zone',true,240),
('Pune Airport - Arrivals T1','AIRPORT',18.5799,73.9077,'T1 Arrivals Pickup',true,280),
('Pune Airport - Arrivals T2','AIRPORT',18.5801,73.9102,'T2 Arrivals Pickup',true,270),
('Phoenix Marketcity Pune - Gate 1','MALL',18.5592,73.9108,'Main Entrance Gate 1',true,180),
('Phoenix Marketcity Pune - Parking','MALL',18.5579,73.9121,'Basement Parking Exit',true,210),
('Amanora Town Centre - North Gate','MALL',18.5143,73.9322,'North Entry Pickup',true,195),
('Seasons Mall Hadapsar','MALL',18.5095,73.9365,'Main Gate Pickup',true,220),
('Ruby Hall Clinic - OPD Entrance','HOSPITAL',18.5313,73.8780,'OPD Drop-off Zone',true,150),
('KEM Hospital Pune','HOSPITAL',18.5195,73.8557,'Emergency Gate',true,140),
('Deenanath Mangeshkar Hospital','HOSPITAL',18.5210,73.8283,'Main Gate',true,155),
('Inamdar Multispecialty Hospital','HOSPITAL',18.5098,73.9174,'Front Entrance',true,160),
('Pune Junction Railway Station - East','STATION',18.5280,73.8742,'East Pickup Bay',true,300),
('Pune Junction Railway Station - West','STATION',18.5269,73.8730,'West Exit Pickup',true,320),
('Shivajinagar Railway Station','STATION',18.5308,73.8434,'Platform 1 Exit',true,260),
('Chinchwad Railway Station','STATION',18.6351,73.7938,'North Exit',true,280),
('JW Marriott Pune','HOTEL',18.5406,73.8281,'Hotel Porch',true,90),
('Hyatt Regency Pune','HOTEL',18.5582,73.9030,'Hotel Main Gate',true,95),
('Conrad Pune','HOTEL',18.5355,73.8934,'Porch Pickup',true,88),
('Novotel Pune Viman Nagar','HOTEL',18.5685,73.9140,'Lobby Entrance',true,100),
('WTC Pune - IT Tower Lobby','OFFICE_PARK',18.5512,73.9487,'WTC Tower A',true,170),
('Cybercity Magarpatta - Gate 3','OFFICE_PARK',18.5162,73.9291,'Gate 3 Pickup',true,185),
('Hinjewadi IT Park - Infosys BPO','OFFICE_PARK',18.5952,73.7398,'BPO Main Gate',true,200),
('EON IT Park Kharadi - Phase 1','OFFICE_PARK',18.5526,73.9472,'Phase 1 Entry',true,190),
('Panchshil Tech Park Yerwada','OFFICE_PARK',18.5614,73.9268,'Tech Park Gate',true,178),
('Mumbai CST - North Entrance','STATION',18.9402,72.8356,'North Pickup Zone',true,360),
('Mumbai Airport T2 Pickup','AIRPORT',19.0896,72.8656,'T2 Arrivals Pickup Bay',true,310),
('Seawoods Grand Central Mall','MALL',19.0164,73.0216,'Grand Central Gate',true,205),
('Thane Station Pickup Bay','STATION',19.1809,72.9754,'East Exit Bay',true,290),
('Nashik Road Railway Station','STATION',19.9975,73.8257,'Platform Exit',true,275),
('Nagpur Airport - Arrivals','AIRPORT',21.0922,79.0472,'Arrivals Pickup',true,295);

-- ============================================================
-- TABLE 4: lir_traffic_zones
-- ============================================================
DROP TABLE IF EXISTS lir_traffic_zones CASCADE;
CREATE TABLE lir_traffic_zones (
    id              TEXT             PRIMARY KEY DEFAULT gen_random_uuid()::text,
    zone_name       VARCHAR(100)     NOT NULL,
    zone_type       zone_type_enum   NOT NULL,
    polygon_json    TEXT             NOT NULL,
    speed_limit_kmh INTEGER,
    is_active       BOOLEAN          NOT NULL DEFAULT TRUE,
    active_from     TIME,
    active_to       TIME,
    created_at      TIMESTAMPTZ        NOT NULL DEFAULT NOW()
);

INSERT INTO lir_traffic_zones (zone_name,zone_type,polygon_json,speed_limit_kmh,is_active,active_from,active_to) VALUES
('Shivajinagar CBD','RESTRICTED','{"type":"Polygon","coordinates":[[[73.840,18.528],[73.852,18.528],[73.852,18.536],[73.840,18.536],[73.840,18.528]]]}',30,true,null,null),
('Pune Airport Toll','TOLL','{"type":"Polygon","coordinates":[[[73.905,18.577],[73.915,18.577],[73.915,18.582],[73.905,18.582],[73.905,18.577]]]}',null,true,null,null),
('Ferguson College Road School Zone','SCHOOL_ZONE','{"type":"Polygon","coordinates":[[[73.840,18.519],[73.848,18.519],[73.848,18.525],[73.840,18.525],[73.840,18.519]]]}',20,true,'07:30','09:30'),
('Swargate Police Checkpoint','POLICE_ZONE','{"type":"Polygon","coordinates":[[[73.860,18.499],[73.866,18.499],[73.866,18.505],[73.860,18.505],[73.860,18.499]]]}',null,true,null,null),
('Hadapsar Industrial MIDC','RESTRICTED','{"type":"Polygon","coordinates":[[[73.920,18.505],[73.935,18.505],[73.935,18.515],[73.920,18.515],[73.920,18.505]]]}',40,true,null,null),
('Hinjewadi Peak Demand Zone','HIGH_DEMAND','{"type":"Polygon","coordinates":[[[73.730,18.585],[73.750,18.585],[73.750,18.598],[73.730,18.598],[73.730,18.585]]]}',null,true,'08:00','10:00'),
('Kothrud School Zone','SCHOOL_ZONE','{"type":"Polygon","coordinates":[[[73.800,18.504],[73.810,18.504],[73.810,18.512],[73.800,18.512],[73.800,18.504]]]}',20,true,'07:30','09:30'),
('Katraj Toll Plaza','TOLL','{"type":"Polygon","coordinates":[[[73.862,18.448],[73.870,18.448],[73.870,18.455],[73.862,18.455],[73.862,18.448]]]}',null,true,null,null),
('Viman Nagar High Demand','HIGH_DEMAND','{"type":"Polygon","coordinates":[[[73.907,18.562],[73.920,18.562],[73.920,18.574],[73.907,18.574],[73.907,18.562]]]}',null,true,'17:00','20:00'),
('Kharadi Bypass Toll','TOLL','{"type":"Polygon","coordinates":[[[73.940,18.546],[73.950,18.546],[73.950,18.556],[73.940,18.556],[73.940,18.546]]]}',null,true,null,null),
('Aundh School Cluster','SCHOOL_ZONE','{"type":"Polygon","coordinates":[[[73.803,18.555],[73.813,18.555],[73.813,18.563],[73.803,18.563],[73.803,18.555]]]}',20,true,'07:00','09:00'),
('Baner Balewadi Peak','HIGH_DEMAND','{"type":"Polygon","coordinates":[[[73.780,18.554],[73.795,18.554],[73.795,18.564],[73.780,18.564],[73.780,18.554]]]}',null,true,'08:00','10:00'),
('Wakad Signal Zone','RESTRICTED','{"type":"Polygon","coordinates":[[[73.755,18.594],[73.765,18.594],[73.765,18.603],[73.755,18.603],[73.755,18.594]]]}',30,true,null,null),
('Koregaon Park Night Zone','RESTRICTED','{"type":"Polygon","coordinates":[[[73.888,18.532],[73.898,18.532],[73.898,18.540],[73.888,18.540],[73.888,18.532]]]}',30,true,'22:00','05:00'),
('Pimpri Chinchwad Police Zone','POLICE_ZONE','{"type":"Polygon","coordinates":[[[73.795,18.615],[73.808,18.615],[73.808,18.626],[73.795,18.626],[73.795,18.615]]]}',null,true,null,null),
('Deccan Gymkhana Peak','HIGH_DEMAND','{"type":"Polygon","coordinates":[[[73.836,18.514],[73.848,18.514],[73.848,18.522],[73.836,18.522],[73.836,18.514]]]}',null,true,'17:30','20:30'),
('Pune Station Restricted','RESTRICTED','{"type":"Polygon","coordinates":[[[73.870,18.524],[73.878,18.524],[73.878,18.532],[73.870,18.532],[73.870,18.524]]]}',20,true,null,null),
('Magarpatta IT Peak Zone','HIGH_DEMAND','{"type":"Polygon","coordinates":[[[73.924,18.511],[73.934,18.511],[73.934,18.521],[73.924,18.521],[73.924,18.511]]]}',null,true,'08:30','10:30'),
('Bhosari MIDC Toll','TOLL','{"type":"Polygon","coordinates":[[[73.848,18.641],[73.858,18.641],[73.858,18.650],[73.848,18.650],[73.848,18.641]]]}',null,true,null,null),
('Katraj-Dehu Road Highway','RESTRICTED','{"type":"Polygon","coordinates":[[[73.858,18.440],[73.870,18.440],[73.870,18.450],[73.858,18.450],[73.858,18.440]]]}',80,true,null,null),
('Yerwada Prison Area','POLICE_ZONE','{"type":"Polygon","coordinates":[[[73.900,18.548],[73.912,18.548],[73.912,18.558],[73.900,18.558],[73.900,18.548]]]}',null,true,null,null),
('Talawade IT Peak','HIGH_DEMAND','{"type":"Polygon","coordinates":[[[73.760,18.627],[73.775,18.627],[73.775,18.638],[73.760,18.638],[73.760,18.627]]]}',null,true,'08:00','10:00'),
('Mundhwa School Zone','SCHOOL_ZONE','{"type":"Polygon","coordinates":[[[73.929,18.530],[73.940,18.530],[73.940,18.538],[73.929,18.538],[73.929,18.530]]]}',20,true,'07:30','09:00'),
('Wanowrie Signal Restricted','RESTRICTED','{"type":"Polygon","coordinates":[[[73.892,18.494],[73.902,18.494],[73.902,18.502],[73.892,18.502],[73.892,18.494]]]}',30,true,null,null),
('Kondhwa Police Naka','POLICE_ZONE','{"type":"Polygon","coordinates":[[[73.888,18.459],[73.898,18.459],[73.898,18.467],[73.888,18.467],[73.888,18.459]]]}',null,true,null,null),
('Pashan Road School Zone','SCHOOL_ZONE','{"type":"Polygon","coordinates":[[[73.806,18.533],[73.815,18.533],[73.815,18.541],[73.806,18.541],[73.806,18.533]]]}',20,true,'07:30','09:30'),
('Sinhagad Road Toll','TOLL','{"type":"Polygon","coordinates":[[[73.814,18.478],[73.822,18.478],[73.822,18.486],[73.814,18.486],[73.814,18.478]]]}',null,true,null,null),
('Undri Peak Zone','HIGH_DEMAND','{"type":"Polygon","coordinates":[[[73.920,18.453],[73.932,18.453],[73.932,18.463],[73.920,18.463],[73.920,18.453]]]}',null,true,'17:00','20:00'),
('Bibwewadi Police Zone','POLICE_ZONE','{"type":"Polygon","coordinates":[[[73.856,18.488],[73.866,18.488],[73.866,18.498],[73.856,18.498],[73.856,18.488]]]}',null,true,null,null),
('Pimple Saudagar School Zone','SCHOOL_ZONE','{"type":"Polygon","coordinates":[[[73.793,18.583],[73.803,18.583],[73.803,18.592],[73.793,18.592],[73.793,18.583]]]}',20,true,'07:30','09:30');

-- ============================================================
-- TABLE 5: Driver
-- ============================================================
DROP TABLE IF EXISTS driver CASCADE;
CREATE TABLE driver (
    id               TEXT               PRIMARY KEY DEFAULT gen_random_uuid()::text,
    email            VARCHAR(150)        NOT NULL UNIQUE,
    phone_number     VARCHAR(15)         UNIQUE,
    first_name       VARCHAR(50),
    last_name        VARCHAR(50),
    profile_image    VARCHAR(255),
    is_active        BOOLEAN             DEFAULT TRUE,
    is_verified      BOOLEAN             DEFAULT FALSE,
    city             VARCHAR(50),
    language         VARCHAR(30),
    employee_id      VARCHAR(30),
    depot_name       VARCHAR(100),
    depot_id         BIGINT              REFERENCES depots(id),
    license_number   VARCHAR(30),
    rating           DOUBLE PRECISION    DEFAULT 4.5,
    total_trips      INTEGER             DEFAULT 0,
    total_distance   DOUBLE PRECISION    DEFAULT 0.0,
    status           driver_status_enum  DEFAULT 'OFFLINE',
    home_zone        VARCHAR(50),
    date_of_joining  DATE,
    created_at       TIMESTAMPTZ           DEFAULT NOW(),
    updated_at       TIMESTAMPTZ           DEFAULT NOW()
);

INSERT INTO driver (email,phone_number,first_name,last_name,is_active,is_verified,city,language,employee_id,depot_name,depot_id,license_number,rating,total_trips,total_distance,status,home_zone,date_of_joining) VALUES
('rahul.pawar@urbanblack.in','9876543210','Rahul','Pawar',true,true,'Pune','Marathi','EMP-001','Shivajinagar Depot',1,'MH12-20180045321',4.8,1240,18600.5,'ONLINE','Zone-A','2022-02-01'),
('suresh.kamble@urbanblack.in','9876543211','Suresh','Kamble',true,true,'Pune','Marathi','EMP-002','Kothrud Depot',2,'MH12-20170034512',4.6,980,14700.0,'ONLINE','Zone-B','2022-03-15'),
('vijay.shinde@urbanblack.in','9876543212','Vijay','Shinde',true,true,'Pune','Marathi','EMP-003','Hadapsar Depot',3,'MH12-20190056234',4.7,1450,21750.0,'ON_TRIP','Zone-C','2022-04-10'),
('anil.jadhav@urbanblack.in','9876543213','Anil','Jadhav',true,true,'Pune','Hindi','EMP-004','Hinjewadi Depot',4,'MH14-20160023411',4.5,760,11400.0,'ONLINE','Zone-D','2022-05-20'),
('ravi.mane@urbanblack.in','9876543214','Ravi','Mane',true,true,'Pune','Marathi','EMP-005','Viman Nagar Depot',5,'MH12-20200067891',4.9,1680,25200.0,'ON_TRIP','Zone-E','2022-06-01'),
('santosh.desai@urbanblack.in','9876543215','Santosh','Desai',true,true,'Pune','Marathi','EMP-006','Wakad Depot',6,'MH12-20180048901',4.4,890,13350.0,'ONLINE','Zone-D','2022-07-12'),
('pramod.kulkarni@urbanblack.in','9876543216','Pramod','Kulkarni',true,true,'Pune','Marathi','EMP-007','Kharadi Depot',7,'MH12-20190059012',4.7,1120,16800.0,'BREAK','Zone-E','2022-08-05'),
('ganesh.thorat@urbanblack.in','9876543217','Ganesh','Thorat',true,true,'Pune','Marathi','EMP-008','Baner Depot',8,'MH12-20170038901',4.6,1340,20100.0,'ON_TRIP','Zone-B','2022-09-10'),
('deepak.nair@urbanblack.in','9876543218','Deepak','Nair',true,true,'Mumbai','Hindi','EMP-009','Andheri Depot',9,'MH01-20190045678',4.8,1890,28350.0,'ONLINE','MUM-West','2022-10-01'),
('rohit.yadav@urbanblack.in','9876543219','Rohit','Yadav',true,true,'Mumbai','Hindi','EMP-010','Thane Depot',10,'MH04-20180056789',4.5,1230,18450.0,'ONLINE','MUM-North','2022-11-15'),
('mahesh.patil@urbanblack.in','9876543220','Mahesh','Patil',true,true,'Pune','Marathi','EMP-011','Shivajinagar Depot',1,'MH12-20160027890',4.7,990,14850.0,'ONLINE','Zone-A','2023-01-10'),
('nitin.gaikwad@urbanblack.in','9876543221','Nitin','Gaikwad',true,true,'Pune','Marathi','EMP-012','Kothrud Depot',2,'MH12-20200068901',4.6,870,13050.0,'ON_TRIP','Zone-B','2023-02-20'),
('amol.bhosale@urbanblack.in','9876543222','Amol','Bhosale',true,true,'Pune','Marathi','EMP-013','Pimpri Depot',13,'MH12-20190057012',4.8,1560,23400.0,'ONLINE','Zone-F','2023-03-05'),
('sanjay.wagh@urbanblack.in','9876543223','Sanjay','Wagh',true,true,'Pune','Marathi','EMP-014','Kondhwa Depot',14,'MH12-20180049123',4.5,1100,16500.0,'OFFLINE','Zone-G','2023-04-15'),
('krishna.more@urbanblack.in','9876543224','Krishna','More',true,true,'Pune','Marathi','EMP-015','Swargate Depot',15,'MH12-20170039234',4.7,1320,19800.0,'ONLINE','Zone-A','2023-05-01'),
('prashant.lokhande@urbanblack.in','9876543225','Prashant','Lokhande',true,true,'Pune','Marathi','EMP-016','Aundh Depot',16,'MH12-20200069345',4.6,780,11700.0,'ON_TRIP','Zone-B','2023-06-10'),
('dinesh.sawant@urbanblack.in','9876543226','Dinesh','Sawant',true,true,'Pune','Marathi','EMP-017','Wanowrie Depot',17,'MH12-20160028456',4.9,1720,25800.0,'ONLINE','Zone-G','2023-07-20'),
('rakesh.londhe@urbanblack.in','9876543227','Rakesh','Londhe',true,true,'Pune','Hindi','EMP-018','Deccan Depot',18,'MH12-20190058567',4.4,820,12300.0,'ONLINE','Zone-A','2023-08-01'),
('sunil.bagul@urbanblack.in','9876543228','Sunil','Bagul',true,true,'Mumbai','Marathi','EMP-019','Powai Depot',19,'MH03-20180050678',4.7,1450,21750.0,'ONLINE','MUM-East','2023-09-05'),
('mohan.kale@urbanblack.in','9876543229','Mohan','Kale',true,true,'Pune','Marathi','EMP-020','Katraj Depot',20,'MH12-20170040789',4.8,1280,19200.0,'ON_TRIP','Zone-H','2023-10-10'),
('vivek.randive@urbanblack.in','9876543230','Vivek','Randive',true,true,'Pune','Marathi','EMP-021','Magarpatta Depot',22,'MH12-20200070890',4.6,950,14250.0,'ONLINE','Zone-C','2023-11-01'),
('ashish.chavan@urbanblack.in','9876543231','Ashish','Chavan',true,true,'Pune','Marathi','EMP-022','Bhosari Depot',23,'MH12-20190059901',4.5,1080,16200.0,'ONLINE','Zone-F','2023-12-15'),
('rajendra.patole@urbanblack.in','9876543232','Rajendra','Patole',true,true,'Pune','Marathi','EMP-023','Talawade Depot',24,'MH12-20180051012',4.7,1190,17850.0,'ON_TRIP','Zone-D','2024-01-10'),
('hemant.shirke@urbanblack.in','9876543233','Hemant','Shirke',true,true,'Pune','Marathi','EMP-024','Shivajinagar Depot',1,'MH12-20160029123',4.6,1030,15450.0,'ONLINE','Zone-A','2024-02-01'),
('balaji.ware@urbanblack.in','9876543234','Balaji','Ware',true,true,'Pune','Marathi','EMP-025','Hinjewadi Depot',4,'MH14-20200071234',4.8,1640,24600.0,'ONLINE','Zone-D','2024-03-05'),
('yogesh.gavhane@urbanblack.in','9876543235','Yogesh','Gavhane',true,true,'Pune','Marathi','EMP-026','Viman Nagar Depot',5,'MH12-20190060345',4.7,1370,20550.0,'ONLINE','Zone-E','2024-04-01'),
('sudhir.pawar2@urbanblack.in','9876543236','Sudhir','Pawar',true,true,'Nagpur','Marathi','EMP-027','Sitabuldi Depot',11,'MH31-20180052456',4.5,860,12900.0,'ONLINE','NGP-Central','2024-04-15'),
('nilesh.joshi@urbanblack.in','9876543237','Nilesh','Joshi',true,true,'Pune','Marathi','EMP-028','Pimple Saudagar Depot',26,'MH12-20170042567',4.6,1010,15150.0,'ON_TRIP','Zone-B','2024-05-01'),
('ramesh.tupe@urbanblack.in','9876543238','Ramesh','Tupe',true,true,'Pune','Marathi','EMP-029','Mundhwa Depot',28,'MH12-20200072678',4.9,1580,23700.0,'ONLINE','Zone-C','2024-05-20'),
('sachin.nimbhorkar@urbanblack.in','9876543239','Sachin','Nimbhorkar',true,true,'Pune','Marathi','EMP-030','Koregaon Park Depot',29,'MH12-20190061789',4.7,1220,18300.0,'ONLINE','Zone-A','2024-06-01');

-- ============================================================
-- TABLE 6: rides
-- ============================================================
DROP TABLE IF EXISTS rides CASCADE;
CREATE TABLE rides (
    id                    TEXT             PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id               VARCHAR(36)      NOT NULL,
    driver_id             TEXT             REFERENCES driver(id),
    pickup_lat            DOUBLE PRECISION NOT NULL,
    pickup_lng            DOUBLE PRECISION NOT NULL,
    drop_lat              DOUBLE PRECISION NOT NULL,
    drop_lng              DOUBLE PRECISION NOT NULL,
    pickup_address        VARCHAR(255),
    drop_address          VARCHAR(255),
    notes                 TEXT,
    vehicle_type          VARCHAR(30),
    status                ride_status      NOT NULL DEFAULT 'REQUESTED',
    ride_km               DOUBLE PRECISION,
    duration_min          INTEGER,
    fare                  NUMERIC(10,2),
    requested_at          TIMESTAMPTZ,
    started_at            TIMESTAMPTZ,
    completed_at          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ        DEFAULT NOW(),
    updated_at            TIMESTAMPTZ        DEFAULT NOW(),
    otp                   VARCHAR(10),
    -- LIR additions
    optimized_pickup_lat  DOUBLE PRECISION,
    optimized_pickup_lng  DOUBLE PRECISION,
    pickup_pin_source     pin_source_enum,
    actual_start_lat      DOUBLE PRECISION,
    actual_start_lng      DOUBLE PRECISION,
    actual_end_lat        DOUBLE PRECISION,
    actual_end_lng        DOUBLE PRECISION
);

INSERT INTO rides (user_id,driver_id,pickup_lat,pickup_lng,drop_lat,drop_lng,pickup_address,drop_address,vehicle_type,status,ride_km,duration_min,fare,requested_at,started_at,completed_at,otp,optimized_pickup_lat,optimized_pickup_lng,pickup_pin_source,actual_start_lat,actual_start_lng,actual_end_lat,actual_end_lng)
SELECT
  'USER-' || LPAD(n::text,3,'0'),
  (SELECT id FROM driver ORDER BY random() LIMIT 1),
  ROUND((18.46 + random()*0.14)::numeric,6),
  ROUND((73.80 + random()*0.16)::numeric,6),
  ROUND((18.46 + random()*0.14)::numeric,6),
  ROUND((73.80 + random()*0.16)::numeric,6),
  'Pickup Location ' || n,
  'Drop Location ' || n,
  CASE (n%3) WHEN 0 THEN 'SEDAN' WHEN 1 THEN 'SUV' ELSE 'HATCHBACK' END,
  'COMPLETED',
  ROUND((3.5 + random()*18.5)::numeric,2),
  (8 + (random()*42)::int),
  ROUND((50 + random()*350)::numeric,2),
  NOW() - ((30 + n*8)::text || ' hours')::interval,
  NOW() - ((29 + n*8)::text || ' hours')::interval,
  NOW() - ((28 + n*8)::text || ' hours')::interval,
  LPAD((1000 + (random()*8999)::int)::text,4,'0'),
  ROUND((18.46 + random()*0.14)::numeric,6),
  ROUND((73.80 + random()*0.16)::numeric,6),
  CASE (n%4) WHEN 0 THEN 'MODEL_OPTIMIZED' WHEN 1 THEN 'VENUE_DATABASE' WHEN 2 THEN 'MODEL_OPTIMIZED' ELSE 'RAW_GEOCODE' END::pin_source_enum,
  ROUND((18.46 + random()*0.14)::numeric,6),
  ROUND((73.80 + random()*0.16)::numeric,6),
  ROUND((18.46 + random()*0.14)::numeric,6),
  ROUND((73.80 + random()*0.16)::numeric,6)
FROM generate_series(1,30) n;

-- ============================================================
-- TABLE 7: ride_routes
-- ============================================================
DROP TABLE IF EXISTS ride_routes CASCADE;
CREATE TABLE ride_routes (
    id                  TEXT             PRIMARY KEY DEFAULT gen_random_uuid()::text,
    ride_id             TEXT             NOT NULL REFERENCES rides(id),
    approach_polyline   TEXT,
    ride_polyline       TEXT,
    approach_km         DOUBLE PRECISION,
    ride_km             DOUBLE PRECISION,
    -- LIR additions
    actual_polyline     TEXT,
    actual_ride_km      DOUBLE PRECISION,
    consistency_score   DOUBLE PRECISION,
    consistency_flag    consistency_flag,
    routing_engine      VARCHAR(50),
    traffic_snapshot_at TIMESTAMPTZ,
    waypoints_json      TEXT,
    computed_at         TIMESTAMPTZ
);

INSERT INTO ride_routes (ride_id,approach_polyline,ride_polyline,approach_km,ride_km,actual_polyline,actual_ride_km,consistency_score,consistency_flag,routing_engine,traffic_snapshot_at,computed_at)
SELECT
  r.id,
  'encoded_approach_' || n,
  'encoded_ride_polyline_' || n,
  ROUND((0.5 + random()*2.5)::numeric,2),
  r.ride_km,
  'encoded_actual_polyline_' || n,
  ROUND((r.ride_km * (0.95 + random()*0.12))::numeric,2),
  ROUND((0.72 + random()*0.27)::numeric,4),
  CASE WHEN random() < 0.75 THEN 'CONSISTENT'
       WHEN random() < 0.85 THEN 'MINOR_DEVIATION'
       WHEN random() < 0.95 THEN 'MAJOR_DEVIATION'
       ELSE 'FRAUD_SUSPECT' END::consistency_flag,
  CASE (n%2) WHEN 0 THEN 'OSRM_v5.27' ELSE 'VALHALLA_v3.4' END,
  r.started_at,
  r.requested_at + interval '2 seconds'
FROM rides r, generate_series(1,30) n
WHERE r.id = (SELECT id FROM rides ORDER BY created_at OFFSET (n-1) LIMIT 1)
LIMIT 30;

-- ============================================================
-- TABLE 8: driver_locations
-- ============================================================
DROP TABLE IF EXISTS driver_locations CASCADE;
CREATE TABLE driver_locations (
    id               TEXT             PRIMARY KEY DEFAULT gen_random_uuid()::text,
    driver_id        TEXT             NOT NULL REFERENCES driver(id),
    lat              DOUBLE PRECISION NOT NULL,
    lng              DOUBLE PRECISION NOT NULL,
    bearing          DOUBLE PRECISION,
    speed_kmh        DOUBLE PRECISION,
    updated_at       TIMESTAMPTZ        NOT NULL DEFAULT NOW(),
    -- LIR additions
    smoothed_lat     DOUBLE PRECISION,
    smoothed_lng     DOUBLE PRECISION,
    accuracy_meters  DOUBLE PRECISION,
    altitude         DOUBLE PRECISION,
    gps_source       gps_source_enum,
    accel_x          DOUBLE PRECISION,
    accel_y          DOUBLE PRECISION,
    accel_z          DOUBLE PRECISION,
    is_on_trip       BOOLEAN          NOT NULL DEFAULT FALSE,
    ride_id          TEXT             REFERENCES rides(id),
    published_to_kafka BOOLEAN        NOT NULL DEFAULT FALSE
);

INSERT INTO driver_locations (driver_id,lat,lng,bearing,speed_kmh,updated_at,smoothed_lat,smoothed_lng,accuracy_meters,altitude,gps_source,accel_x,accel_y,accel_z,is_on_trip,published_to_kafka)
SELECT
  d.id,
  ROUND((18.46 + random()*0.14)::numeric,7),
  ROUND((73.80 + random()*0.16)::numeric,7),
  ROUND((random()*360)::numeric,1),
  ROUND((random()*60)::numeric,1),
  NOW() - ((n*2)::text || ' minutes')::interval,
  ROUND((18.46 + random()*0.14)::numeric,7),
  ROUND((73.80 + random()*0.16)::numeric,7),
  ROUND((4 + random()*16)::numeric,1),
  ROUND((555 + random()*25)::numeric,1),
  CASE (n%4) WHEN 0 THEN 'GPS' WHEN 1 THEN 'FUSED' WHEN 2 THEN 'GPS' ELSE 'NETWORK' END::gps_source_enum,
  ROUND((-2 + random()*4)::numeric,4),
  ROUND((-2 + random()*4)::numeric,4),
  ROUND((9.6 + random()*0.4)::numeric,4),
  (n%3 = 0),
  true
FROM driver d, generate_series(1,30) n
WHERE d.id = (SELECT id FROM driver ORDER BY date_of_joining, id OFFSET (n-1) LIMIT 1)
LIMIT 30;

-- ============================================================
-- TABLE 9: driver_shifts  (ride-service shift KM accounting)
-- ============================================================
DROP TABLE IF EXISTS driver_shifts CASCADE;
CREATE TABLE driver_shifts (
    id                    TEXT               PRIMARY KEY DEFAULT gen_random_uuid()::text,
    driver_id             TEXT             NOT NULL REFERENCES driver(id),
    shift_ref             VARCHAR(50)         UNIQUE,
    shift_start           TIMESTAMPTZ           NOT NULL,
    shift_end             TIMESTAMPTZ,
    status                driver_status_enum  NOT NULL,
    goal_km               DOUBLE PRECISION    NOT NULL DEFAULT 0,
    goal_km_reached       DOUBLE PRECISION    NOT NULL DEFAULT 0,
    total_ride_km         DOUBLE PRECISION    NOT NULL DEFAULT 0,
    total_dead_km         DOUBLE PRECISION    NOT NULL DEFAULT 0,
    total_free_roaming_km DOUBLE PRECISION    NOT NULL DEFAULT 0,
    total_km              DOUBLE PRECISION    NOT NULL DEFAULT 0,
    -- LIR additions
    last_known_lat        DOUBLE PRECISION,
    last_known_lng        DOUBLE PRECISION,
    gps_sample_mode       gps_mode_enum,
    tracking_session_id   VARCHAR(36)
);

INSERT INTO driver_shifts (driver_id,shift_ref,shift_start,shift_end,status,goal_km,goal_km_reached,total_ride_km,total_dead_km,total_free_roaming_km,total_km,last_known_lat,last_known_lng,gps_sample_mode,tracking_session_id)
SELECT
  d.id,
  'SHIFT-' || TO_CHAR(NOW() - ((n*24)::text || ' hours')::interval,'YYYYMMDD') || '-' || d.employee_id,
  NOW() - ((n*24 + 8)::text || ' hours')::interval,
  NOW() - ((n*24)::text || ' hours')::interval,
  'ONLINE',
  150.0,
  ROUND((80 + random()*80)::numeric,2),
  ROUND((80 + random()*80)::numeric,2),
  ROUND((10 + random()*20)::numeric,2),
  ROUND((5 + random()*15)::numeric,2),
  ROUND((100 + random()*110)::numeric,2),
  ROUND((18.46 + random()*0.14)::numeric,7),
  ROUND((73.80 + random()*0.16)::numeric,7),
  'ACTIVE_TRIP',
  gen_random_uuid()::text
FROM driver d, generate_series(1,30) n
WHERE d.id = (SELECT id FROM driver ORDER BY date_of_joining, id OFFSET (n-1) LIMIT 1)
LIMIT 30;

-- ============================================================
-- TABLE 10: driver_km_log
-- ============================================================
DROP TABLE IF EXISTS driver_km_log CASCADE;
CREATE TABLE driver_km_log (
    id          TEXT             PRIMARY KEY DEFAULT gen_random_uuid()::text,
    driver_id   TEXT             NOT NULL REFERENCES driver(id),
    shift_id    TEXT             NOT NULL REFERENCES driver_shifts(id),
    category    km_category      NOT NULL,
    km          DOUBLE PRECISION NOT NULL,
    ride_id     TEXT             REFERENCES rides(id),
    recorded_at TIMESTAMPTZ        NOT NULL DEFAULT NOW(),
    -- LIR additions
    start_lat   DOUBLE PRECISION,
    start_lng   DOUBLE PRECISION,
    end_lat     DOUBLE PRECISION,
    end_lng     DOUBLE PRECISION
);

INSERT INTO driver_km_log (driver_id,shift_id,category,km,ride_id,recorded_at,start_lat,start_lng,end_lat,end_lng)
SELECT
  ds.driver_id,
  ds.id,
  CASE (n%3) WHEN 0 THEN 'RIDE' WHEN 1 THEN 'DEAD' ELSE 'FREE_ROAMING' END::km_category,
  ROUND((1.5 + random()*18)::numeric,3),
  CASE WHEN n%3=0 THEN (SELECT id FROM rides ORDER BY random() LIMIT 1) ELSE NULL END,
  ds.shift_start + ((n*25)::text || ' minutes')::interval,
  ROUND((18.46 + random()*0.14)::numeric,7),
  ROUND((73.80 + random()*0.16)::numeric,7),
  ROUND((18.46 + random()*0.14)::numeric,7),
  ROUND((73.80 + random()*0.16)::numeric,7)
FROM driver_shifts ds, generate_series(1,30) n
WHERE ds.id = (SELECT id FROM driver_shifts ORDER BY shift_start OFFSET (n-1) LIMIT 1)
LIMIT 30;

-- ============================================================
-- TABLE 11: shifts  (driver-service)
-- ============================================================
DROP TABLE IF EXISTS shifts CASCADE;
CREATE TABLE shifts (
    id                        TEXT               PRIMARY KEY DEFAULT gen_random_uuid()::text,
    driver_id                 TEXT             REFERENCES driver(id),
    status                    shift_status_enum,
    availability              driver_avail,
    clock_in_time             TIMESTAMPTZ,
    clock_in_latitude         DOUBLE PRECISION,
    clock_in_longitude        DOUBLE PRECISION,
    clock_out_time            TIMESTAMPTZ,
    clock_out_latitude        DOUBLE PRECISION,
    clock_out_longitude       DOUBLE PRECISION,
    last_online_time          TIMESTAMPTZ,
    last_offline_time         TIMESTAMPTZ,
    accumulated_active_seconds BIGINT           DEFAULT 0,
    total_active_minutes      BIGINT            DEFAULT 0,
    starting_odometer         INTEGER,
    ending_odometer           INTEGER,
    fuel_level_at_start       fuel_level_enum,
    fuel_level_at_end         fuel_level_enum,
    vehicle_condition         vehicle_cond_enum,
    -- LIR additions
    last_known_lat            DOUBLE PRECISION,
    last_known_lng            DOUBLE PRECISION,
    gps_sample_mode           gps_mode_enum,
    tracking_session_id       VARCHAR(36)
);

INSERT INTO shifts (driver_id,status,availability,clock_in_time,clock_in_latitude,clock_in_longitude,clock_out_time,clock_out_latitude,clock_out_longitude,last_online_time,accumulated_active_seconds,total_active_minutes,starting_odometer,ending_odometer,fuel_level_at_start,fuel_level_at_end,vehicle_condition,last_known_lat,last_known_lng,gps_sample_mode,tracking_session_id)
SELECT
  d.id,
  'COMPLETED',
  'AVAILABLE',
  NOW() - ((n*24 + 9)::text || ' hours')::interval,
  ROUND((18.46 + random()*0.14)::numeric,7),
  ROUND((73.80 + random()*0.16)::numeric,7),
  NOW() - ((n*24 + 1)::text || ' hours')::interval,
  ROUND((18.46 + random()*0.14)::numeric,7),
  ROUND((73.80 + random()*0.16)::numeric,7),
  NOW() - ((n*24 + 1)::text || ' hours')::interval,
  28800 + (random()*3600)::int,
  480 + (random()*60)::int,
  45000 + (n*120),
  45000 + (n*120) + 100 + (random()*100)::int,
  CASE (n%5) WHEN 0 THEN 'FULL' WHEN 1 THEN 'THREE_QUARTER' WHEN 2 THEN 'HALF' WHEN 3 THEN 'FULL' ELSE 'THREE_QUARTER' END::fuel_level_enum,
  CASE (n%5) WHEN 0 THEN 'THREE_QUARTER' WHEN 1 THEN 'HALF' WHEN 2 THEN 'LOW' WHEN 3 THEN 'HALF' ELSE 'LOW' END::fuel_level_enum,
  CASE (n%3) WHEN 0 THEN 'EXCELLENT' WHEN 1 THEN 'GOOD' ELSE 'FAIR' END::vehicle_cond_enum,
  ROUND((18.46 + random()*0.14)::numeric,7),
  ROUND((73.80 + random()*0.16)::numeric,7),
  CASE (n%3) WHEN 0 THEN 'ACTIVE_TRIP' WHEN 1 THEN 'IDLE' ELSE 'OFFLINE' END::gps_mode_enum,
  gen_random_uuid()::text
FROM driver d, generate_series(1,30) n
WHERE d.id = (SELECT id FROM driver ORDER BY date_of_joining, id OFFSET (n-1) LIMIT 1)
LIMIT 30;

-- ============================================================
-- TABLE 12: lir_trip_gps_trail
-- ============================================================
DROP TABLE IF EXISTS lir_trip_gps_trail CASCADE;
CREATE TABLE lir_trip_gps_trail (
    id           TEXT             PRIMARY KEY DEFAULT gen_random_uuid()::text,
    ride_id      TEXT             NOT NULL REFERENCES rides(id),
    driver_id    TEXT             NOT NULL REFERENCES driver(id),
    sequence_no  INTEGER          NOT NULL,
    lat          DOUBLE PRECISION NOT NULL,
    lng          DOUBLE PRECISION NOT NULL,
    speed_kmh    DOUBLE PRECISION,
    bearing      DOUBLE PRECISION,
    recorded_at  TIMESTAMPTZ        NOT NULL,
    segment_km   DOUBLE PRECISION,
    UNIQUE (ride_id, sequence_no)
);

-- 30 trail points (one per ride, simulating mid-trip GPS fix)
INSERT INTO lir_trip_gps_trail (ride_id,driver_id,sequence_no,lat,lng,speed_kmh,bearing,recorded_at,segment_km)
SELECT
  r.id,
  r.driver_id,
  n,
  ROUND((r.pickup_lat + (n * 0.0008 * (random()-0.3)))::numeric,7),
  ROUND((r.pickup_lng + (n * 0.0010 * (random()-0.3)))::numeric,7),
  ROUND((15 + random()*45)::numeric,1),
  ROUND((random()*360)::numeric,1),
  r.started_at + ((n*45)::text || ' seconds')::interval,
  ROUND((0.05 + random()*0.35)::numeric,4)
FROM rides r, generate_series(1,30) n
WHERE r.id = (SELECT id FROM rides ORDER BY created_at OFFSET (n-1) LIMIT 1)
  AND r.driver_id IS NOT NULL
LIMIT 30;

-- ============================================================
-- TABLE 13: lir_geo_anomaly_events
-- ============================================================
DROP TABLE IF EXISTS lir_geo_anomaly_events CASCADE;
CREATE TABLE lir_geo_anomaly_events (
    id               TEXT               PRIMARY KEY DEFAULT gen_random_uuid()::text,
    ride_id          TEXT             NOT NULL REFERENCES rides(id),
    driver_id        TEXT             NOT NULL REFERENCES driver(id),
    anomaly_type     anomaly_type_enum  NOT NULL,
    severity         anomaly_severity   NOT NULL,
    anomaly_score    DOUBLE PRECISION   NOT NULL,
    deviation_meters DOUBLE PRECISION,
    stop_duration_sec INTEGER,
    lat              DOUBLE PRECISION   NOT NULL,
    lng              DOUBLE PRECISION   NOT NULL,
    detected_at      TIMESTAMPTZ          NOT NULL,
    driver_alerted_at TIMESTAMPTZ,
    resolved_at      TIMESTAMPTZ,
    escalated_to_sos BOOLEAN            NOT NULL DEFAULT FALSE,
    is_shadow_mode   BOOLEAN            NOT NULL DEFAULT FALSE,
    model_version    VARCHAR(30)
);

INSERT INTO lir_geo_anomaly_events (ride_id,driver_id,anomaly_type,severity,anomaly_score,deviation_meters,stop_duration_sec,lat,lng,detected_at,driver_alerted_at,resolved_at,escalated_to_sos,is_shadow_mode,model_version)
SELECT
  r.id,
  r.driver_id,
  CASE (n%5) WHEN 0 THEN 'DETOUR' WHEN 1 THEN 'PROLONGED_STOP' WHEN 2 THEN 'SPEED_ANOMALY' WHEN 3 THEN 'GEOFENCE_VIOLATION' ELSE 'GPS_SPOOF_SUSPECTED' END::anomaly_type_enum,
  CASE WHEN random()<0.4 THEN 'LOW' WHEN random()<0.7 THEN 'MEDIUM' WHEN random()<0.92 THEN 'HIGH' ELSE 'CRITICAL' END::anomaly_severity,
  ROUND((0.45 + random()*0.54)::numeric,4),
  CASE WHEN n%5=0 THEN ROUND((120 + random()*680)::numeric,1) ELSE NULL END,
  CASE WHEN n%5=1 THEN (180 + (random()*600)::int) ELSE NULL END,
  ROUND((18.46 + random()*0.14)::numeric,7),
  ROUND((73.80 + random()*0.16)::numeric,7),
  r.started_at + ((10 + n*3)::text || ' minutes')::interval,
  r.started_at + ((10 + n*3 + 1)::text || ' minutes')::interval,
  CASE WHEN random()>0.3 THEN r.started_at + ((10 + n*3 + 8)::text || ' minutes')::interval ELSE NULL END,
  random() < 0.06,
  random() < 0.15,
  CASE (n%2) WHEN 0 THEN 'isolation_forest_v2.1' ELSE 'lstm_autoenc_v1.4' END
FROM rides r, generate_series(1,30) n
WHERE r.id = (SELECT id FROM rides ORDER BY created_at OFFSET (n-1) LIMIT 1)
  AND r.driver_id IS NOT NULL
LIMIT 30;

-- ============================================================
-- TABLE 14: lir_pickup_optimizations
-- ============================================================
DROP TABLE IF EXISTS lir_pickup_optimizations CASCADE;
CREATE TABLE lir_pickup_optimizations (
    id                TEXT             PRIMARY KEY DEFAULT gen_random_uuid()::text,
    ride_id           TEXT             NOT NULL REFERENCES rides(id),
    raw_lat           DOUBLE PRECISION NOT NULL,
    raw_lng           DOUBLE PRECISION NOT NULL,
    optimized_lat     DOUBLE PRECISION,
    optimized_lng     DOUBLE PRECISION,
    pin_source        pin_source_enum  NOT NULL,
    venue_id          TEXT             REFERENCES lir_venue_pickup_points(id),
    model_score       DOUBLE PRECISION,
    candidate_count   INTEGER,
    estimated_wait_sec INTEGER,
    actual_wait_sec   INTEGER,
    driver_rating     INTEGER          CHECK (driver_rating BETWEEN 1 AND 5),
    created_at        TIMESTAMPTZ        NOT NULL DEFAULT NOW()
);

INSERT INTO lir_pickup_optimizations (ride_id,raw_lat,raw_lng,optimized_lat,optimized_lng,pin_source,model_score,candidate_count,estimated_wait_sec,actual_wait_sec,driver_rating,created_at)
SELECT
  r.id,
  r.pickup_lat,
  r.pickup_lng,
  COALESCE(r.optimized_pickup_lat, r.pickup_lat + (random()-0.5)*0.0015),
  COALESCE(r.optimized_pickup_lng, r.pickup_lng + (random()-0.5)*0.0015),
  COALESCE(r.pickup_pin_source, 'MODEL_OPTIMIZED')::pin_source_enum,
  ROUND((0.60 + random()*0.39)::numeric,4),
  (3 + (random()*7)::int),
  (90 + (random()*180)::int),
  (80 + (random()*200)::int),
  (ARRAY[3,4,4,4,5,5,5,3,4,5])[1 + FLOOR(random()*10)::int],
  r.created_at
FROM rides r, generate_series(1,30) n
WHERE r.id = (SELECT id FROM rides ORDER BY created_at OFFSET (n-1) LIMIT 1)
LIMIT 30;

-- ============================================================
-- INDEXES for LIR model performance
-- ============================================================

-- ride_routes
CREATE INDEX idx_ride_routes_ride_id ON ride_routes(ride_id);
CREATE INDEX idx_ride_routes_consistency_flag ON ride_routes(consistency_flag) WHERE consistency_flag IN ('MAJOR_DEVIATION','FRAUD_SUSPECT');

-- rides
CREATE INDEX idx_rides_driver_id ON rides(driver_id);
CREATE INDEX idx_rides_status ON rides(status);
CREATE INDEX idx_rides_completed_at ON rides(completed_at);

-- driver_locations
CREATE INDEX idx_driver_locations_driver_id ON driver_locations(driver_id);
CREATE INDEX idx_driver_locations_updated_at ON driver_locations(updated_at DESC);
CREATE INDEX idx_driver_locations_ride_id ON driver_locations(ride_id) WHERE ride_id IS NOT NULL;

-- lir_trip_gps_trail
CREATE INDEX idx_gps_trail_ride_id_seq ON lir_trip_gps_trail(ride_id, sequence_no);
CREATE INDEX idx_gps_trail_driver_id ON lir_trip_gps_trail(driver_id);
CREATE INDEX idx_gps_trail_recorded_at ON lir_trip_gps_trail(recorded_at DESC);

-- lir_geo_anomaly_events
CREATE INDEX idx_anomaly_ride_id ON lir_geo_anomaly_events(ride_id);
CREATE INDEX idx_anomaly_driver_id ON lir_geo_anomaly_events(driver_id);
CREATE INDEX idx_anomaly_type_severity ON lir_geo_anomaly_events(anomaly_type, severity);
CREATE INDEX idx_anomaly_detected_at ON lir_geo_anomaly_events(detected_at DESC);
CREATE INDEX idx_anomaly_sos ON lir_geo_anomaly_events(escalated_to_sos) WHERE escalated_to_sos = TRUE;

-- lir_pickup_optimizations
CREATE INDEX idx_pickup_opt_ride_id ON lir_pickup_optimizations(ride_id);
CREATE INDEX idx_pickup_opt_pin_source ON lir_pickup_optimizations(pin_source);

-- lir_traffic_zones
CREATE INDEX idx_traffic_zones_type ON lir_traffic_zones(zone_type);
CREATE INDEX idx_traffic_zones_active ON lir_traffic_zones(is_active) WHERE is_active = TRUE;

-- depots
CREATE INDEX idx_depots_zone ON depots(zone);
CREATE INDEX idx_depots_active ON depots(is_active) WHERE is_active = TRUE;

-- Driver
CREATE INDEX idx_driver_status ON driver(status);
CREATE INDEX idx_driver_depot_id ON driver(depot_id);

-- shifts
CREATE INDEX idx_shifts_driver_id ON shifts(driver_id);
CREATE INDEX idx_shifts_availability ON shifts(availability);

-- driver_shifts
CREATE INDEX idx_driver_shifts_driver_id ON driver_shifts(driver_id);

-- driver_km_log
CREATE INDEX idx_km_log_driver_shift ON driver_km_log(driver_id, shift_id);
CREATE INDEX idx_km_log_ride_id ON driver_km_log(ride_id) WHERE ride_id IS NOT NULL;

-- ============================================================
-- SUMMARY VIEW  (useful for quick model training data checks)
-- ============================================================
CREATE OR REPLACE VIEW lir_training_summary AS
SELECT
  r.id                         AS ride_id,
  r.status,
  r.ride_km                    AS planned_km,
  rr.actual_ride_km,
  rr.consistency_score,
  rr.consistency_flag,
  rr.routing_engine,
  po.pin_source,
  po.estimated_wait_sec,
  po.actual_wait_sec,
  po.driver_rating,
  COUNT(gt.id)                 AS gps_trail_points,
  COUNT(ae.id)                 AS anomaly_events,
  MAX(ae.severity::text)       AS max_anomaly_severity,
  BOOL_OR(ae.escalated_to_sos) AS had_sos_escalation
FROM rides r
LEFT JOIN ride_routes            rr ON rr.ride_id  = r.id
LEFT JOIN lir_pickup_optimizations po ON po.ride_id = r.id
LEFT JOIN lir_trip_gps_trail     gt ON gt.ride_id  = r.id
LEFT JOIN lir_geo_anomaly_events ae ON ae.ride_id  = r.id
GROUP BY r.id, r.status, r.ride_km, rr.actual_ride_km,
         rr.consistency_score, rr.consistency_flag, rr.routing_engine,
         po.pin_source, po.estimated_wait_sec, po.actual_wait_sec, po.driver_rating;

-- ============================================================
-- END OF FILE
-- ============================================================
