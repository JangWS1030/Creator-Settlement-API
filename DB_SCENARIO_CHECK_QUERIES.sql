-- Creator Settlement API verification queries
-- Run after starting MySQL and application.
-- UI scenario buttons create rows with fixed ids:
-- sale-tc01-ui, sale-tc02-ui, sale-tc03-ui, sale-tc04-ui, sale-tc05-ui-1, sale-tc05-ui-2

-- ------------------------------------------------------------------
-- 0. DB objects
-- ------------------------------------------------------------------
SHOW FULL TABLES WHERE Table_type = 'VIEW';

SHOW FUNCTION STATUS
WHERE Db = DATABASE();

-- ------------------------------------------------------------------
-- 1. Canonical seed counts
-- ------------------------------------------------------------------
SELECT COUNT(*) AS creators FROM creators;
SELECT COUNT(*) AS courses FROM courses;
SELECT COUNT(*) AS sales_total FROM sale_records;
SELECT COUNT(*) AS cancellations_total FROM cancellation_records;

SELECT COUNT(*) AS sample_sales
FROM sale_records
WHERE id REGEXP '^sale-[0-9]+$';

SELECT COUNT(*) AS extra_sales
FROM sale_records
WHERE id NOT REGEXP '^sale-[0-9]+$';

SELECT id
FROM sale_records
WHERE id NOT REGEXP '^sale-[0-9]+$'
ORDER BY id;

-- ------------------------------------------------------------------
-- 2. Reporting views baseline
-- ------------------------------------------------------------------
SELECT *
FROM vw_creator_monthly_settlement_base
ORDER BY creator_id, settlement_month;

SELECT *
FROM vw_sale_audit
ORDER BY creator_id, paid_at, sale_id;

-- ------------------------------------------------------------------
-- 3. TC-01 accumulated partial refund
-- ------------------------------------------------------------------
SELECT id, amount, paid_at
FROM sale_records
WHERE id = 'sale-tc01-ui';

SELECT id, refund_amount, cancelled_at
FROM cancellation_records
WHERE sale_record_id = 'sale-tc01-ui'
ORDER BY cancelled_at, id;

SELECT *
FROM vw_sale_audit
WHERE sale_id = 'sale-tc01-ui';

SELECT *
FROM vw_creator_monthly_settlement_base
WHERE creator_id = 'creator-6'
  AND settlement_month = '2025-09';

-- ------------------------------------------------------------------
-- 4. TC-02 negative monthly net sale
-- ------------------------------------------------------------------
SELECT id, amount, paid_at
FROM sale_records
WHERE id = 'sale-tc02-ui';

SELECT id, refund_amount, cancelled_at
FROM cancellation_records
WHERE sale_record_id = 'sale-tc02-ui';

SELECT *
FROM vw_creator_monthly_settlement_base
WHERE creator_id = 'creator-6'
  AND settlement_month = '2025-11';

-- ------------------------------------------------------------------
-- 5. TC-03 cancellation after paid settlement
-- ------------------------------------------------------------------
SELECT id, status, total_sale_amount, refund_amount, net_sale_amount,
       platform_fee_amount, scheduled_settlement_amount, created_at, confirmed_at, paid_at
FROM settlements
WHERE id = 'settlement-creator-4-2025-03';

SELECT id, amount, paid_at
FROM sale_records
WHERE id = 'sale-tc03-ui';

SELECT id, refund_amount, cancelled_at
FROM cancellation_records
WHERE sale_record_id = 'sale-tc03-ui';

SELECT *
FROM vw_creator_monthly_settlement_base
WHERE creator_id = 'creator-4'
  AND settlement_month IN ('2025-03', '2025-04')
ORDER BY settlement_month;

-- ------------------------------------------------------------------
-- 6. TC-04 retro fee-rate refund
-- ------------------------------------------------------------------
SELECT effective_from, fee_rate_percentage, created_at
FROM fee_rate_histories
ORDER BY effective_from;

SELECT id, amount, paid_at
FROM sale_records
WHERE id = 'sale-tc04-ui';

SELECT id, refund_amount, cancelled_at
FROM cancellation_records
WHERE sale_record_id = 'sale-tc04-ui';

SELECT *
FROM vw_creator_monthly_settlement_base
WHERE creator_id = 'creator-5'
  AND settlement_month = '2025-04';

-- ------------------------------------------------------------------
-- 7. TC-05 millisecond / second boundary
-- ------------------------------------------------------------------
SELECT id, amount, paid_at
FROM sale_records
WHERE id IN ('sale-tc05-ui-1', 'sale-tc05-ui-2')
ORDER BY paid_at, id;

SELECT *
FROM vw_sale_audit
WHERE sale_id IN ('sale-tc05-ui-1', 'sale-tc05-ui-2')
ORDER BY paid_at, sale_id;

SELECT *
FROM vw_creator_monthly_settlement_base
WHERE creator_id = 'creator-4'
  AND settlement_month IN ('2025-08', '2025-09')
ORDER BY settlement_month;
