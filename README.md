# Alert Service

Alert and notification service for the AI Log Monitoring System. Monitors anomaly detections and triggers alerts based on configured rules, with support for multiple notification channels.

## Features

- **Rule-Based Alerting**: Flexible alert rules with multiple trigger conditions
- **Multiple Notification Channels**: Email, Slack, and generic Webhooks
- **Anomaly Integration**: Automatic monitoring of ML-detected anomalies
- **Alert Lifecycle Management**: Track alerts from creation to resolution
- **Rate Limiting**: Prevent alert storms with configurable rate limits
- **Scheduled Monitoring**: Periodic checks for new anomalies
- **Alert History**: Complete audit trail of all alerts

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Alert Service                         │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  ┌──────────────────────────────────────────────────┐   │
│  │         Anomaly Monitoring Scheduler             │   │
│  │  (Checks ml_service.anomaly_detections table)    │   │
│  └──────────────────┬───────────────────────────────┘   │
│                     ↓                                     │
│  ┌──────────────────────────────────────────────────┐   │
│  │            Alert Rule Engine                      │   │
│  │  - Evaluate rules against anomalies               │   │
│  │  - Check rate limits                              │   │
│  │  - Create alert instances                         │   │
│  └──────────────────┬───────────────────────────────┘   │
│                     ↓                                     │
│  ┌──────────────────────────────────────────────────┐   │
│  │         Notification Service                      │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐       │   │
│  │  │  Email   │  │  Slack   │  │ Webhook  │       │   │
│  │  │ Service  │  │ Service  │  │ Service  │       │   │
│  │  └──────────┘  └──────────┘  └──────────┘       │   │
│  └──────────────────────────────────────────────────┘   │
│                     ↓                                     │
│  ┌──────────────────────────────────────────────────┐   │
│  │         Alert Management APIs                     │   │
│  │  - CRUD operations for rules                      │   │
│  │  - Alert acknowledgment/resolution                │   │
│  │  - Alert history and statistics                   │   │
│  └──────────────────────────────────────────────────┘   │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

## Database Schema

### alert_service.alert_rules
- Alert rule definitions
- Rule type, severity, conditions
- Anomaly threshold configuration
- Service and log level filters

### alert_service.notification_channels
- Notification channel configurations
- Email, Slack, Webhook settings
- Success/failure tracking

### alert_service.alerts
- Alert instances
- Status tracking (OPEN, ACKNOWLEDGED, RESOLVED)
- Notification status
- Resolution notes

### ml_service.anomaly_detections (read-only)
- Anomaly detection results from ML Service
- Monitored by Alert Service scheduler

## Alert Rule Types

1. **ANOMALY_DETECTION**: Trigger on ML-detected anomalies
   - Configurable confidence threshold (default: 0.7)
   - Automatic evaluation of new anomalies

2. **THRESHOLD**: Trigger when log count exceeds threshold
   - Time window configuration
   - Count-based thresholds

3. **PATTERN_MATCH**: Trigger on specific log patterns
   - Regex pattern matching
   - Custom conditions

4. **ERROR_RATE**: Trigger when error rate exceeds threshold
   - Percentage-based thresholds
   - Time window analysis

5. **CUSTOM**: Complex custom rules
   - JSON-based condition definitions

## Notification Channels

### Email
- SMTP configuration
- Multiple recipients
- HTML templates
- Attachment support

### Slack
- Webhook integration
- Channel routing
- Rich message formatting
- Thread support

### Webhook
- Generic HTTP webhooks
- Configurable HTTP method (POST, PUT)
- Custom headers
- Retry logic

## Configuration

### Application Properties

```yaml
alert:
  monitoring:
    enabled: true
    interval: 60000  # Check every 60 seconds
    lookback-minutes: 5
    batch-size: 100
  
  notification:
    retry:
      max-attempts: 3
      backoff-delay: 2000
    timeout: 10000
    
    email:
      enabled: true
      from: noreply@aimonitoring.com
    
    slack:
      enabled: true
      default-channel: "#alerts"
    
    webhook:
      enabled: true
      timeout: 10000
  
  rules:
    default-severity: MEDIUM
    anomaly-confidence-threshold: 0.7
  
  rate-limit:
    enabled: true
    max-alerts-per-rule: 10
    time-window-minutes: 60
    cooldown-minutes: 15
```

