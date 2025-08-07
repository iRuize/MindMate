CREATE TABLE user
(
    phone_number           VARCHAR(20) PRIMARY KEY COMMENT '手机号，主键',
    username               VARCHAR(50) COMMENT '用户名',
    real_name              VARCHAR(50) COMMENT '真实姓名',
    personalized_signature VARCHAR(255) COMMENT '个性签名',
    email                  VARCHAR(100) COMMENT '电子邮箱'
) COMMENT ='用户信息表';