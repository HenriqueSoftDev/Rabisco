@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF)
@REM Maven Wrapper startup batch script
@REM ----------------------------------------------------------------------------
@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET ___MVNW_MESS_=%__MVNW_ARG0_NAME__%
@REM Find the project basedir, i.e. the directory that contains the folder ".mvn".
@REM Fallback to current directory if not found.
@SET MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%
@IF NOT "%MAVEN_PROJECTBASEDIR%"=="" GOTO endDetectBaseDir

@SET EXEC_DIR=%CD%
@SET WDIR=%EXEC_DIR%
:findBaseDir
@IF EXIST "%WDIR%"\.mvn GOTO baseDirFound
@cd ..
@IF "%WDIR%"=="%CD%" GOTO baseDirNotFound
@SET WDIR=%CD%
@GOTO findBaseDir

:baseDirFound
SET MAVEN_PROJECTBASEDIR=%WDIR%
@cd "%EXEC_DIR%"
@GOTO endDetectBaseDir

:baseDirNotFound
SET MAVEN_PROJECTBASEDIR=%EXEC_DIR%
@cd "%EXEC_DIR%"

:endDetectBaseDir

@IF NOT EXIST "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties" (
  @ECHO Could not find .mvn\wrapper\maven-wrapper.properties
  @EXIT /B 1
)

@FOR /F "usebackq tokens=1,2 delims==" %%a IN ("%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties") DO (
  @IF "%%a"=="distributionUrl" SET distributionUrl=%%b
)

@SET MAVEN_DIST_NAME=%distributionUrl%
@FOR %%i IN ("%MAVEN_DIST_NAME%") DO @SET MAVEN_DIST_NAME=%%~ni
@SET M2_HOME=%USERPROFILE%\.m2\wrapper\dists\%MAVEN_DIST_NAME%

@IF EXIST "%M2_HOME%\bin\mvn.cmd" GOTO runMaven
@IF EXIST "%M2_HOME%\bin\mvn" GOTO runMaven

@ECHO Downloading Maven...
@SET DOWNLOAD_URL=%distributionUrl%
@IF NOT EXIST "%M2_HOME%" @MD "%M2_HOME%"

@powershell -Command "& { [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; $wc=New-Object System.Net.WebClient; $wc.DownloadFile('%DOWNLOAD_URL%','%M2_HOME%\maven.zip') }"
@IF NOT EXIST "%M2_HOME%\maven.zip" (
  @ECHO Download failed. Please install Maven manually from https://maven.apache.org/download.cgi
  @EXIT /B 1
)

@powershell -Command "Expand-Archive -Path '%M2_HOME%\maven.zip' -DestinationPath '%M2_HOME%\tmp' -Force"
@FOR /D %%d IN ("%M2_HOME%\tmp\*") DO @ROBOCOPY "%%d" "%M2_HOME%" /E /NFL /NDL /NJH /NJS >NUL
@RMDIR /S /Q "%M2_HOME%\tmp"
@DEL "%M2_HOME%\maven.zip"
@ECHO Maven downloaded successfully.

:runMaven
@SET M2_HOME_CMD=%M2_HOME%
@IF EXIST "%M2_HOME_CMD%\bin\mvn.cmd" (
  @"%M2_HOME_CMD%\bin\mvn.cmd" %*
) ELSE (
  @"%M2_HOME_CMD%\bin\mvn" %*
)
