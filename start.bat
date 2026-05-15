@echo off
REM Script de démarrage de PharmaBenin pour Windows
REM Usage: start.bat

echo ======================================
echo    PharmaBenin - Démarrage
echo ======================================
echo.

REM Vérifier Java
echo Verification de Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERREUR] Java n'est pas installe. Veuillez installer Java 17 ou superieur.
    pause
    exit /b 1
)
echo [OK] Java detecte
echo.

REM Vérifier Maven wrapper
echo Verification de Maven...
if not exist "mvnw.cmd" (
    echo [ERREUR] Maven wrapper non trouve. Veuillez verifier l'installation.
    pause
    exit /b 1
)
echo [OK] Maven wrapper trouve
echo.

REM Nettoyer et compiler
echo Compilation du projet...
call mvnw.cmd clean install -DskipTests
if errorlevel 1 (
    echo [ERREUR] Erreur lors de la compilation
    pause
    exit /b 1
)
echo [OK] Compilation reussie
echo.

REM Démarrer l'application
echo Demarrage de l'application...
echo.
echo ======================================
echo    Application disponible sur:
echo    http://localhost:8080
echo ======================================
echo.
echo Appuyez sur Ctrl+C pour arreter
echo.

call mvnw.cmd spring-boot:run
