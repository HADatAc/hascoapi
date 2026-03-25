@echo off
REM Test script for WKF integration
REM Date: 2026-02-10

echo ========================================
echo Testing WKF Integration
echo ========================================
echo.

echo 1. Testing WKF creation endpoint...
curl -X POST "http://localhost:9000/hascoapi/api/wkf/create/%7B%22uri%22%3A%22https%3A%2F%2Fhadatac.org%2Font%2Fhadatac%23%2FWKF-TEST-001%22%2C%22label%22%3A%22Test%20Workflow%22%2C%22hasDataFileUri%22%3A%22https%3A%2F%2Fhadatac.org%2Font%2Fhadatac%23%2FDFL-TEST-001%22%2C%22hasVersion%22%3A%221%22%2C%22comment%22%3A%22Integration%20test%22%2C%22hasSIRManagerEmail%22%3A%22admin%40example.com%22%7D"
echo.
echo.

echo 2. Testing WKF listing endpoint...
curl -X GET "http://localhost:9000/hascoapi/api/wkf/elements/10/0"
echo.
echo.

echo 3. Testing WKF by keyword endpoint...
curl -X GET "http://localhost:9000/hascoapi/api/wkf/keyword/Test/10/0"
echo.
echo.

echo ========================================
echo Test Complete
echo ========================================
pause
