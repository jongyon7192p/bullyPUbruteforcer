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

rem Setup options
if [%1]==[] (
  set "src=main.cpp"
) else (
  set src=%1
)

rem Compile
echo Compiling [0;36;1m%src%[0m
g++ -o "build\debug.out" -std="c++17" -Og -g %src% || exit /b

rem Open debugger
echo [0;1mGDB DEBUG OUTPUT[0m
echo ----------------------------------------------
gdb "build\debug.out"
echo:
echo [0m----------------------------------------------
@echo on