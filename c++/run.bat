@echo off
rem This is the buildscript.
rem It should be easy enough to change this if the app grows more complex,
rem but I think for now, this is enough.

cls
setlocal

rem Clean build folder
if not exist "build\" mkdir "build"
cd build
rmdir /s /q . 2>nul
cd ..

rem Setup options
if [%1]==[] (
  set "src=main.cpp"
) else (
  set src=%1
)

rem Compile
echo Compiling [0;36m%src%[0m
g++ -o "build\main.exe" -std="c++17" %src% || exit /b

rem Set CLI arguments
set /p args="Command line arguments: "

rem Run generated exe
echo Program output
echo ----------------------------------------------
build\main.exe %args% 2>&1
echo:
echo [0m----------------------------------------------
pause
@echo on