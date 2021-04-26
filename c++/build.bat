@echo off
if not exist "build\" mkdir "build"
if exist "build\main.exe" del /q "build\main.exe"
echo Compiling...
g++ -o "build\main.exe" -std="c++17" main.cpp
echo Program output
echo ----------------------------------------------
build\main.exe %*
echo:
echo ----------------------------------------------
pause
@echo on