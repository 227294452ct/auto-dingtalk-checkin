@echo off
setlocal enabledelayedexpansion

echo ========================================
echo   AutoDingDing APK Build Script
echo ========================================
echo.

set "PROJECT_DIR=%~dp0"
set "ANDROID_SDK=%LOCALAPPDATA%\Android\Sdk"

:: ---- Auto-detect Java from Android Studio ----
set "JAVA_HOME="

:: Android Studio default locations
if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
)
if not defined JAVA_HOME (
    if exist "C:\Program Files\Android\Android Studio\jre\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Android\Android Studio\jre"
    )
)
if not defined JAVA_HOME (
    for /d %%d in ("%LOCALAPPDATA%\Programs\Android Studio*") do (
        if exist "%%d\jbr\bin\java.exe" (
            set "JAVA_HOME=%%d\jbr"
        )
    )
)
if not defined JAVA_HOME (
    for /d %%d in ("%ProgramFiles%\JetBrains\Toolbox\apps\AndroidStudio*") do (
        for /d %%e in ("%%d\*") do (
            if exist "%%e\jbr\bin\java.exe" (
                set "JAVA_HOME=%%e\jbr"
            )
        )
    )
)

if not defined JAVA_HOME (
    echo [ERROR] Cannot find Java. Please set JAVA_HOME manually.
    echo   Expected locations:
    echo     C:\Program Files\Android\Android Studio\jbr
    echo     %LOCALAPPDATA%\Programs\Android Studio\jbr
    echo.
    echo To set manually, run:
    echo   set JAVA_HOME=YOUR_JDK_PATH
    echo   gradlew assembleDebug
    pause
    exit /b 1
)

echo [OK] JAVA_HOME = !JAVA_HOME!

:: ---- Set ANDROID_HOME ----
if not exist "!ANDROID_SDK!\platforms" (
    echo [ERROR] Android SDK not found at !ANDROID_SDK!
    echo   Please set ANDROID_HOME to your SDK location.
    pause
    exit /b 1
)

echo [OK] ANDROID_HOME = !ANDROID_SDK!
echo.

:: ---- Generate Gradle Wrapper if missing ----
if not exist "%PROJECT_DIR%gradlew.bat" (
    echo [INFO] Generating Gradle wrapper...
    set "PATH=!JAVA_HOME!\bin;!PATH!"
    
    :: Download gradle-wrapper.jar from Maven Central
    set "WRAPPER_JAR=%PROJECT_DIR%gradle\wrapper\gradle-wrapper.jar"
    if not exist "!WRAPPER_JAR!" (
        echo [INFO] Downloading gradle-wrapper.jar...
        powershell -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.7-wrapper.jar' -OutFile '!WRAPPER_JAR!'" 2>nul
        if not exist "!WRAPPER_JAR!" (
            powershell -Command "Invoke-WebRequest -Uri 'https://mirrors.cloud.tencent.com/gradle/distributions/gradle-8.7-wrapper.jar' -OutFile '!WRAPPER_JAR!'"
        )
    )
    
    :: Create gradlew.bat
    > "%PROJECT_DIR%gradlew.bat" echo @echo off
    >>"%PROJECT_DIR%gradlew.bat" echo set DIRNAME=%%~dp0
    >>"%PROJECT_DIR%gradlew.bat" echo if "%%DIRNAME%%"=="" set DIRNAME=%%CD%%\
    >>"%PROJECT_DIR%gradlew.bat" echo set APP_HOME=%%DIRNAME%%
    >>"%PROJECT_DIR%gradlew.bat" echo set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"
    >>"%PROJECT_DIR%gradlew.bat" echo set CLASSPATH=%%APP_HOME%%gradle\wrapper\gradle-wrapper.jar
    >>"%PROJECT_DIR%gradlew.bat" echo "!JAVA_HOME!\bin\java.exe" %%DEFAULT_JVM_OPTS%% -classpath "%%CLASSPATH%%" org.gradle.wrapper.GradleWrapperMain %%*
)

echo [INFO] Starting build...
echo.

set "PATH=!JAVA_HOME!\bin;!PATH!"
set "ANDROID_HOME=!ANDROID_SDK!"

cd /d "%PROJECT_DIR%"

:: Build
call gradlew.bat assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   BUILD SUCCESSFUL!
    echo ========================================
    echo.
    echo APK location:
    for /r "app\build\outputs\apk" %%f in (*.apk) do echo   %%f
    echo.
) else (
    echo.
    echo [ERROR] Build failed. See output above for details.
)

pause
endlocal
