# 轻社交类博客平台后端项目说明

## 项目简介
本项目是一个轻社交类博客平台的后端，基于Spring Boot、Spring Security、JPA、MapStruct等技术栈实现。平台支持用户注册、登录、个人信息管理、文章发布、动态、交友、实时消息等功能，旨在为用户提供一个集内容创作与社交互动于一体的现代化博客社区。

## 项目结构

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/kirisamemarisa/blog/
│   │   │   ├── BlogApplication.java         # Spring Boot 启动类
│   │   │   ├── common/                     # 通用工具类（如ApiResponse、JwtUtil）
│   │   │   ├── config/                     # 安全配置（如SecurityConfig）
│   │   │   ├── controller/                 # 控制器层，处理HTTP请求
│   │   │   ├── dto/                        # 数据传输对象（DTO）
│   │   │   ├── mapper/                     # MapStruct映射接口
│   │   │   ├── model/                      # 实体类（数据库表映射）
│   │   │   ├── repository/                 # JPA数据访问层
│   │   │   └── service/                    # 业务逻辑层
│   │   └── resources/
│   │       ├── application.properties      # Spring Boot 配置
│   │       ├── static/                     # 静态资源
│   │       └── templates/                  # 模板文件
│   └── test/
│       └── java/com/kirisamemarisa/blog/   # 测试代码
├── pom.xml                                 # Maven 配置
└── ...
```

## 各层次功能与分工

### 1. Controller（控制器层）
- 负责接收前端请求，参数校验，调用Service层处理业务，返回统一响应（ApiResponse）。
- 例如：UserController 提供注册、登录、获取/更新用户信息等接口。

### 2. Service（业务逻辑层）
- 处理具体业务逻辑，如注册校验、密码加密、用户信息更新等。
- 通过调用Repository层进行数据持久化。
- 通过MapStruct的Mapper进行DTO与实体的转换。

### 3. Repository（数据访问层）
- 基于Spring Data JPA，负责与数据库的交互。
- 例如：UserRepository、UserProfileRepository 提供用户、用户资料的增删查改。

### 4. Model（实体层）
- 定义数据库表结构的Java实体类。
- 例如：User、UserProfile、BlogPost等。

### 5. DTO（数据传输对象）
- 用于前后端数据交互，避免直接暴露数据库实体。
- 例如：UserRegisterDTO、UserLoginDTO、UserProfileDTO等。

### 6. Mapper（对象映射层）
- 使用MapStruct自动生成DTO与实体的转换代码，提升开发效率。

### 7. Common（通用工具）
- 提供统一响应封装（ApiResponse）、JWT工具（JwtUtil）等。

### 8. Config（配置层）
- 主要为安全配置（如SecurityConfig），配置Spring Security、CORS等。

## 主要功能说明

- **用户注册/登录**：支持用户名、密码注册，密码加密存储，登录返回JWT。
- **用户信息管理**：支持获取、更新个人资料（昵称、头像、背景、性别等）。
- **文章发布/管理**：BlogPost相关功能（待完善）。
- **社交互动**：后续将支持动态、交友、实时消息等。

## 典型请求流程
1. 前端请求Controller接口（如注册、登录、获取资料）。
2. Controller参数校验后调用Service。
3. Service处理业务逻辑，调用Repository访问数据库。
4. Service通过Mapper进行DTO与实体转换。
5. Controller返回统一ApiResponse响应。

## 项目当前进度
- 已实现：用户注册、登录、个人信息管理（含DTO、实体、Mapper、Repository、Service、Controller全链路）。
- 文章、动态、消息等功能结构已预留，部分代码待完善。

## 后续功能规划
1. **文章与动态模块**
   - 文章发布、编辑、删除、评论、点赞、收藏
   - 动态（短内容）发布、互动
2. **社交与交友**
   - 用户关注、粉丝、好友关系
   - 好友请求、同意/拒绝、黑名单
3. **实时消息/聊天**
   - WebSocket或长轮询实现实时消息
   - 聊天记录、已读未读、消息撤回
4. **通知与系统消息**
   - 评论、点赞、关注等通知
5. **内容推荐与发现**
   - 热门文章、推荐用户、兴趣标签
6. **后台管理与内容审核**
   - 用户管理、内容审核、举报处理
7. **API文档与测试**
   - 完善接口文档，补充单元测试、集成测试
8. **安全与性能优化**
   - 接口权限、限流、XSS/CSRF防护、缓存优化

## 技术选型与扩展建议
- 持续完善领域模型，分离社交、内容、消息等子模块
- 前后端分离，建议配合Vue/React等现代前端框架
- 可扩展为微服务架构，支持高并发
- 支持多端（Web/小程序/APP）接入

## 贡献指南
- 遵循分层架构，保持代码整洁
- 新增功能请补充对应DTO、Mapper、Service、Controller
- 重要变更请补充单元测试

## 联系方式
如有疑问或建议，请联系项目维护者。

