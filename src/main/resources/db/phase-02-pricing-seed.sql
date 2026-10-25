/*
 * Phase 02 demo/dev pricing seed: 3 services x 3 from-regions x 3 to-regions = 27 ACTIVE rows.
 * Reference prices only (market range of GHTK, GHN, Viettel Post, SPX); the team may replace them.
 * Route kinds: same region, adjacent (NORTH-CENTRAL, CENTRAL-SOUTH), far (NORTH-SOUTH).
 * Inserts only combinations that have no ACTIVE row yet, so it is safe to run again and never edits existing prices.
 */
SET XACT_ABORT ON;

IF OBJECT_ID(N'dbo.pricing_policies', N'U') IS NULL
BEGIN
    THROW 51000, 'Apply the base schema (pricing_policies) before the pricing seed.', 1;
END;

;WITH r(region, pos) AS (SELECT * FROM (VALUES (N'NORTH', 0), (N'CENTRAL', 1), (N'SOUTH', 2)) AS x(region, pos)),
s(service_code, same_base, near_base, far_base, same_step, near_step, far_step) AS (
    SELECT * FROM (VALUES
        (N'STANDARD', 22000, 30000, 35000, 3000, 4000, 5000),
        (N'FAST',     28000, 38000, 45000, 4000, 5000, 6000),
        (N'EXPRESS',  40000, 55000, 65000, 6000, 8000, 10000)
    ) AS x(service_code, same_base, near_base, far_base, same_step, near_step, far_step)
),
c AS (
    SELECT s.service_code, f.region AS from_region, t.region AS to_region,
           ABS(f.pos - t.pos) AS gap, s.same_base, s.near_base, s.far_base, s.same_step, s.near_step, s.far_step
    FROM s CROSS JOIN r AS f CROSS JOIN r AS t
)
INSERT INTO dbo.pricing_policies
    (service_code, from_region, to_region, base_weight_g, base_price, extra_weight_step_g, extra_weight_price,
     insurance_rate_percent, cod_fee_percent, status)
SELECT c.service_code, c.from_region, c.to_region, 500,
       CASE c.gap WHEN 0 THEN c.same_base WHEN 1 THEN c.near_base ELSE c.far_base END,
       500,
       CASE c.gap WHEN 0 THEN c.same_step WHEN 1 THEN c.near_step ELSE c.far_step END,
       0.50, 0.80, N'ACTIVE'
FROM c
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.pricing_policies p
    WHERE p.service_code = c.service_code AND p.from_region = c.from_region
      AND p.to_region = c.to_region AND p.status = N'ACTIVE');
GO
