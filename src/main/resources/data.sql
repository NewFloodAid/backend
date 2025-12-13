INSERT INTO report_status (status, user_ordering_number , government_ordering_number)
VALUES
('SENT', 3 , 3),
('PENDING', 2 , 1),
('PROCESS', 1 , 2),
('SUCCESS', 4 , 4);

INSERT INTO assistance_types (name, unit)
VALUES
('ตัดหญ้า - ต้นไม้', 'งาน'),
('ขุดลอกทางระบายน้ำ', 'งาน'),
('เก็บขยะ', 'งาน'),
('ซ่อมแซมถนน', 'งาน'),
('ซ่อมไฟฟ้า', 'งาน'),
('ซ่อมเสียงตามสาย', 'งาน'),
('อื่นๆ', 'งาน');


INSERT INTO image_categories (name, file_limit)
VALUES
('files', 4);

INSERT INTO configs(key,value)
VALUES
('government_phone_number', '0832617497');


INSERT INTO users_admin (username, password, created_at)
VALUES
('admin', 'password', NOW());

INSERT INTO "locations" (latitude, longitude, address, sub_district, district, province, postal_code)
VALUES
    (13.736717, 100.523186, 'Bangkok, Thailand', 'Rattanakosin', 'Phra Nakhon', 'Bangkok', '10200');

INSERT INTO "reports" (user_id, first_name, last_name, location_id, main_phone_number, reserve_phone_number, report_status_id, additional_detail, created_at, updated_at)
VALUES
    ('e2fd4e6c-7e6f-4f8b-85bb-83c6d40efca2', 'John', 'Doe', 1, '1234567890', '0987654321', 2, 'Flood emergency in the area, requires immediate assistance.', NOW(), NOW());

INSERT INTO "report_assistances" (report_id, assistance_type_id, quantity, is_active)
VALUES
    (1, 1, 1, true),  -- 'มีผู้บาดเจ็บหนัก' with quantity 5
    (1, 2, 1, true),  -- 'มีผู้บาดเจ็บหนัก' with quantity 5
    (1, 3, 2, false),  -- 'มีผู้บาดเจ็บหนัก' with quantity 5
    (1, 4, 5, true),  -- 'มีผู้บาดเจ็บหนัก' with quantity 5
    (1, 5, 2, true);  -- 'ต้องการขนย้ายผู้ป่วยติดเตียง' with quantity 3

INSERT INTO "images" (name, image_category_id, report_id)
VALUES
    ('image1.jpg', 1, 1),
    ('image2.jpg', 1, 1);
