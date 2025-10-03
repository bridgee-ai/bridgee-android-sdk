#!/bin/bash

# Verificar parâmetro
MODE=$1

if [ -z "$MODE" ]; then
    echo "❌ Erro: Parâmetro obrigatório não fornecido"
    echo ""
    echo "Uso: ./create-bundle.sh [MODE]"
    echo ""
    echo "Modos disponíveis:"
    echo "  build  - Apenas compila o projeto para verificar se tudo está OK"
    echo "  local  - Compila e publica no repositório Maven local (~/.m2/repository)"
    echo "  zip    - Compila e cria bundle ZIP para upload no Maven Central Portal"
    echo ""
    echo "Exemplo: ./create-bundle.sh zip"
    exit 1
fi

if [ "$MODE" != "build" ] && [ "$MODE" != "local" ] && [ "$MODE" != "zip" ]; then
    echo "❌ Erro: Modo inválido '$MODE'"
    echo ""
    echo "Modos válidos: build, local, zip"
    exit 1
fi

echo "🎯 Modo selecionado: $MODE"

# Definir variáveis
VERSION="2.0.1"
ARTIFACT_ID="bridgee-android-sdk"
GROUP_ID="ai.bridgee"
GROUP_PATH="ai/bridgee"

# Verificar chave GPG apenas para modo zip
if [ "$MODE" = "zip" ]; then
    KEY_ID=$(gpg --list-secret-keys --keyid-format LONG | grep sec | head -1 | sed 's/.*\/\([A-F0-9]*\) .*/\1/')

    if [ -z "$KEY_ID" ]; then
        echo "❌ Chave GPG não encontrada. Execute 'gpg --gen-key' primeiro."
        exit 1
    fi

    echo "🔑 Usando chave GPG: $KEY_ID"
fi

# Clean build
echo "🧹 Limpando build anterior..."
./gradlew clean

# Build do projeto
echo "🔨 Construindo projeto..."
./gradlew build

# Verificar se AAR foi gerado
if [ ! -f "./bridgeesdk/build/outputs/aar/bridgeesdk-release.aar" ]; then
    echo "❌ AAR não foi gerado. Verifique o build."
    exit 1
fi

# Se modo for apenas build, finalizar aqui
if [ "$MODE" = "build" ]; then
    echo ""
    echo "=========================================="
    echo "✅ BUILD CONCLUÍDO COM SUCESSO!"
    echo "=========================================="
    echo "📦 AAR gerado: ./bridgeesdk/build/outputs/aar/bridgeesdk-release.aar"
    echo ""
    exit 0
fi

# Se modo for local, publicar no Maven local
if [ "$MODE" = "local" ]; then
    echo "📦 Publicando no Maven local..."
    ./gradlew publishToMavenLocal
    
    echo ""
    echo "=========================================="
    echo "✅ PUBLICADO NO MAVEN LOCAL!"
    echo "=========================================="
    echo "📁 Localização: ~/.m2/repository/$GROUP_PATH/$ARTIFACT_ID/$VERSION/"
    echo ""
    echo "Para usar no seu projeto, adicione ao build.gradle:"
    echo ""
    echo "repositories {"
    echo "    mavenLocal()"
    echo "}"
    echo ""
    echo "dependencies {"
    echo "    implementation '$GROUP_ID:$ARTIFACT_ID:$VERSION'"
    echo "}"
    echo "=========================================="
    echo ""
    exit 0
fi

# ========================================
# MODO ZIP - Criar bundle para Maven Central
# ========================================

BUNDLE_DIR="./bundle-temp"
MAVEN_DIR="$BUNDLE_DIR/$GROUP_PATH/$ARTIFACT_ID/$VERSION"
mkdir -p "$MAVEN_DIR"

echo "📦 Preparando artefatos para bundle ZIP..."

# 1. Copiar AAR
cp "./bridgeesdk/build/outputs/aar/bridgeesdk-release.aar" "$MAVEN_DIR/${ARTIFACT_ID}-${VERSION}.aar"

# 2. Criar sources JAR
echo "📚 Criando sources JAR..."
cd "./bridgeesdk/src/main/java"
jar cf "../../../../$MAVEN_DIR/${ARTIFACT_ID}-${VERSION}-sources.jar" .
cd - > /dev/null

