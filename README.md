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
