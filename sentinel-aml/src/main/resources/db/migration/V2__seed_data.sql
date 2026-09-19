-- Synthetic seed data covering 3+ AML typologies:
--   1) Structuring/Smurfing        -> Priya Sharma / ACC-002
--   2) Rapid Movement of Funds     -> Global Traders Pvt Ltd / ACC-003
--   3) High-Risk Jurisdiction      -> Amara Corp / ACC-004
--   4) Behavioral Deviation        -> Sunil Mehta / ACC-005
-- Plus clean customers (Ravi Kumar, Neha Verma) as false-positive control noise.

INSERT INTO customer (id, customer_ref, full_name, id_number, customer_type, base_risk_rating) VALUES
('11111111-1111-1111-1111-111111111001','CUST-001','Ravi Kumar','ID-9001','RETAIL','LOW'),
('11111111-1111-1111-1111-111111111002','CUST-002','Priya Sharma','ID-9002','RETAIL','LOW'),
('11111111-1111-1111-1111-111111111003','CUST-003','Global Traders Pvt Ltd','ID-9003','BUSINESS','MEDIUM'),
('11111111-1111-1111-1111-111111111004','CUST-004','Amara Corp','ID-9004','BUSINESS','HIGH'),
('11111111-1111-1111-1111-111111111005','CUST-005','Sunil Mehta','ID-9005','RETAIL','LOW'),
('11111111-1111-1111-1111-111111111006','CUST-006','Neha Verma','ID-9006','RETAIL','LOW');

INSERT INTO account (id, customer_id, account_number, currency, risk_rating) VALUES
('22222222-2222-2222-2222-222222222001','11111111-1111-1111-1111-111111111001','ACC-001','INR','LOW'),
('22222222-2222-2222-2222-222222222002','11111111-1111-1111-1111-111111111002','ACC-002','INR','LOW'),
('22222222-2222-2222-2222-222222222003','11111111-1111-1111-1111-111111111003','ACC-003','INR','MEDIUM'),
('22222222-2222-2222-2222-222222222004','11111111-1111-1111-1111-111111111004','ACC-004','INR','HIGH'),
('22222222-2222-2222-2222-222222222005','11111111-1111-1111-1111-111111111005','ACC-005','INR','LOW'),
('22222222-2222-2222-2222-222222222006','11111111-1111-1111-1111-111111111006','ACC-006','INR','LOW');

-- 1) Structuring: 4 txns of 9000-9999 within ~20 hours on ACC-002
INSERT INTO transaction (id, external_txn_id, account_id, direction, amount, currency, amount_base, counterparty_name, counterparty_jurisdiction, channel, txn_timestamp) VALUES
(RANDOM_UUID(),'TXN-S-001','22222222-2222-2222-2222-222222222002','DEBIT',9200,'INR',9200,'Cash Out','IN','CASH', DATEADD('HOUR',-20,CURRENT_TIMESTAMP)),
(RANDOM_UUID(),'TXN-S-002','22222222-2222-2222-2222-222222222002','DEBIT',9500,'INR',9500,'Cash Out','IN','CASH', DATEADD('HOUR',-14,CURRENT_TIMESTAMP)),
(RANDOM_UUID(),'TXN-S-003','22222222-2222-2222-2222-222222222002','DEBIT',9800,'INR',9800,'Cash Out','IN','CASH', DATEADD('HOUR',-8,CURRENT_TIMESTAMP)),
(RANDOM_UUID(),'TXN-S-004','22222222-2222-2222-2222-222222222002','DEBIT',9300,'INR',9300,'Cash Out','IN','CASH', DATEADD('HOUR',-2,CURRENT_TIMESTAMP));

-- 2) Rapid Movement: big credit then >=80% moved out within 48h on ACC-003
INSERT INTO transaction (id, external_txn_id, account_id, direction, amount, currency, amount_base, counterparty_name, counterparty_jurisdiction, channel, txn_timestamp) VALUES
(RANDOM_UUID(),'TXN-R-001','22222222-2222-2222-2222-222222222003','CREDIT',500000,'INR',500000,'Overseas Partner Ltd','SG','WIRE', DATEADD('HOUR',-40,CURRENT_TIMESTAMP)),
(RANDOM_UUID(),'TXN-R-002','22222222-2222-2222-2222-222222222003','DEBIT',250000,'INR',250000,'Shell Co A','AE','WIRE', DATEADD('HOUR',-20,CURRENT_TIMESTAMP)),
(RANDOM_UUID(),'TXN-R-003','22222222-2222-2222-2222-222222222003','DEBIT',170000,'INR',170000,'Shell Co B','AE','WIRE', DATEADD('HOUR',-5,CURRENT_TIMESTAMP));

