## 1.1.1
preboot-query:
- Fixed ID mismatch vulnerability in CrudFilterableController update method - entity ID now enforced to match path parameter
- Fixed UUID mismatch vulnerability in CrudUuidFilterableController update method - entity UUID now enforced to match path parameter

## 1.1.0
preboot-eventbus-core:
- fixed thread safety in event handler initialization
  
preboot-query:
- fixed enum handling in FilterCriteria - enum values are now automatically converted to their string representation
- added support for passing enum instances directly to FilterCriteria.eq(), FilterCriteria.neq(), FilterCriteria.in(), and other filter operations
- eliminates need for manual enum.name() conversion when filtering by enum fields
- fixed Instant type handling for PostgreSQL compatibility - Instant values are now properly converted to java.sql.Timestamp
- added support for string-based date inputs when filtering Instant fields (both direct string values and string arrays)
- fixed array type inference for IN operations with temporal values (Instant, LocalDateTime, java.sql.Timestamp)
- added asynchronous export functionality to FilterableController and UuidFilterableController with generic processing
preboot-auth:
- moved SessionAwareAuthentication from auth-core to auth-api
- added RunAsUserService for executing tasks under different user security contexts
preboot-files:
- made max file size configurable in in-memory file storage service
- added property `preboot.files.max-file-size` (default: 52428800 bytes / 50MB)
preboot-exporters:
- added OutputStream support to DataExporter for streaming large datasets

## 0.3.11 -> Becomed v1.0.0
preboot-auth-emails
- added preboot logo as default one

## 0.3.10
preboot-auth-emails
- preboot.auth-emails.logoPath property added to customize email logo file
preboot-auth:
- preboot.account.assignDemoRoleToNewTenants property added (default = true)
preboot-feature-flags:
- implementation ready for use
preboot-core
- added RateLimiter
- added AccessSynchronizer
preboot-files:
- created new module preboot-files for file storage
- implemented in-memory file storage

## 0.3.9
preboot-exporters:
- optimized ExcelService
- liquibase scripts for auth tables creation (postgres)

## 0.3.8
preboot-auth:
- fixed technical-admin not being able to log-in if not connected to a real tenant

## 0.3.7
preboot-auth:
- added endpoints for managing tenants and users by super admin
- implemented tenant roles to extend the permissions of the tenant users

preboot-exporters:
- added new preboot-exporters-api module with interfaces for data exporters
- implemented preboot-exporters-excel with Apache POI
- added conversions for numeric and boolean values from text
- fixed LocalDateTime conversion if date is zoned

preboot-query:
- added export functionality to FilterableController and UuidFilterableController
## 0.3.6
preboot-auth:
- added endpoints for managing tenant users by the tenant admin
## 0.3.5
preboot-query:
- added array overlap operator (ao) for string arrays
- proper handling of dates from rest controllers (as string) 
## 0.3.4
preboot-query:
- complex aggregate reference queries are now supported
## 0.3.3
preboot-query:
- added support for @AggregateReference annotation - allows for creating projections with joins to other aggregates
## 0.3.2
preboot-securedata:
- added support for audit anotations: @CreatedBy, @CreatedAt, @ModifiedBy, @ModifiedAt
## 0.3.1
preboot-query:
- added better support for entities with second id field (uuid): repositories and controllers
preboot-securedata:
- added support for entities with second id field (uuid): repositories
## 0.3.0
preboot-query:
- added support for complex queries with "or" and "and" operators 
- added equals case-insensitive operator (eqic) for string fields
## 0.2.2
preboot-securedata:
- added access validator checking user role when accessing an entity
## 0.2.1
preboot-eventbus-core:
- added support for generic events (for example DataEvent<T>) and handlers based on generic type
preboot-securedata:
- added crud events: before/after create/update/delete (uses preboot-eventbus-core)
## 0.2.0
- preboot-data-access and preboot-dynamic-query were replaced with preboot-query and tollbox-securedata modules
- updated documentation
## 0.1.0
- updated auth documentation, moved documentation to the documentation folder.
- Added TTLMap collectio to preboot-core
- added preboot-observability module
## 0.0.7
preboot-auth-core (previous preboot-auth-impl):
- feature: added support for multi tenancy in user account management

## 0.0.6
preboot-dynamic-query:
-  feature: dynamic query sql builder looks for Spring's @Table annotation for determining table name

preboot-data-access:
- feature: @DataAccessResource annotation is optional for entities (resource name can be taken from class name)
- feature: patch endpoint functionality added
- bugfix: access validators are "create entity" operation aware
## 0.0.5
preboot-auth-core:
-  feature: Added tenantId to UserAccount
-  change: UserAccount tables don't have tb (preboot) prefix anymore
## 0.0.4 
preboot-auth-core:
-  feature: Added removing user account's functionality to UserAccountManagementApi
## 0.0.3
preboot-eventbus-core:
 -  Change: Event bus by default is not throwing exception if no handlers are found, added annotation @ExceptionIfNoHandler on event class to force exception if no handlers are found
## 0.0.2
- Skipped version
## 0.0.1
preboot-auth-core: 
- Bugfix: CreateInactiveUserAccountRequest uses Set for roles and permissions instead of List
