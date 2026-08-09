@echo off
setlocal
title YONPUOLI

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0support\launch_yonpuoli.ps1" %*
set "YONPUOLI_EXIT=%ERRORLEVEL%"

if not "%YONPUOLI_EXIT%"=="0" (
    echo.
    echo YONPUOLI could not start or stopped with an error.
    echo The diagnostic message is shown above.
    pause
)

exit /b %YONPUOLI_EXIT%
