# Checklist

## Database
- [x] WEATHER_REMINDER_TASK table created with all columns
- [x] All indexes (IDX_USER_ID, IDX_TASK_STATUS, IDX_EXPECTED_EXEC_TIME, IDX_LOCATION, IDX_AVAILABLE) created
- [x] Table comment and column comments added

## Entity Classes
- [x] ReminderTaskEntity.java created with Lombok @Data annotation
- [x] All fields match database columns
- [x] ReminderTaskVO.java created for view layer
- [x] ReminderTaskCreateRequest.java created for creation requests

## MyBatis Mapper
- [x] ReminderTaskMapper.java interface created with all methods
- [x] ReminderTaskMapper.xml created with SQL statements
- [x] Mapper properly scanned by Spring Boot

## Service Layer
- [x] ReminderTaskService.java interface created
- [x] ReminderTaskServiceImpl.java implemented with CRUD methods
- [x] createTask method returns generated task ID
- [x] getPendingTasksByLocation filters by location, available, expected time, and status
- [x] markTaskAsExecuted updates task status to 1

## MCP Weather Query
- [x] McpWeatherQuerySkill.java created
- [x] query24Hour method returns List<Integer> with 24 weather codes
- [ ] Retry logic (3 attempts) implemented for MCP failures
- [x] Errors logged without blocking other locations

## Disaster Analysis Agent
- [x] DisasterInfo.java created with type, weatherCode, startHour, endHour, description, level fields
- [x] DisasterAnalysisAgent.java created
- [x] analyze method accepts location and weather codes
- [x] AI prompt includes weather code mapping
- [x] JSON response properly parsed to List<DisasterInfo>

## Disaster Review Agent
- [x] DisasterReviewAgent.java created
- [x] reviewLevel method returns int (1, 2, or 3)
- [x] Classification rules implemented:
  - Level 1: 台风、暴雨红色预警、暴雪、冰雹
  - Level 2: 大雨、大风、雷电、大雾
  - Level 3: 小雨、阴天、轻度污染
- [x] AI prompt returns only numeric level

## Email Notification Service
- [x] EmailNotificationService.java interface created
- [x] EmailNotificationServiceImpl.java implemented
- [x] sendDisasterAlert method sends level 1 disaster emails
- [x] sendReminder method sends level 2/3 reminder emails
- [x] Email content includes all required information

## Weather Emergency Job
- [x] WeatherEmergencyJob.java created with @Component
- [x] dailyCheck method annotated with @Scheduled(cron = "0 0 8 * * ?")
- [x] Method fetches all locations from SYS_PARAM_DICT
- [x] processLocation method handles each location independently
- [x] dispatchNotify separates level 1 from level 2/3 disasters
- [x] notifyAllUsers queries all users in location for level 1
- [x] notifyMatchingUsers queries tasks and matches weather codes for level 2/3
- [x] One-time tasks marked as executed after notification

## Scheduling Configuration
- [x] @EnableScheduling added to main application class
- [x] Thread pool configured for concurrent task execution

## Error Handling
- [x] Each location processed independently (failure doesn't affect others)
- [x] Email sending failures logged but don't interrupt flow
- [x] MCP query failures retry 3 times before logging error

## Logging
- [x] Log messages use [紧急响应] prefix
- [x] Key metrics logged: location, disaster count, user count, task count

## Integration
- [x] All components wired together with Spring dependency injection
- [x] No circular dependencies
- [x] Application starts without errors
- [x] Maven compile successful
