@echo off
@rem This is the buildscript.
@rem It should be easy enough to change this if the app grows more complex,
@rem but I think for now, this is enough.

cls
setlocal

rem Clean build folder
if not exist "build\" mkdir "build"
cd build
rmdir /s /q . 2>nul
cd ..

rem Compile code
if [%1]==[] (
  set "src=main.cpp"
) else (
  set src=%1
)

echo Compiling %src%

g++ -o "build\main.exe" -std="c++17" %src% || exit /b

rem Set CLI arguments
set /p args="Command line arguments: "

rem Run generated exe
pushd
echo Program output
echo ----------------------------------------------
build\main.exe %args% > con
echo:
echo ----------------------------------------------
pause

rem reset cmd prompt
popd
cls
ver
echo (c) Microsoft Corporation. All rights reserved.
@echo on