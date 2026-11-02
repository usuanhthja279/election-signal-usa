ALTER TABLE historical_contests DROP CONSTRAINT historical_contests_result_status_check;
ALTER TABLE historical_contests ADD CONSTRAINT historical_contests_result_status_check CHECK (result_status IN ('CERTIFIED','UNCONTESTED','NOT_ON_BALLOT','MISSING_SOURCE','NOT_IMPORTED','SOURCE_REPORTED'));
