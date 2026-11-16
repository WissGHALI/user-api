@echo off
rem Minimal Maven wrapper (Windows). Downloads Maven once into %USERPROFILE%\.m2\wrapper\dists, then runs it.
setlocal
set "BASE_DIR=%~dp0"
for /f "usebackq tokens=1,* delims==" %%a in ("%BASE_DIR%.mvn\wrapper\maven-wrapper.properties") do if "%%a"=="distributionUrl" set "DIST_URL=%%b"
for %%f in ("%DIST_URL%") do set "ZIP_NAME=%%~nf"
set "NAME=%ZIP_NAME:-bin=%"
if "%MAVEN_USER_HOME%"=="" set "MAVEN_USER_HOME=%USERPROFILE%\.m2"
set "DEST=%MAVEN_USER_HOME%\wrapper\dists\%NAME%"
if exist "%DEST%\bin\mvn.cmd" goto run

echo Downloading %DIST_URL%
set "MVNW_URL=%DIST_URL%"
set "MVNW_DEST=%DEST%"
set "MVNW_NAME=%NAME%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference = 'Stop'; [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $tmp = Join-Path $env:TEMP ('mvnw-' + [guid]::NewGuid()); New-Item -ItemType Directory -Path $tmp | Out-Null; $zip = Join-Path $tmp 'maven.zip'; Invoke-WebRequest -UseBasicParsing -Uri $env:MVNW_URL -OutFile $zip; Expand-Archive -Path $zip -DestinationPath $tmp; New-Item -ItemType Directory -Force -Path (Split-Path $env:MVNW_DEST) | Out-Null; if (Test-Path $env:MVNW_DEST) { Remove-Item -Recurse -Force $env:MVNW_DEST }; Move-Item -Path (Join-Path $tmp $env:MVNW_NAME) -Destination $env:MVNW_DEST; Remove-Item -Recurse -Force $tmp"
if errorlevel 1 (
  echo Failed to download Maven. 1>&2
  exit /b 1
)

:run
call "%DEST%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%