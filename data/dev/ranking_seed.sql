-- ============================================================
-- 랭킹 검증용 catch_record 시드 (로컬 전용, 임시 데이터)
-- 전제: 로컬 기동으로 fishes 24종(is_collectible=true)이 적재돼 있어야 함.
-- fishes_id는 시드 순서에 따라 달라질 수 있어 '이름 서브쿼리'로 안전하게 참조한다.
-- ============================================================

-- user 1 : 고유 5종 + 감성돔 '중복 인증'(총 6행) → COUNT(DISTINCT) 검증용. max size 45.0
INSERT INTO catch_record (user_id, fishes_id, certified_image_url, size, created_at, modified_at) VALUES
(1, (SELECT id FROM fishes WHERE name='감성돔'), 'https://picsum.photos/seed/u1a/300', 30.0, NOW(), NOW()),
(1, (SELECT id FROM fishes WHERE name='감성돔'), 'https://picsum.photos/seed/u1b/300', 45.0, NOW(), NOW()),
(1, (SELECT id FROM fishes WHERE name='농어'),   'https://picsum.photos/seed/u1c/300', 38.0, NOW(), NOW()),
(1, (SELECT id FROM fishes WHERE name='돌돔'),   'https://picsum.photos/seed/u1d/300', 40.0, NOW(), NOW()),
(1, (SELECT id FROM fishes WHERE name='벵에돔'), 'https://picsum.photos/seed/u1e/300', 28.0, NOW(), NOW()),
(1, (SELECT id FROM fishes WHERE name='우럭'),   'https://picsum.photos/seed/u1f/300', 25.0, NOW(), NOW());

-- user 2 : 고유 8종, max size 88.0 → 완성도 공동1위 & 크기 공동1위
INSERT INTO catch_record (user_id, fishes_id, certified_image_url, size, created_at, modified_at) VALUES
(2, (SELECT id FROM fishes WHERE name='감성돔'), 'https://picsum.photos/seed/u2a/300', 50.0, NOW(), NOW()),
(2, (SELECT id FROM fishes WHERE name='농어'),   'https://picsum.photos/seed/u2b/300', 88.0, NOW(), NOW()),
(2, (SELECT id FROM fishes WHERE name='돌돔'),   'https://picsum.photos/seed/u2c/300', 55.0, NOW(), NOW()),
(2, (SELECT id FROM fishes WHERE name='벵에돔'), 'https://picsum.photos/seed/u2d/300', 35.0, NOW(), NOW()),
(2, (SELECT id FROM fishes WHERE name='우럭'),   'https://picsum.photos/seed/u2e/300', 30.0, NOW(), NOW()),
(2, (SELECT id FROM fishes WHERE name='참돔'),   'https://picsum.photos/seed/u2f/300', 60.0, NOW(), NOW()),
(2, (SELECT id FROM fishes WHERE name='광어'),   'https://picsum.photos/seed/u2g/300', 70.0, NOW(), NOW()),
(2, (SELECT id FROM fishes WHERE name='볼락'),   'https://picsum.photos/seed/u2h/300', 20.0, NOW(), NOW());

-- user 3 : 고유 8종, max size 60.0 → user2와 완성도 동점(공동1위), 크기는 3위
INSERT INTO catch_record (user_id, fishes_id, certified_image_url, size, created_at, modified_at) VALUES
(3, (SELECT id FROM fishes WHERE name='감성돔'), 'https://picsum.photos/seed/u3a/300', 48.0, NOW(), NOW()),
(3, (SELECT id FROM fishes WHERE name='농어'),   'https://picsum.photos/seed/u3b/300', 55.0, NOW(), NOW()),
(3, (SELECT id FROM fishes WHERE name='돌돔'),   'https://picsum.photos/seed/u3c/300', 50.0, NOW(), NOW()),
(3, (SELECT id FROM fishes WHERE name='벵에돔'), 'https://picsum.photos/seed/u3d/300', 33.0, NOW(), NOW()),
(3, (SELECT id FROM fishes WHERE name='우럭'),   'https://picsum.photos/seed/u3e/300', 28.0, NOW(), NOW()),
(3, (SELECT id FROM fishes WHERE name='참돔'),   'https://picsum.photos/seed/u3f/300', 60.0, NOW(), NOW()),
(3, (SELECT id FROM fishes WHERE name='갈치'),   'https://picsum.photos/seed/u3g/300', 45.0, NOW(), NOW()),
(3, (SELECT id FROM fishes WHERE name='고등어'), 'https://picsum.photos/seed/u3h/300', 35.0, NOW(), NOW());

-- user 4 : 고유 3종(완성도 하위), max size 88.0 → user2와 크기 동점(공동1위)
INSERT INTO catch_record (user_id, fishes_id, certified_image_url, size, created_at, modified_at) VALUES
(4, (SELECT id FROM fishes WHERE name='방어'),   'https://picsum.photos/seed/u4a/300', 88.0, NOW(), NOW()),
(4, (SELECT id FROM fishes WHERE name='삼치'),   'https://picsum.photos/seed/u4b/300', 70.0, NOW(), NOW()),
(4, (SELECT id FROM fishes WHERE name='갈치'), 'https://picsum.photos/seed/u4c/300', 65.0, NOW(), NOW());

-- user 5 : 고유 6종, max size 52.0 → 완성도 3위, 크기 4위
INSERT INTO catch_record (user_id, fishes_id, certified_image_url, size, created_at, modified_at) VALUES
(5, (SELECT id FROM fishes WHERE name='감성돔'), 'https://picsum.photos/seed/u5a/300', 40.0, NOW(), NOW()),
(5, (SELECT id FROM fishes WHERE name='농어'),   'https://picsum.photos/seed/u5b/300', 52.0, NOW(), NOW()),
(5, (SELECT id FROM fishes WHERE name='돌돔'),   'https://picsum.photos/seed/u5c/300', 48.0, NOW(), NOW()),
(5, (SELECT id FROM fishes WHERE name='벵에돔'), 'https://picsum.photos/seed/u5d/300', 30.0, NOW(), NOW()),
(5, (SELECT id FROM fishes WHERE name='우럭'),   'https://picsum.photos/seed/u5e/300', 26.0, NOW(), NOW()),
(5, (SELECT id FROM fishes WHERE name='전갱이'), 'https://picsum.photos/seed/u5f/300', 22.0, NOW(), NOW());

-- ============================================================
-- 검증 쿼리 (앱의 JPQL과 동일한 집계)
-- ============================================================

-- (0) 완성도 분모 확인 (기대: 24)
-- SELECT COUNT(*) FROM fishes WHERE is_collectible = true;

-- (1) 완성도 랭킹: 사용자별 고유 어종 수 (기대: u2=8, u3=8, u5=6, u1=5, u4=3)
-- SELECT c.user_id, COUNT(DISTINCT c.fishes_id) AS caught
-- FROM catch_record c
-- JOIN fishes f ON f.id = c.fishes_id AND f.is_collectible = true
-- GROUP BY c.user_id
-- ORDER BY caught DESC;

-- (2) 크기 랭킹: 사용자별 최대 크기 (기대: u2=88, u4=88, u3=60, u5=52, u1=45)
-- SELECT c.user_id, MAX(c.size) AS max_size
-- FROM catch_record c
-- GROUP BY c.user_id
-- ORDER BY max_size DESC;

-- ============================================================
-- 초기화 (다시 넣고 싶을 때)
-- DELETE FROM catch_record WHERE user_id BETWEEN 1 AND 5;
-- ============================================================