### Environment Variables

- `DB_HOST`: PostgreSQL host (default: localhost)
- `DB_PORT`: PostgreSQL port (default: 5432)
- `DB_NAME`: Database name (default: ai_monitoring)
- `DB_USER`: Database user (default: admin)
- `DB_PASSWORD`: Database password
- `SMTP_HOST`: SMTP server host
- `SMTP_PORT`: SMTP server port
- `SMTP_USERNAME`: SMTP username
- `SMTP_PASSWORD`: SMTP password
- `EMAIL_ENABLED`: Enable email notifications (default: true)
- `SLACK_ENABLED`: Enable Slack notifications (default: true)
- `WEBHOOK_ENABLED`: Enable webhook notifications (default: true)

## API Endpoints

### Alert Rules

```
POST   /api/v1/alert-rules          Create alert rule
GET    /api/v1/alert-rules          List all rules
GET    /api/v1/alert-rules/{id}     Get rule by ID
PUT    /api/v1/alert-rules/{id}     Update rule
DELETE /api/v1/alert-rules/{id}     Delete rule
POST   /api/v1/alert-rules/{id}/enable   Enable rule
POST   /api/v1/alert-rules/{id}/disable  Disable rule
```

### Alerts

```
GET    /api/v1/alerts               List alerts (with filters)
GET    /api/v1/alerts/{id}          Get alert by ID
POST   /api/v1/alerts/{id}/acknowledge  Acknowledge alert
POST   /api/v1/alerts/{id}/resolve      Resolve alert
GET    /api/v1/alerts/statistics    Get alert statistics
```

### Notification Channels

```
POST   /api/v1/channels             Create channel
GET    /api/v1/channels             List channels
GET    /api/v1/channels/{id}        Get channel by ID
PUT    /api/v1/channels/{id}        Update channel
DELETE /api/v1/channels/{id}        Delete channel
POST   /api/v1/channels/{id}/test   Test channel
```

## Alert Workflow

1. **Detection**: Scheduler monitors `ml_service.anomaly_detections` table
2. **Evaluation**: Alert rules are evaluated against new anomalies
3. **Rate Limiting**: Check if alert should be suppressed
4. **Creation**: Alert instance is created in database
5. **Notification**: Notifications sent via configured channels
6. **Tracking**: Alert status tracked through lifecycle
7. **Resolution**: Alert can be acknowledged and resolved

## Rate Limiting

Prevents alert storms by limiting alerts per rule:

- **Max Alerts**: Maximum alerts per rule within time window
- **Time Window**: Rolling time window for counting alerts
- **Cooldown**: Cooldown period after hitting limit
- **Override**: Critical alerts can bypass rate limits

## Development

### Build

```bash
cd backend/alert-service
./mvnw clean install
```

### Run Locally

```bash
./mvnw spring-boot:run
```

### Run Tests

```bash
./mvnw test
```

### Docker Build

```bash
docker build -t alert-service:latest .
```

## Dependencies

- Spring Boot 3.2.1
- Spring Data JPA
- Spring Mail
- Spring WebFlux (for webhooks)
- PostgreSQL
- Lombok
- Quartz Scheduler

## Monitoring

### Health Check

```bash
curl http://localhost:8083/actuator/health
```

### Metrics

```bash
curl http://localhost:8083/actuator/metrics
```

## Integration

### With ML Service

Alert Service reads from `ml_service.anomaly_detections` table to monitor for new anomalies. High-confidence anomalies (confidence > 0.7) trigger alerts automatically.

### With Log Processor

Log Processor writes anomaly detections to the database, which Alert Service monitors and processes.

### With Frontend

Frontend consumes Alert Service REST APIs to:
- Display active alerts
- Manage alert rules
- Configure notification channels
- View alert history and statistics

## Security

- Database credentials via environment variables
- SMTP credentials encrypted
- Webhook authentication via custom headers
- API authentication (to be implemented)
- Rate limiting to prevent abuse

## Future Enhancements

- [ ] Alert escalation policies
- [ ] Alert grouping and deduplication
- [ ] PagerDuty integration
- [ ] Microsoft Teams integration
- [ ] Alert templates
- [ ] Alert scheduling (quiet hours)
- [ ] Alert dependencies
- [ ] Machine learning for alert tuning

## License

IBM Internal Use Only