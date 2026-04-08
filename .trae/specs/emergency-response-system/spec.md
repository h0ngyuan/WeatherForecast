# 紧急响应系统 Spec

## Why

WeatherForecast 项目需要一个独立的紧急响应子系统，用于每日定时监控各地区天气灾害，并根据灾害级别自动通知相关用户。该系统与气象预测查询(query)完全解耦，独立运行。

## What Changes

- 新增 WEATHER_REMINDER_TASK 数据表
- 新增 MCP天气查询 Skill (McpWeatherQuerySkill)
- 新增 灾害分析 Agent (DisasterAnalysisAgent)
- 新增 灾害评审 Agent (DisasterReviewAgent)
- 新增 邮件通知服务 (EmailNotificationService)
- 新增 提醒任务服务 (ReminderTaskService)
- 新增 定时任务入口 (WeatherEmergencyJob)

## Impact

- 新增数据库表和索引
- 新增 6 个 Java 类/接口
- 新增 MyBatis Mapper 和 XML
- 新增定时任务配置

## ADDED Requirements

### Requirement: 定时任务调度

The system SHALL provide a scheduled job that runs daily at 8 AM to check weather disasters for all monitored locations.

#### Scenario: Daily execution
- **WHEN** the scheduled time (8 AM) is reached
- **THEN** the job SHALL fetch all locations from SYS_PARAM_DICT
- **AND** process each location sequentially

### Requirement: MCP Weather Query

The system SHALL query 24-hour weather codes via MCP service for a given location.

#### Scenario: Successful query
- **WHEN** a location name is provided
- **THEN** the system SHALL call MCP service
- **AND** return a list of 24 integers representing hourly weather codes

#### Scenario: MCP failure
- **WHEN** MCP service is unavailable
- **THEN** the system SHALL retry up to 3 times
- **AND** log the error without blocking other locations

### Requirement: Disaster Analysis

The system SHALL analyze 24-hour weather codes using AI to identify potential disasters.

#### Scenario: Disaster detected
- **WHEN** 24-hour weather codes are provided
- **THEN** the AI SHALL analyze and return a list of disasters with type, time range, and description

#### Scenario: No disaster
- **WHEN** weather conditions are normal
- **THEN** the AI SHALL return an empty list

### Requirement: Disaster Level Review

The system SHALL review and classify disaster severity into 3 levels.

#### Scenario: Level classification
- **GIVEN** a disaster with type and weather code
- **WHEN** the review agent processes it
- **THEN** it SHALL return level 1 (severe), 2 (moderate), or 3 (minor)

**Classification Rules:**
- Level 1: 台风、暴雨红色预警、暴雪、冰雹
- Level 2: 大雨、大风、雷电、大雾
- Level 3: 小雨、阴天、轻度污染

### Requirement: Notification Dispatch

The system SHALL dispatch notifications based on disaster level.

#### Scenario: Level 1 disaster
- **GIVEN** a level 1 disaster is detected in a location
- **THEN** the system SHALL notify ALL users in that location via email

#### Scenario: Level 2/3 disaster
- **GIVEN** a level 2 or 3 disaster is detected
- **THEN** the system SHALL query WEATHER_REMINDER_TASK table
- **AND** notify users whose CONCERN_CONDITION matches the disaster weather code
- **AND** mark one-time tasks (TASK_TYPE=0) as executed

### Requirement: Email Notification

The system SHALL send email notifications for disaster alerts and reminders.

#### Scenario: Disaster alert email
- **GIVEN** a level 1 disaster
- **WHEN** sending notification
- **THEN** the email SHALL contain disaster type, time, description, and safety suggestions

#### Scenario: Reminder email
- **GIVEN** a level 2/3 disaster matches a user's task
- **WHEN** sending notification
- **THEN** the email SHALL contain the original question, location, and current weather

### Requirement: Reminder Task Management

The system SHALL manage reminder tasks with CRUD operations.

#### Scenario: Create task
- **WHEN** a new task request is received
- **THEN** the system SHALL persist it to WEATHER_REMINDER_TASK table
- **AND** return the generated task ID

#### Scenario: Query pending tasks
- **GIVEN** a location name
- **WHEN** querying pending tasks
- **THEN** return tasks where:
  - LOCATION matches
  - AVAILABLE = 1
  - EXPECTED_EXEC_TIME <= current time
  - TASK_STATUS = 0 OR TASK_TYPE = 1

#### Scenario: Mark task executed
- **WHEN** a one-time task is executed
- **THEN** update TASK_STATUS to 1

## Data Model

### WEATHER_REMINDER_TASK Table

| Column | Type | Description |
|--------|------|-------------|
| ID | BIGINT PK | Task ID, auto increment from 100000 |
| USER_ID | BIGINT | User ID |
| ORIGINAL_QUESTION | VARCHAR(500) | User's original question |
| CONCERN_WORD | VARCHAR(100) | Keyword like "晒被子" |
| CONCERN_CONDITION | INT | Weather code of concern |
| TASK_TYPE | TINYINT | 0=once, 1=always |
| NOTIFY_CONDITION | VARCHAR(200) | Reserved for complex conditions |
| LOCATION | VARCHAR(50) | Location to monitor |
| TASK_STATUS | TINYINT | 0=pending, 1=executed |
| EXPECTED_EXEC_TIME | DATETIME | When the task becomes active |
| NOTIFY_BY_EMAIL | TINYINT | 1=yes (default), 0=no |
| NOTIFY_BY_SMS | TINYINT | 1=yes, 0=no (default) |
| NOTIFY_BY_WECHAT | TINYINT | 1=yes, 0=no (default) |
| AVAILABLE | TINYINT | 1=active (default), 0=deleted |
| CREATE_TIME | DATETIME | Record creation time |
| UPDATE_TIME | DATETIME | Record update time |

**Indexes:**
- IDX_USER_ID
- IDX_TASK_STATUS
- IDX_EXPECTED_EXEC_TIME
- IDX_LOCATION
- IDX_AVAILABLE
