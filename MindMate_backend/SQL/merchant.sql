CREATE TABLE merchant_info
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商家ID',

    -- 文字类
    merchant_name      VARCHAR(100) NOT NULL COMMENT '商家名字',
    address            VARCHAR(255) COMMENT '商家地址',
    business_hours     VARCHAR(100) COMMENT '营业时间',
    established_date   DATE COMMENT '成立时间',
    menu               TEXT COMMENT '菜单信息',
    main_dishes        VARCHAR(255) COMMENT '招牌菜品',

    -- 分类
    cuisine_type       VARCHAR(50) COMMENT '菜系类别：川菜、粤菜、鲁菜、湘菜',
    dish_type          VARCHAR(50) COMMENT '菜品类型：火锅、披萨、自助餐、小炒',
    diet_preference    VARCHAR(50) COMMENT '饮食偏好：素菜，清真',
    meal_time_category VARCHAR(50) COMMENT '用餐时间分类：夜宵，早餐，午餐，晚餐，全天候24H',

    -- 照片类
    env_photo          VARCHAR(255) COMMENT '店内环境照片',
    menu_photo         VARCHAR(255) COMMENT '店内菜单照片',
    kitchen_photo      VARCHAR(255) COMMENT '后厨照片',
    license_photo      VARCHAR(255) COMMENT '营业执照照片'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商家信息表';
