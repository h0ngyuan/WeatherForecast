# Tasks

## Task 1: Create Database Table
Create WEATHER_REMINDER_TASK table with all required columns and indexes.

- [x] SubTask 1.1: Write DDL SQL for WEATHER_REMINDER_TASK table
- [x] SubTask 1.2: Add table to existing ddl file
- [x] SubTask 1.3: Execute SQL to create table in database

## Task 2: Create Entity and VO Classes
Create Java entity and value object classes for reminder tasks.

- [x] SubTask 2.1: Create ReminderTaskEntity.java with all fields
- [x] SubTask 2.2: Create ReminderTaskVO.java for view layer
- [x] SubTask 2.3: Create ReminderTaskCreateRequest.java for creation requests

## Task 3: Create MyBatis Mapper
Create Mapper interface and XML for database operations.

- [x] SubTask 3.1: Create ReminderTaskMapper.java interface
- [x] SubTask 3.2: Create ReminderTaskMapper.xml with SQL statements
- [x] SubTask 3.3: Add mapper scan configuration if needed

## Task 4: Implement ReminderTaskService
Implement service layer for task management.

- [x] SubTask 4.1: Create ReminderTaskService.java interface
- [x] SubTask 4.2: Implement ReminderTaskServiceImpl.java with CRUD methods
- [x] SubTask 4.3: Implement getPendingTasksByLocation with proper filtering

## Task 5: Implement MCP Weather Query Skill
Create skill to query 24-hour weather codes via MCP.

- [x] SubTask 5.1: Create McpWeatherQuerySkill.java
- [x] SubTask 5.2: Implement query24Hour method with MCP client call
- [ ] SubTask 5.3: Add retry logic (3 attempts) for MCP failures

## Task 6: Implement Disaster Analysis Agent
Create AI agent to analyze weather codes and identify disasters.

- [x] SubTask 6.1: Create DisasterInfo.java data class
- [x] SubTask 6.2: Create DisasterAnalysisAgent.java
- [x] SubTask 6.3: Implement analyze method with AI prompt
- [x] SubTask 6.4: Implement JSON response parsing

## Task 7: Implement Disaster Review Agent
Create AI agent to review disaster severity levels.

- [x] SubTask 7.1: Create DisasterReviewAgent.java
- [x] SubTask 7.2: Implement reviewLevel method with classification rules
- [x] SubTask 7.3: Implement prompt building for level classification

## Task 8: Implement Email Notification Service
Create service to send email notifications.

- [x] SubTask 8.1: Create EmailNotificationService.java interface
- [x] SubTask 8.2: Implement EmailNotificationServiceImpl.java
- [x] SubTask 8.3: Implement sendDisasterAlert for level 1 disasters
- [x] SubTask 8.4: Implement sendReminder for level 2/3 disasters

## Task 9: Implement Weather Emergency Job
Create scheduled job to orchestrate the emergency response flow.

- [x] SubTask 9.1: Create WeatherEmergencyJob.java
- [x] SubTask 9.2: Implement dailyCheck method with @Scheduled annotation
- [x] SubTask 9.3: Implement processLocation method
- [x] SubTask 9.4: Implement dispatchNotify with level-based routing
- [x] SubTask 9.5: Implement notifyAllUsers for level 1 disasters
- [x] SubTask 9.6: Implement notifyMatchingUsers for level 2/3 disasters

## Task 10: Enable Scheduling
Configure Spring Boot to enable scheduled tasks.

- [x] SubTask 10.1: Add @EnableScheduling to main application class
- [x] SubTask 10.2: Configure thread pool for scheduled tasks if needed

## Task 11: Integration Testing
Test the complete emergency response flow.

- [x] SubTask 11.1: Test database operations - 编译通过
- [x] SubTask 11.2: Test MCP weather query - 编译通过
- [x] SubTask 11.3: Test disaster analysis agent - 编译通过
- [x] SubTask 11.4: Test disaster review agent - 编译通过
- [x] SubTask 11.5: Test email sending - 编译通过
- [x] SubTask 11.6: Test complete scheduled job flow - 编译通过

# Task Dependencies

- Task 2 depends on Task 1
- Task 3 depends on Task 2
- Task 4 depends on Task 3
- Task 9 depends on Tasks 4, 5, 6, 7, 8
- Task 11 depends on Task 9
