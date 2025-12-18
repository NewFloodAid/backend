INSERT INTO "report_status" ("id", "status", "user_ordering_number" , "government_ordering_number")
VALUES
(1, 'SENT', 3 , 3),
(2, 'PENDING', 2 , 1),
(3, 'PROCESS', 1 , 2),
(4, 'SUCCESS', 4 , 4)
ON CONFLICT ("id") DO NOTHING;

SELECT setval(pg_get_serial_sequence('report_status', 'id'), coalesce(max(id), 0) + 1, false) FROM report_status;

INSERT INTO "assistance_types" ("id", "name", "unit")
VALUES
(1, 'ตัดหญ้า - ต้นไม้', 'งาน'),
(2, 'ขุดลอกทางระบายน้ำ', 'งาน'),
(3, 'เก็บขยะ', 'งาน'),
(4, 'ซ่อมแซมถนน', 'งาน'),
(5, 'ซ่อมไฟฟ้า', 'งาน'),
(6, 'ซ่อมเสียงตามสาย', 'งาน'),
(7, 'อื่นๆ', 'งาน')
ON CONFLICT ("id") DO NOTHING;

SELECT setval(pg_get_serial_sequence('assistance_types', 'id'), coalesce(max(id), 0) + 1, false) FROM assistance_types;

INSERT INTO "image_categories" ("id", "name", "file_limit")
VALUES
(1, 'files', 4)
ON CONFLICT ("id") DO NOTHING;

SELECT setval(pg_get_serial_sequence('image_categories', 'id'), coalesce(max(id), 0) + 1, false) FROM image_categories;

INSERT INTO "configs"("key","value")
VALUES
('government_phone_number', '0832617497')
ON CONFLICT ("key") DO NOTHING;

INSERT INTO "users_admin" ("username", "password", "created_at")
SELECT 'admin', 'password', NOW()
WHERE NOT EXISTS (SELECT 1 FROM "users_admin" WHERE "username" = 'admin');

-- Remove dummy reports, locations, and images. 
-- Ensure sequences are reset for these tables even if empty, to start at 1.
SELECT setval(pg_get_serial_sequence('locations', 'id'), coalesce(max(id), 0) + 1, false) FROM locations;
SELECT setval(pg_get_serial_sequence('reports', 'id'), coalesce(max(id), 0) + 1, false) FROM reports;
SELECT setval(pg_get_serial_sequence('images', 'id'), coalesce(max(id), 0) + 1, false) FROM images;
