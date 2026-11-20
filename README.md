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
