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

INSERT INTO "locations" ("id", "latitude", "longitude", "address", "sub_district", "district", "province", "postal_code")
VALUES
    (1, 13.736717, 100.523186, 'Bangkok, Thailand', 'Rattanakosin', 'Phra Nakhon', 'Bangkok', '10200')
ON CONFLICT ("id") DO NOTHING;

SELECT setval(pg_get_serial_sequence('locations', 'id'), coalesce(max(id), 0) + 1, false) FROM locations;

INSERT INTO "reports" ("id", "user_id", "first_name", "last_name", "location_id", "main_phone_number", "reserve_phone_number", "report_status_id", "additional_detail", "is_anonymous", "created_at", "updated_at")
VALUES
    (1, 'e2fd4e6c-7e6f-4f8b-85bb-83c6d40efca2', 'John', 'Doe', 1, '1234567890', '0987654321', 2, 'Flood emergency in the area, requires immediate assistance.', false, NOW(), NOW())
ON CONFLICT ("id") DO NOTHING;

SELECT setval(pg_get_serial_sequence('reports', 'id'), coalesce(max(id), 0) + 1, false) FROM reports;

INSERT INTO "report_assistances" ("report_id", "assistance_type_id", "quantity", "is_active")
VALUES
    (1, 1, 1, true),
    (1, 2, 1, true),
    (1, 3, 2, false),
    (1, 4, 5, true),
    (1, 5, 2, true)
ON CONFLICT ("report_id", "assistance_type_id") DO NOTHING;

INSERT INTO "images" ("name", "image_category_id", "report_id")
SELECT 'image1.jpg', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM "images" WHERE "name" = 'image1.jpg' AND "report_id" = 1);

INSERT INTO "images" ("name", "image_category_id", "report_id")
SELECT 'image2.jpg', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM "images" WHERE "name" = 'image2.jpg' AND "report_id" = 1);

SELECT setval(pg_get_serial_sequence('images', 'id'), coalesce(max(id), 0) + 1, false) FROM images;
