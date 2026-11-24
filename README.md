# Hospital Management System (JavaFX)

This project is a JavaFX desktop application backed by PostgreSQL.

## Requirements

- JDK 17 or newer
- PostgreSQL 15 or newer
- Internet access the first time you run the Maven Wrapper, to download Maven and dependencies

## Database setup

Make sure the PostgreSQL Windows service is running. Check its status with:

```powershell
Get-Service '*postgres*'
```

If it is stopped, start the service from an Administrator PowerShell window. For
PostgreSQL 15 installed with the default service name:

```powershell
Start-Service postgresql-x64-15
```

Open PowerShell in the project directory (the folder containing `pom.xml` and
`mvnw.cmd`). For the current installation, navigate there with:

```powershell
Set-Location 'C:\Users\Raman Oli\Desktop\HospitalManagementSysyem-master\HospitalManagementSysyem-master'
```

Create the database once, then apply the schema. `CREATE DATABASE` reports an error
if the database already exists; in that case, skip the first command. PostgreSQL
will prompt for the `postgres` role password:

```powershell
$psql = 'C:\Program Files\PostgreSQL\15\bin\psql.exe'
& $psql -U postgres -d postgres -c "CREATE DATABASE hospital;"
& $psql -U postgres -d hospital -f '.\hospital-postgresql.sql'
```

If PostgreSQL is installed in another location or version, update `$psql`
accordingly. The schema script creates the tables and seeds the demo administrator
account `admin` / `admin`. The old `hospital-mysql.sql` dump is not compatible with
PostgreSQL and is retained for reference only.

## Run the desktop app

The app defaults to `localhost:5432`, database `hospital`, and username `postgres`.
In the same PowerShell window, set the PostgreSQL password without echoing it:

```powershell
Set-Location 'C:\Users\Raman Oli\Desktop\HospitalManagementSysyem-master\HospitalManagementSysyem-master'
$env:HOSPITAL_DB_URL = 'jdbc:postgresql://localhost:5432/hospital'
$env:HOSPITAL_DB_USER = 'postgres'

$secure = Read-Host 'PostgreSQL password' -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $env:HOSPITAL_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
}
.\mvnw.cmd clean javafx:run


The Maven Wrapper downloads Maven and project dependencies on the first run. The
application opens as a desktop window; it does not run in a browser.

The administrator can manage patients, doctors, receptionists, and workers. A
regular user can register, sign in, add patients, and search patient records.

Admin

Username: admin
Password: admin