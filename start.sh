#!/bin/bash

# Script de démarrage de PharmaBenin
# Usage: ./start.sh

echo "======================================"
echo "   PharmaBenin - Démarrage"
echo "======================================"
echo ""

# Vérifier Java
echo "🔍 Vérification de Java..."
if ! command -v java &> /dev/null; then
    echo "❌ Java n'est pas installé. Veuillez installer Java 17 ou supérieur."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java 17 ou supérieur est requis. Version actuelle: $JAVA_VERSION"
    exit 1
fi
echo "✅ Java $JAVA_VERSION détecté"
echo ""

# Vérifier Maven
echo "🔍 Vérification de Maven..."
if [ ! -f "./mvnw" ]; then
    echo "❌ Maven wrapper non trouvé. Veuillez vérifier l'installation."
    exit 1
fi
echo "✅ Maven wrapper trouvé"
echo ""

# Nettoyer et compiler
echo "🔨 Compilation du projet..."
./mvnw clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "❌ Erreur lors de la compilation"
    exit 1
fi
echo "✅ Compilation réussie"
echo ""

# Démarrer l'application
echo "🚀 Démarrage de l'application..."
echo ""
echo "======================================"
echo "   Application disponible sur:"
echo "   http://localhost:8080"
echo "======================================"
echo ""
echo "Appuyez sur Ctrl+C pour arrêter"
echo ""

./mvnw spring-boot:run