-- 3) High-risk jurisdiction (North Korea) + also crosses the $10k-equivalent CTR threshold
INSERT INTO transaction (id, external_txn_id, account_id, direction, amount, currency, amount_base, counterparty_name, counterparty_jurisdiction, channel, txn_timestamp) VALUES
(RANDOM_UUID(),'TXN-H-001','22222222-2222-2222-2222-222222222004','DEBIT',15000,'INR',15000,'Unnamed Trading Co','KP','WIRE', DATEADD('HOUR',-3,CURRENT_TIMESTAMP));

-- 4) Behavioral deviation: 90-day baseline of small txns, then a sudden spike today
INSERT INTO transaction (id, external_txn_id, account_id, direction, amount, currency, amount_base, counterparty_name, counterparty_jurisdiction, channel, txn_timestamp) VALUES
(RANDOM_UUID(),'TXN-B-001','22222222-2222-2222-2222-222222222005','DEBIT',2000,'INR',2000,'Grocery Store','IN','CARD', DATEADD('DAY',-60,CURRENT_TIMESTAMP)),
(RANDOM_UUID(),'TXN-B-002','22222222-2222-2222-2222-222222222005','DEBIT',1800,'INR',1800,'Electric Bill','IN','ONLINE', DATEADD('DAY',-45,CURRENT_TIMESTAMP)),
(RANDOM_UUID(),'TXN-B-003','22222222-2222-2222-2222-222222222005','DEBIT',2200,'INR',2200,'Grocery Store','IN','CARD', DATEADD('DAY',-30,CURRENT_TIMESTAMP)),
(RANDOM_UUID(),'TXN-B-004','22222222-2222-2222-2222-222222222005','DEBIT',1900,'INR',1900,'Fuel Station','IN','CARD', DATEADD('DAY',-15,CURRENT_TIMESTAMP)),
(RANDOM_UUID(),'TXN-B-005','22222222-2222-2222-2222-222222222005','DEBIT',2100,'INR',2100,'Grocery Store','IN','CARD', DATEADD('DAY',-7,CURRENT_TIMESTAMP)),
(RANDOM_UUID(),'TXN-B-SPIKE','22222222-2222-2222-2222-222222222005','DEBIT',30000,'INR',30000,'Electronics Store','IN','CARD', DATEADD('HOUR',-1,CURRENT_TIMESTAMP));

-- Clean noise traffic (Ravi Kumar, Neha Verma) - should generate zero alerts.
-- Kept deliberately below every rule threshold (CTR 10000, structuring band
-- 9000-9999) to actually prove the false-positive-control story.
INSERT INTO transaction (id, external_txn_id, account_id, direction, amount, currency, amount_base, counterparty_name, counterparty_jurisdiction, channel, txn_timestamp) VALUES
(RANDOM_UUID(),'TXN-N-001','22222222-2222-2222-2222-222222222001','DEBIT',1200,'INR',1200,'Coffee Shop','IN','CARD', DATEADD('DAY',-3,CURRENT_TIMESTAMP)),
(RANDOM_UUID(),'TXN-N-002','22222222-2222-2222-2222-222222222001','CREDIT',3000,'INR',3000,'Employer Payroll','IN','ACH', DATEADD('DAY',-2,CURRENT_TIMESTAMP)),
(RANDOM_UUID(),'TXN-N-003','22222222-2222-2222-2222-222222222006','DEBIT',900,'INR',900,'Bookstore','IN','CARD', DATEADD('DAY',-1,CURRENT_TIMESTAMP)),
(RANDOM_UUID(),'TXN-N-004','22222222-2222-2222-2222-222222222006','CREDIT',2500,'INR',2500,'Employer Payroll','IN','ACH', DATEADD('DAY',-1,CURRENT_TIMESTAMP));
