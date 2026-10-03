-- ============================================================
-- Seamline — MySQL schema + seed data
--
-- A standalone port of the schema Hibernate generates from
-- src/main/java/com/seamline/domain/*.java, plus the same demo
-- factory DataSeeder.java creates on first boot (2 styles, 4 lines,
-- a 14-operator roster with skill matrix, 4 sign-in accounts, 4
-- orders). shift_runs / shift_run_hourly / station_results are
-- created empty — those fill in the moment someone signs in and
-- the app runs its first simulation, real or seeded.
--
-- Requires MySQL 8+ (or MariaDB 10.3+).
-- Run with:  mysql -u root -p < seamline_mysql.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS seamline CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE seamline;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS station_results;
DROP TABLE IF EXISTS shift_run_hourly;
DROP TABLE IF EXISTS shift_runs;
DROP TABLE IF EXISTS operator_skills;
DROP TABLE IF EXISTS operators;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS operation_predecessors;
DROP TABLE IF EXISTS operations;
DROP TABLE IF EXISTS daily_checkins;
DROP TABLE IF EXISTS operator_requests;
DROP TABLE IF EXISTS approval_requests;
DROP TABLE IF EXISTS chat_messages;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS production_lines;
DROP TABLE IF EXISTS styles;
SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------
-- Schema
-- ------------------------------------------------------------

CREATE TABLE styles (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  code VARCHAR(40) NOT NULL,
  name VARCHAR(120) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_styles_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE production_lines (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  code VARCHAR(40) NOT NULL,
  unit_name VARCHAR(60),
  shift_minutes INT NOT NULL DEFAULT 480,
  target_rate_per_hour INT NOT NULL DEFAULT 78,
  current_style_id BIGINT,
  PRIMARY KEY (id),
  UNIQUE KEY uk_lines_code (code),
  CONSTRAINT fk_lines_style FOREIGN KEY (current_style_id) REFERENCES styles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE employees (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  employee_id VARCHAR(30) NOT NULL,
  email VARCHAR(160) NOT NULL,
  phone VARCHAR(30),
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(120) NOT NULL,
  job_title VARCHAR(120),
  role VARCHAR(40) NOT NULL,
  assigned_line_id BIGINT,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  reset_otp_hash VARCHAR(100),
  reset_otp_expires_at DATETIME(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_employees_employee_id (employee_id),
  UNIQUE KEY uk_employees_email (email),
  UNIQUE KEY uk_employees_phone (phone),
  CONSTRAINT fk_employees_line FOREIGN KEY (assigned_line_id) REFERENCES production_lines(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Group channel (recipient_id NULL) and 1:1 direct messages, open to every role.
CREATE TABLE chat_messages (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  sender_id BIGINT NOT NULL,
  recipient_id BIGINT,
  body VARCHAR(2000) NOT NULL,
  read_at DATETIME(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_chat_sender FOREIGN KEY (sender_id) REFERENCES employees(id),
  CONSTRAINT fk_chat_recipient FOREIGN KEY (recipient_id) REFERENCES employees(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Problem reports, time-off, shift-swap and other asks any employee can raise;
-- a supervisor or admin closes each one out with an optional note.
CREATE TABLE operator_requests (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  requester_id BIGINT NOT NULL,
  type VARCHAR(30) NOT NULL,
  description VARCHAR(1000) NOT NULL,
  resolved BOOLEAN NOT NULL DEFAULT FALSE,
  resolution_note VARCHAR(1000),
  resolved_by_id BIGINT,
  resolved_at DATETIME(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_opreq_requester FOREIGN KEY (requester_id) REFERENCES employees(id),
  CONSTRAINT fk_opreq_resolver FOREIGN KEY (resolved_by_id) REFERENCES employees(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Line-plan changes an Industrial Engineer proposes; a Supervisor or Admin
-- signs off on each one (pending/approved/rejected) before it's real.
CREATE TABLE approval_requests (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  proposer_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  projected_impact VARCHAR(300) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  decided_by_id BIGINT,
  decided_at DATETIME(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_appr_proposer FOREIGN KEY (proposer_id) REFERENCES employees(id),
  CONSTRAINT fk_appr_decider FOREIGN KEY (decided_by_id) REFERENCES employees(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- One row per employee per day they tapped "I'm here and at my station."
CREATE TABLE daily_checkins (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  employee_id BIGINT NOT NULL,
  check_date DATE NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_checkin_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
  CONSTRAINT uq_checkin_employee_date UNIQUE (employee_id, check_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE operations (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  code VARCHAR(20) NOT NULL,
  name VARCHAR(120) NOT NULL,
  smv DOUBLE NOT NULL,
  machine_type VARCHAR(30) NOT NULL,
  min_grade INT NOT NULL,
  sequence_no INT NOT NULL,
  style_id BIGINT,
  PRIMARY KEY (id),
  CONSTRAINT fk_operations_style FOREIGN KEY (style_id) REFERENCES styles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Self-referencing many-to-many: the precedence graph a style's operations form.
CREATE TABLE operation_predecessors (
  operation_id BIGINT NOT NULL,
  predecessor_id BIGINT NOT NULL,
  PRIMARY KEY (operation_id, predecessor_id),
  CONSTRAINT fk_pred_operation FOREIGN KEY (operation_id) REFERENCES operations(id),
  CONSTRAINT fk_pred_predecessor FOREIGN KEY (predecessor_id) REFERENCES operations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- The floor roster used by the balancer/simulator — a separate concept from
-- `employees` (who signs in). Nothing currently links an OPERATOR-role
-- employee to their own row here.
CREATE TABLE operators (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  code VARCHAR(20) NOT NULL,
  name VARCHAR(120) NOT NULL,
  grade INT NOT NULL,
  default_efficiency DOUBLE NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_operators_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE operator_skills (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  operator_id BIGINT NOT NULL,
  machine_type VARCHAR(30) NOT NULL,
  efficiency DOUBLE NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_operator_machine (operator_id, machine_type),
  CONSTRAINT fk_skills_operator FOREIGN KEY (operator_id) REFERENCES operators(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE orders (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  order_code VARCHAR(40) NOT NULL,
  buyer VARCHAR(120) NOT NULL,
  quantity INT NOT NULL,
  completed_quantity INT NOT NULL,
  due_date DATE NOT NULL,
  style_id BIGINT,
  PRIMARY KEY (id),
  UNIQUE KEY uk_orders_code (order_code),
  CONSTRAINT fk_orders_style FOREIGN KEY (style_id) REFERENCES styles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE shift_runs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  line_id BIGINT NOT NULL,
  style_id BIGINT NOT NULL,
  triggered_by_id BIGINT,
  algorithm VARCHAR(40) NOT NULL,
  team_size INT NOT NULL,
  buffer_per_station INT NOT NULL,
  variability DOUBLE NOT NULL,
  random_seed BIGINT NOT NULL,
  run_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  pieces_produced INT NOT NULL,
  output_rate_per_hour DOUBLE NOT NULL,
  line_efficiency DOUBLE NOT NULL,
  balance_loss DOUBLE NOT NULL,
  cycle_time_minutes DOUBLE NOT NULL,
  working_minutes DOUBLE NOT NULL,
  blocked_minutes DOUBLE NOT NULL,
  starved_minutes DOUBLE NOT NULL,
  idle_minutes DOUBLE NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_runs_line FOREIGN KEY (line_id) REFERENCES production_lines(id),
  CONSTRAINT fk_runs_style FOREIGN KEY (style_id) REFERENCES styles(id),
  CONSTRAINT fk_runs_employee FOREIGN KEY (triggered_by_id) REFERENCES employees(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE shift_run_hourly (
  run_id BIGINT NOT NULL,
  hour_index INT NOT NULL,
  pieces INT,
  PRIMARY KEY (run_id, hour_index),
  CONSTRAINT fk_hourly_run FOREIGN KEY (run_id) REFERENCES shift_runs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE station_results (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  run_id BIGINT NOT NULL,
  station_index INT NOT NULL,
  station_code VARCHAR(20) NOT NULL,
  machine_type VARCHAR(30) NOT NULL,
  operator_code VARCHAR(20),
  operator_name VARCHAR(120) NOT NULL,
  operation_codes VARCHAR(200) NOT NULL,
  minutes_per_piece DOUBLE NOT NULL,
  utilisation DOUBLE NOT NULL,
  blocked_minutes DOUBLE NOT NULL,
  starved_minutes DOUBLE NOT NULL,
  pieces_done INT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'HEALTHY',
  PRIMARY KEY (id),
  CONSTRAINT fk_station_run FOREIGN KEY (run_id) REFERENCES shift_runs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Seed data — mirrors DataSeeder.java
-- ------------------------------------------------------------

INSERT INTO styles (id, code, name) VALUES
 (1, 'POLO-MEN', 'Men''s Polo Shirt'),
 (2, 'TEE-BASIC', 'Basic Crew Tee');

INSERT INTO production_lines (id, code, unit_name, shift_minutes, target_rate_per_hour, current_style_id) VALUES
 (1, 'Line-07', 'Unit 2', 480, 78, 1),
 (2, 'Line-03', 'Unit 1', 480, 92, 2),
 (3, 'Line-11', 'Unit 2', 480, 71, 1),
 (4, 'Line-14', 'Unit 3', 480, 88, 2);

-- password for every account below is seamline123
-- (BCrypt hash generated with the app's own BCryptPasswordEncoder and verified to match)
-- Sign in with the email address below, not the employee ID.
INSERT INTO employees (id, employee_id, email, password_hash, full_name, job_title, role, assigned_line_id, enabled) VALUES
 (1, 'IE-2214',  'farhana.akter@seamline.com', '$2a$10$wE.HND3pFQkcDfg7fMuFn.z.fQ5cQ/vzkpeY8koGXHbPSy.0maQb6', 'Farhana Akter',        'Industrial Engineer',   'INDUSTRIAL_ENGINEER', 1, TRUE),
 (2, 'SUP-1180', 'kamal.hossain@seamline.com', '$2a$10$wE.HND3pFQkcDfg7fMuFn.z.fQ5cQ/vzkpeY8koGXHbPSy.0maQb6', 'Kamal Hossain',        'Production Supervisor', 'SUPERVISOR',           1, TRUE),
 (3, 'ADM-0001', 'rezaul.karim@seamline.com',  '$2a$10$wE.HND3pFQkcDfg7fMuFn.z.fQ5cQ/vzkpeY8koGXHbPSy.0maQb6', 'S. M. Rezaul Karim',   'Production Director',   'ADMIN',                1, TRUE),
 (4, 'MO-2201',  'rahima.begum@seamline.com',  '$2a$10$wE.HND3pFQkcDfg7fMuFn.z.fQ5cQ/vzkpeY8koGXHbPSy.0maQb6', 'Rahima Begum',         'Machine Operator',      'OPERATOR',              1, TRUE);

-- Line-plan proposals: two pending, one approved and one rejected by Kamal
-- (id 2), all raised by Farhana (id 1) — same demo shape the Approvals
-- screen used to fake in the browser before it had a backend.
INSERT INTO approval_requests (id, proposer_id, title, projected_impact, status, decided_by_id, decided_at) VALUES
 (1, 1, 'Increase buffer at ST4 to 5', '+3.1% projected output', 'PENDING', NULL, NULL),
 (2, 1, 'Switch to Greedy topological fill algorithm', '-1.4% projected output, faster balance', 'PENDING', NULL, NULL),
 (3, 1, 'Move Shirin (OP02) to ST9 (overlock)', 'Reduces bottleneck at ST9', 'APPROVED', 2, NOW()),
 (4, 1, 'Reduce team size to 11 for low-volume order', '-9% output, -3 operators', 'REJECTED', 2, NOW());

-- Operations — Men's Polo Shirt (style_id 1), ids 1-16, in the bulletin's sequence order
INSERT INTO operations (id, code, name, smv, machine_type, min_grade, sequence_no, style_id) VALUES
 (1,  'C01', 'Cut panel check',      0.30, 'MANUAL',        1, 1,  1),
 (2,  'S01', 'Shoulder join',        0.55, 'OVERLOCK',      3, 2,  1),
 (3,  'S02', 'Shoulder topstitch',   0.45, 'PLAIN',         3, 3,  1),
 (4,  'P01', 'Placket make',         0.95, 'PLAIN',         4, 4,  1),
 (5,  'P02', 'Placket attach',       0.85, 'PLAIN',         4, 5,  1),
 (6,  'B01', 'Collar make',          0.80, 'PLAIN',         4, 6,  1),
 (7,  'B02', 'Collar join',          0.75, 'PLAIN',         4, 7,  1),
 (8,  'L01', 'Sleeve attach',        0.90, 'OVERLOCK',      4, 8,  1),
 (9,  'L02', 'Armhole topstitch',    0.60, 'FLATLOCK',      3, 9,  1),
 (10, 'S03', 'Side seam',            1.10, 'OVERLOCK',      4, 10, 1),
 (11, 'C02', 'Sleeve hem',           0.50, 'FLATLOCK',      3, 11, 1),
 (12, 'H01', 'Bottom hem',           0.65, 'PLAIN',         3, 12, 1),
 (13, 'BH1', 'Button hole',          0.35, 'BUTTON_HOLE',   3, 13, 1),
 (14, 'BA1', 'Button attach',        0.40, 'BUTTON_ATTACH', 3, 14, 1),
 (15, 'T01', 'Thread trim',          0.70, 'MANUAL',        1, 15, 1),
 (16, 'I01', 'Iron and fold',        0.85, 'IRON',          2, 16, 1);

-- Operations — Basic Crew Tee (style_id 2), ids 17-24
INSERT INTO operations (id, code, name, smv, machine_type, min_grade, sequence_no, style_id) VALUES
 (17, 'C01', 'Cut panel check',       0.25, 'MANUAL',   1, 1, 2),
 (18, 'S01', 'Shoulder join',         0.50, 'OVERLOCK', 3, 2, 2),
 (19, 'N01', 'Neck rib attach',       0.65, 'OVERLOCK', 4, 3, 2),
 (20, 'N02', 'Neck topstitch',        0.45, 'FLATLOCK', 3, 4, 2),
 (21, 'L01', 'Sleeve attach',         0.80, 'OVERLOCK', 4, 5, 2),
 (22, 'S02', 'Side seam',             0.95, 'OVERLOCK', 4, 6, 2),
 (23, 'H01', 'Bottom hem',            0.60, 'FLATLOCK', 3, 7, 2),
 (24, 'T01', 'Thread trim and fold',  0.55, 'MANUAL',   1, 8, 2);

-- Precedence graph (operation_id depends on predecessor_id)
INSERT INTO operation_predecessors (operation_id, predecessor_id) VALUES
 (2,1), (3,2), (4,1), (5,4), (5,3), (6,1), (7,6), (7,5), (8,3), (9,8),
 (10,9), (11,10), (12,10), (13,7), (13,12), (14,13), (15,14), (15,11), (16,15),
 (18,17), (19,18), (20,19), (21,20), (22,21), (23,22), (24,23);

-- Roster, ids 1-14
INSERT INTO operators (id, code, name, grade, default_efficiency) VALUES
 (1,  'OP01', 'Rahima',  4, 0.85),
 (2,  'OP02', 'Shirin',  4, 0.80),
 (3,  'OP03', 'Nasima',  5, 0.90),
 (4,  'OP04', 'Parvin',  3, 0.75),
 (5,  'OP05', 'Jesmin',  4, 0.85),
 (6,  'OP06', 'Ruma',    3, 0.70),
 (7,  'OP07', 'Sultana', 5, 0.95),
 (8,  'OP08', 'Momena',  4, 0.80),
 (9,  'OP09', 'Fatema',  3, 0.75),
 (10, 'OP10', 'Rekha',   4, 0.85),
 (11, 'OP11', 'Anowara', 3, 0.70),
 (12, 'OP12', 'Halima',  5, 0.90),
 (13, 'OP13', 'Sabina',  4, 0.80),
 (14, 'OP14', 'Morjina', 3, 0.75);

-- Skill matrix — two machine skills per operator, matching TEAM.eff in the front end
INSERT INTO operator_skills (operator_id, machine_type, efficiency) VALUES
 (1,  'PLAIN',         1.05), (1,  'OVERLOCK',      0.80),
 (2,  'OVERLOCK',      1.10), (2,  'FLATLOCK',      0.90),
 (3,  'PLAIN',         1.15), (3,  'BUTTON_HOLE',   0.95),
 (4,  'FLATLOCK',      1.00), (4,  'MANUAL',        1.10),
 (5,  'PLAIN',         0.95), (5,  'OVERLOCK',      1.00),
 (6,  'MANUAL',        1.20), (6,  'IRON',          1.00),
 (7,  'PLAIN',         1.20), (7,  'FLATLOCK',      0.85),
 (8,  'OVERLOCK',      1.05), (8,  'PLAIN',         0.90),
 (9,  'BUTTON_ATTACH', 1.10), (9,  'BUTTON_HOLE',   1.05),
 (10, 'PLAIN',         1.00), (10, 'IRON',          0.95),
 (11, 'FLATLOCK',      0.95), (11, 'MANUAL',        1.05),
 (12, 'OVERLOCK',      1.15), (12, 'PLAIN',         1.00),
 (13, 'PLAIN',         1.05), (13, 'BUTTON_ATTACH', 0.90),
 (14, 'IRON',          1.15), (14, 'MANUAL',        1.00);

-- Order book. Due dates are relative to today, same as Order(..., today.plusDays(N)) in DataSeeder.
INSERT INTO orders (id, order_code, buyer, quantity, completed_quantity, due_date, style_id) VALUES
 (1, 'PO-88214', 'Nordfeld',         42000, 25620, DATE_ADD(CURDATE(), INTERVAL 12 DAY), 1),
 (2, 'PO-88301', 'Kloud Basics',     68000, 21760, DATE_ADD(CURDATE(), INTERVAL 21 DAY), 2),
 (3, 'PO-88355', 'Aveline',          18500, 15910, DATE_ADD(CURDATE(), INTERVAL 6  DAY), 1),
 (4, 'PO-88402', 'Northgate Retail', 90000, 12600, DATE_ADD(CURDATE(), INTERVAL 38 DAY), 2);

-- shift_runs / shift_run_hourly / station_results are intentionally left empty:
-- they're written by DashboardService the first time someone signs in and the
-- app simulates a shift for each line (see LineSimulationService).
