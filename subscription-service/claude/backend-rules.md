# Backend Development - Generic Rules

## Input Validation

- Validate all input at system boundaries
- Reject invalid data early with clear error messages
- Validate data types, formats, ranges, and required fields
- Sanitize inputs to prevent injection attacks
- Validate data at entry points to the system

## Error Handling

- Catch and handle errors at appropriate levels
- Log errors with sufficient context for debugging
- Return user-friendly error messages (avoid exposing internal details)
- Use specific error types/codes for different failure scenarios
- Handle partial failures in batch operations gracefully

## Security

### Authentication & Authorization

- Require authentication for protected resources
- Implement proper authorization checks before data access
- Use secure session management
- Validate tokens/credentials on every request

### Data Protection

- Never log sensitive data (passwords, tokens, PII)
- Use secure password hashing
- Encrypt sensitive data at rest
- Use HTTPS/TLS for data in transit
- Implement rate limiting to prevent abuse

### Attack Prevention

- Validate and sanitize all inputs to prevent injection attacks (SQL, NoSQL, command, etc.)
- Implement CSRF protection where needed
- Set appropriate CORS policies
- Avoid exposing internal system details in errors

## Data Management

### Database Operations

- Use transactions for multi-step operations that must be atomic
- Implement proper indexing for query performance
- Avoid N+1 query problems
- Use connection pooling
- Handle database errors gracefully

### Data Integrity

- Enforce constraints at the database level where possible
- Validate business rules before persisting data
- Use appropriate data types for fields
- Handle concurrent updates appropriately
- Implement soft deletes where data recovery is needed

## Performance

- Implement caching where appropriate
- Use asynchronous processing for long-running operations
- Paginate large result sets
- Monitor and optimize slow queries
- Set appropriate timeouts for external calls
- Use bulk operations for batch processing

## Testing

- Write unit tests for business logic
- Write integration tests for service interfaces
- Test error scenarios and edge cases
- Test authentication and authorization
- Mock external dependencies in tests
- Maintain high test coverage for critical paths

## Logging & Monitoring

### Logging

- Log at appropriate levels (ERROR, WARN, INFO, DEBUG)
- Include correlation IDs for request tracing
- Log enough context to diagnose issues
- Don't log sensitive information
- Use structured logging where possible

### Monitoring

- Track key metrics (request rates, error rates, latency)
- Monitor resource usage (CPU, memory, database connections)
- Set up alerts for critical issues
- Track business metrics

## Code Organization

- Separate concerns (presentation, business logic, data access)
- Use dependency injection for flexibility and testability
- Keep functions/methods focused and small
- Use clear, descriptive names
- Document complex logic and business rules
- Avoid tight coupling between components

## Configuration

- Externalize configuration (don't hardcode values)
- Use environment-specific configurations
- Protect sensitive configuration (secrets, credentials)
- Validate configuration at startup
- Document required configuration

## Dependencies

- Keep dependencies up to date
- Review security vulnerabilities regularly
- Minimize dependency count
- Understand what dependencies do before adding them

## Backwards Compatibility

- Consider impact of changes on existing consumers
- Deprecate features before removing them
- Use versioning for breaking changes to public interfaces
- Support migration paths for data model changes

## Documentation

- Document public interfaces and their behavior
- Document error codes and their meanings
- Document authentication/authorization requirements
- Keep documentation up to date with code changes
- Include examples for common use cases

## Idempotency

- Make operations idempotent where possible
- Use idempotency keys for critical operations
- Handle duplicate requests gracefully

## Deployment

- Implement health check mechanisms
- Support graceful shutdown
- Use feature flags for gradual rollouts
- Plan for rollback scenarios
- Automate deployment processes

## General Principles

- Fail fast and explicitly
- Design for observability
- Make systems self-healing where possible
- Prefer simple solutions over complex ones
- Write code for maintainability
- Consider scalability from the start
- Handle failures gracefully
- Test in production-like environments