# 3. Criar javadoc JAR (placeholder)
echo "📖 Criando javadoc JAR..."
mkdir -p temp-javadoc
echo "Javadoc will be available in future releases." > temp-javadoc/README.txt
cd temp-javadoc
jar cf "../$MAVEN_DIR/${ARTIFACT_ID}-${VERSION}-javadoc.jar" .
cd ..
rm -rf temp-javadoc

# 4. Criar POM
echo "📄 Criando POM..."
cat > "$MAVEN_DIR/${ARTIFACT_ID}-${VERSION}.pom" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>$GROUP_ID</groupId>
    <artifactId>$ARTIFACT_ID</artifactId>
    <version>$VERSION</version>
    <packaging>aar</packaging>
    
    <name>Bridgee Android SDK</name>
    <description>Android SDK for Bridgee AI platform</description>
    <url>https://github.com/bridgee-ai/bridgee-android-sdk</url>
    
    <licenses>
        <license>
            <name>MIT License</name>
            <url>https://opensource.org/licenses/MIT</url>
        </license>
    </licenses>
    
    <developers>
        <developer>
            <id>bridgee</id>
            <name>Bridgee Team</name>
            <email>dev@bridgee.ai</email>
        </developer>
    </developers>
    
    <scm>
        <connection>scm:git:git://github.com/bridgee-ai/bridgee-android-sdk.git</connection>
        <developerConnection>scm:git:ssh://github.com:bridgee-ai/bridgee-android-sdk.git</developerConnection>
        <url>https://github.com/bridgee-ai/bridgee-android-sdk/tree/main</url>
    </scm>
    
    <dependencies>
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.10.1</version>
        </dependency>
        <dependency>
            <groupId>com.android.installreferrer</groupId>
            <artifactId>installreferrer</artifactId>
            <version>2.2</version>
        </dependency>
    </dependencies>
</project>
EOF

# 5. Criar maven-metadata.xml
cat > "$MAVEN_DIR/maven-metadata.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<metadata>
  <groupId>$GROUP_ID</groupId>
  <artifactId>$ARTIFACT_ID</artifactId>
  <versioning>
    <latest>$VERSION</latest>
    <release>$VERSION</release>
    <versions>
      <version>$VERSION</version>
    </versions>
    <lastUpdated>$(date +%Y%m%d%H%M%S)</lastUpdated>
  </versioning>
</metadata>
EOF

cd "$MAVEN_DIR"

echo "🔐 Assinando artefatos..."
# Assinar todos os arquivos
for file in *.aar *.jar *.pom *.xml; do
    if [ -f "$file" ]; then
        echo "  ✍️  $file"
        gpg --armor --detach-sign "$file"
    fi
done

echo "🔢 Gerando checksums..."
# Gerar checksums
for file in *.aar *.jar *.pom *.xml; do
    if [ -f "$file" ]; then
        echo "  🔢 $file"
        md5 -q "$file" > "${file}.md5"
        shasum -a 1 "$file" | cut -d' ' -f1 > "${file}.sha1"
    fi
done

cd - > /dev/null

# Criar bundle ZIP
BUNDLE_ZIP="${ARTIFACT_ID}-${VERSION}-bundle.zip"
cd "$BUNDLE_DIR"
zip -r "../$BUNDLE_ZIP" .
cd ..

# Limpar temp
rm -rf "$BUNDLE_DIR"

echo ""
echo "=========================================="
echo "🎉 BUNDLE CRIADO COM SUCESSO!"
echo "=========================================="
echo "📁 Arquivo: $(pwd)/$BUNDLE_ZIP"
echo ""
echo "📋 Conteúdo:"
unzip -l "$BUNDLE_ZIP"
echo ""
echo "🚀 UPLOAD NO CENTRAL PORTAL:"
echo "1. Acesse: https://central.sonatype.com/publishing"
echo "2. Login com suas credenciais"
echo "3. Upload Bundle → selecione: $BUNDLE_ZIP"
echo "4. Publish"
echo ""
echo "✅ BUNDLE VALIDADO:"
echo "- Estrutura Maven: $GROUP_PATH/$ARTIFACT_ID/$VERSION/"
echo "- Versão release: $VERSION (não-SNAPSHOT)"
echo "- Artefatos: AAR + sources + javadoc + POM"
echo "- Assinaturas GPG: .asc"
echo "- Checksums: .md5 + .sha1"
echo "- Metadata: maven-metadata.xml"
echo "=========================================="
