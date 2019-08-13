-- noinspection SqlNoDataSourceInspectionForFile

-- Setup the test account data.
INSERT INTO account(id, name, image_prefix, colour) VALUES ( 'BANK',  'Bank Account', 'bank', 'FFFFFF' );
INSERT INTO account(id, name, image_prefix, colour) VALUES ( 'AMEX',  'Amex', 'amex', 'FFFFF1' );

-- Setup the test category data.
INSERT INTO category(id, name, sort, restricted, expense, colour, groupid, system_use) VALUES ('TST', 'Test', 2, 'N', 'N', 'FFFFFF', 'TST', 'N');
INSERT INTO category(id, name, sort, restricted, expense, colour, groupid, system_use) VALUES ('HSF', 'Test 2', 1, 'N', 'Y', 'FFFFFF', 'TST', 'N');
INSERT INTO category(id, name, sort, restricted, expense, colour, groupid, system_use) VALUES ('XSF', 'x Test 2', 3, 'N', 'Y', 'FFFFFF', 'TST', 'N');
INSERT INTO category(id, name, sort, restricted, expense, colour, groupid, system_use) VALUES ('RGL', 'Regular', 4, 'N', 'Y', 'FFFFFF', 'TST', 'N');

-- Setup the statements
INSERT INTO statement(account, month, year, open_balance, locked) VALUES ('BANK', 5, 1968, 10.0, 'N');
INSERT INTO statement(account, month, year, open_balance, locked) VALUES ('AMEX', 5, 1968, 125.3, 'N');
INSERT INTO statement(account, month, year, open_balance, locked) VALUES ('DEFS', 4, 1967, 125.3, 'Y');

