@echo off
setlocal
REM Use Node dev server by default for easier local development
if exist node -o exist npm (
  echo Starting Node dev server...
  cd %~dp0
  if not exist node_modules (
    echo Installing Node dependencies...
    npm install
  )
  npm start
) else (
  echo Node/npm not found; falling back to Java run (if available)
  echo Compiling Java server...
  if not exist out mkdir out
  javac -d out src\MainServer.java
  if errorlevel 1 (
    echo Compilation failed.
    exit /b 1
  )
  echo Running server on port 8000...
  java -cp out MainServer
)
