# Eve

College Event Hub backend built with Spring Boot and PostgreSQL.

## Local configuration

The application loads local settings from the ignored project-root `.env` file.
Set these variables there before starting the default application profile:

```properties
COLLEGE_EVENT_HUB_DB_PASSWORD=
COLLEGE_STUDENT_DIRECTORY_DB_PASSWORD=
COLLEGE_EVENT_HUB_JWT_SECRET=
```

For the Newman test profile, also set:

```properties
NEWMAN_APP_DB_PASSWORD=
NEWMAN_DIRECTORY_DB_PASSWORD=
```

Keep `.env` private; it is excluded from Git.

## Run

```powershell
.\mvnw.cmd spring-boot:run
```

Run the isolated Newman test profile on port 8082 with:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=newman-test"
```

## Tests

Run the unit and integration test suite with:

```powershell
.\mvnw.cmd test
```

Integration tests use the configured local PostgreSQL databases. Set
`COLLEGE_EVENT_HUB_DB_PASSWORD` and `COLLEGE_STUDENT_DIRECTORY_DB_PASSWORD` in
the project-root `.env` file. The student-directory connection test also
requires the `student_directory` table to be available.

## Placement module

The Placement API supports admin-managed job and internship listings, student
applications, and applicant review:

- `POST /api/v1/admin/placements` creates a draft.
- `GET /api/v1/admin/placements` lists drafts and published/closed placements.
- `PUT /api/v1/admin/placements/{id}` updates a draft or published listing.
- `PUT /api/v1/admin/placements/{id}/publish` publishes a listing.
- `PUT /api/v1/admin/placements/{id}/close` closes applications.
- `GET /api/v1/admin/placements/{id}/applications` lists applicants.
- `GET /api/v1/placements` and `GET /api/v1/placements/{id}` expose published
  opportunities whose application deadlines have not passed.
- `POST /api/v1/placements/{id}/applications` submits a verified student's
  application; the same student can apply only once per placement.
- `GET /api/v1/placements/applications/me` lists the authenticated student's
  applications.

All endpoints require authentication. Placement management and applicant review
require the `ADMIN` role. Applying and viewing personal applications require a
verified `STUDENT` profile.

The project currently uses Hibernate schema updates rather than an automated
migration runner. Before deploying this version to a database with the existing
admin audit table, apply
[`src/main/resources/db/manual/V1__placement_audit_values.sql`](src/main/resources/db/manual/V1__placement_audit_values.sql)
to extend its action and target-type check constraints. The SQL preserves all
existing audit values and adds the Placement values.

## Event feedback

Verified students with a registered attendance record can submit one 1–5 star
rating per event after its status is `COMPLETED` or its scheduled end time has
passed. Cancelled and deleted events do not accept feedback. The submitting
student may update or retrieve their own feedback; the event organization's
leader and `ADMIN` users can review paginated feedback.

- `POST /api/v1/events/{eventId}/feedback` submits feedback.
- `PUT /api/v1/events/{eventId}/feedback` updates the authenticated student's
  feedback.
- `GET /api/v1/events/{eventId}/feedback/me` retrieves the student's feedback.
- `GET /api/v1/events/{eventId}/feedback?page=0&size=10` lists feedback for the
  event (organizer or `ADMIN` only).

Feedback is unique per event and student at the database level.

## Resource library

The Resource Library catalogs external learning links with a title, description,
category, and tags. Resources are associated with an organization. `ADMIN` users
and that organization's leader can create, update, and deactivate its resources;
deactivation is a soft delete. Only authenticated students with verified
profiles can browse the active catalog.

- `GET /api/v1/resources` returns a paginated catalog. Optional filters:
  `q`, `category`, and `tag`; pagination uses `page` and `size`.
- `GET /api/v1/resources/{id}` retrieves an active resource.
- `POST /api/v1/resources` creates and immediately publishes a resource.
  The request includes `organizationId`, `title`, `description`, `url`,
  `category`, and optional `tags`.
- `PUT /api/v1/resources/{id}` updates resource metadata.
- `DELETE /api/v1/resources/{id}` deactivates the resource without deleting its
  record.
- `GET /api/v1/resources/manage` lists resources the authenticated admin or
  organization leader can manage, including deactivated resources. Admins may
  filter this list with `organizationId`.

Resource URLs must use HTTP or HTTPS. File uploads and student-submitted
resources are not supported.
