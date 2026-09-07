# 人事管理系统数据库课程设计 · HRMS-404

> **404 Not Found 小组** · 数据库课程设计项目
> 项目代号：**HRMS-404**（Human Resource Management System × 404 Not Found）

一个基于 **Java + Spring Boot + MySQL** 的人事管理系统课程设计项目。系统覆盖部门、员工、考勤、薪资等核心人事业务，并将**数据库理论**（范式设计、视图、存储过程、触发器）与 **AI 大模型应用**（DeepSeek V4 Flash 自然语言转 SQL）结合，作为本项目区别于一般增删改查系统的亮点。

**当前状态：全量功能已完成并本地运行通过**（数据库层 + 后端 API + 前端页面 + Text to SQL + RBAC）。

---

## 快速开始

### 1. 初始化数据库

```bash
# 依次执行（MySQL 8.0，默认 root，密码可通过环境变量 DB_PASSWORD 覆盖）
mysql -uroot -p123456 --default-character-set=utf8mb4 < sql/01_schema.sql
mysql -uroot -p123456 --default-character-set=utf8mb4 < sql/02_views.sql
mysql -uroot -p123456 --default-character-set=utf8mb4 < sql/03_procs_triggers.sql
mysql -uroot -p123456 --default-character-set=utf8mb4 < sql/04_seed.sql
```

### 2. 启动后端

```bash
cd backend
mvn clean package -DskipTests          # JDK 17+，项目要求 JDK 25 全量支持
java -jar target/hrms-404-4.0.4.jar    # 浏览器打开 http://localhost:8080/login
```

可选环境变量：`DB_USERNAME` / `DB_PASSWORD`（默认 root / 123456）、`DEEPSEEK_API_KEY`（Text to SQL，默认读取环境变量）。

### 3. 演示账号（密码均为 `123456`）

| 账号 | 角色 | 可见范围 |
|------|------|----------|
| `admin` | 系统管理员 | 全部功能 |
| `hr01` | HR 专员 | 全部模块管理 |
| `manager01` | 销售部经理 | 仅销售一部及其子部门数据（树形范围） |
| `E0001`~`E1002` | 普通员工 | 我的中心 / 打卡 / 我的薪资 |

> 新增员工自动按工号创建账号（如 E1003），默认密码 123456；员工离职时触发器自动禁用账号。

## 技术选型

| 类别 | 选型 | 用途 |
|------|------|------|
| 开发语言 | Java（JDK 17+） | 全项目统一 Java 开发 |
| 构建工具 | Maven | 依赖管理与打包 |
| 后端框架 | Spring Boot 3.5 + Spring MVC | IoC/AOP、RESTful 接口、声明式事务 |
| ORM 持久层 | MyBatis-Plus 3.5 | 简化 CRUD、分页插件、存储过程调用 |
| 前端 | Thymeleaf + Bootstrap 5（本地化） | 服务端渲染响应式页面 |
| 数据库 | MySQL 8.0 | 视图 / 存储过程 / 触发器 |
| AI 大模型 | DeepSeek V4 Flash API | Text to SQL 自然语言查询（OpenAI 兼容协议） |

## 功能模块（已实现）

| 模块 | 实现要点 |
|------|----------|
| 员工管理 | 多条件组合查询、分页、自动生成工号、逻辑删除（离职触发器自动禁用账号）、复职 |
| 部门管理 | 组织树（存储过程 `sp_get_org_tree` 递归）、直属/含子树人数、删除前校验 |
| 职位管理 | 编制占用进度条、**编制预算触发器**校验（超编 409 拒绝） |
| 考勤打卡 | 每日一卡（重复打卡 409）、9:00/18:00 自动判定迟到早退、月度汇总视图 |
| 薪资管理 | 存储过程 `sp_generate_monthly_salary` 幂等批量生成、实发工资触发器计算、绩效/扣款调整 |
| 用户权限 | 会话登录 + MD5 加盐（盐 `404n0tf0und`）、RBAC 角色（页面/接口双层）、经理数据范围=部门子树 |
| **Text to SQL** | 真实表结构注入提示词 → DeepSeek V4 Flash 生成 → 白名单安全校验 → 执行渲染 + CSV 导出 |
| 仪表盘 | 按角色差异化统计（全公司 / 部门子树 / 个人） |

## 数据库设计亮点（课程评分点）

- 关系模式规范化达到 **BCNF**（Boyce-Codd 范式）
- 3 个视图：`v_employee_full_info` / `v_attendance_summary` / `v_salary_history`
- 3 个存储过程：部门人数递归统计、组织树递归、月度薪资幂等生成
- 4 类触发器：入职自动建号、离职禁用账号、实发工资自动计算、编制预算校验（INSERT/UPDATE 两个对象）

## 小组特征元素（404 Not Found）

组名取自 HTTP 状态码 404，系统全流程植入品牌细节：

- 自定义 **404 错误页**：大号 404 + "页面不存在，已被按离职流程处理"
- 页签标题统一 `xx · HRMS-404`、顶栏 404 徽标、页脚小组署名
- 空数据占位统一 "404：暂无数据"、Toast 删除提示"已从列表 404"
- 浏览器控制台彩蛋、密码盐 `404n0tf0und`
- 版本号 v4.0.4

完整清单见 [`开发文档.md` 第 4.4 节](开发文档.md)。

## 目录结构

```
.
├── README.md          # 本文件
├── 开发文档.md         # 设计文档（架构/数据库/AI 接入/404 元素）
├── sql/               # 建库脚本（01 表结构 → 02 视图 → 03 过程触发器 → 04 种子数据）
└── backend/           # Spring Boot 后端（含 Thymeleaf 前端页面）
    ├── pom.xml
    └── src/main/
        ├── java/com/hrms404/   # controller / service / mapper / entity / common
        └── resources/
            ├── application.yml
            ├── static/         # bootstrap 本地化 + 自定义 css/js
            └── templates/      # 页面模板 + 品牌错误页
```

## 文档

- [开发文档.md](开发文档.md)：分层架构、数据库设计思路、RESTful API 规范、Text to SQL 设计、DeepSeek V4 Flash 接入设计、404 特征元素设计

---

*© 2026 404 Not Found 小组 · 人事管理系统数据库课程设计*
