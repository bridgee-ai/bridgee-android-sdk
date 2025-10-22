# Bridgee Android SDK

[![Maven Central](https://img.shields.io/maven-central/v/ai.bridgee/bridgee-android-sdk)](https://central.sonatype.com/artifact/ai.bridgee/bridgee-android-sdk)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## 📖 Visão Geral

O **Bridgee Android SDK** é uma solução completa de atribuição que conecta suas campanhas de marketing aos eventos de instalação e primeira abertura do seu aplicativo Android. Ele resolve o problema de atribuição precisa em campanhas de aquisição de usuários, integrando-se perfeitamente com provedores de analytics como Firebase Analytics.

### 🎯 Principais Funcionalidades

- **Atribuição Precisa**: Conecta cliques em campanhas com instalações reais
- **Install Referrer**: Utiliza o Android Install Referrer para dados confiáveis
- **Integração Flexível**: Funciona com qualquer provedor de analytics
- **Callbacks Assíncronos**: Receba dados de atribuição em tempo real
- **Eventos Automáticos**: Dispara eventos padronizados automaticamente
- **User Properties**: Define propriedades de usuário com dados de atribuição

---

## 🚀 Instalação

### Gradle (Recomendado)

Adicione a dependência no arquivo `build.gradle` do seu módulo:

```gradle
dependencies {
    implementation 'ai.bridgee:bridgee-android-sdk:2.2.0'
}
```

### Maven

```xml
<dependency>
    <groupId>ai.bridgee</groupId>
    <artifactId>bridgee-android-sdk</artifactId>
    <version>2.2.0</version>
</dependency>
```

---

## 🔧 Configuração Rápida

### 1. Implementar AnalyticsProvider

Primeiro, crie uma implementação do `AnalyticsProvider` para seu provedor de analytics:

```java
// Para Firebase Analytics
public class FirebaseAnalyticsProvider implements AnalyticsProvider {
    private FirebaseAnalytics analytics;
    
    public FirebaseAnalyticsProvider(Context context) {
        this.analytics = FirebaseAnalytics.getInstance(context);
    }
    
    @Override
    public void logEvent(String name, Bundle params) {
        analytics.logEvent(name, params);
    }
    
    @Override
    public void setUserProperty(String name, String value) {
        analytics.setUserProperty(name, value);
    }
}
```

### 2. Inicializar o SDK

```java
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Configurar o provider de analytics
        AnalyticsProvider provider = new FirebaseAnalyticsProvider(this);
        
        // Inicializar o Bridgee SDK
        BridgeeSDK sdk = BridgeeSDK.getInstance(
            this,                    // Context
            provider,               // AnalyticsProvider
            "seu_tenant_id",        // Tenant ID fornecido pela Bridgee
            "sua_tenant_key",       // Tenant Key fornecida pela Bridgee
            false                   // Dry run (false para produção)
        );
    }
}
```

### 3. Registrar Primeira Abertura

No evento de primeira abertura do app (geralmente na MainActivity):

```java
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Verificar se é a primeira abertura
        if (isFirstOpen()) {
            BridgeeSDK sdk = BridgeeSDK.getInstance(/* parâmetros já configurados */);
            
            // Versão simples
            sdk.firstOpen(new MatchBundle());
            
            // Ou versão com callback para receber dados de atribuição
            sdk.firstOpen(new MatchBundle(), new ResponseCallback<MatchResponse>() {
                @Override
                public void ok(MatchResponse response) {
                    Log.i("Bridgee", "Atribuição resolvida:");
                    Log.i("Bridgee", "Source: " + response.getUtmSource());
                    Log.i("Bridgee", "Medium: " + response.getUtmMedium());
                    Log.i("Bridgee", "Campaign: " + response.getUtmCampaign());
                }
                
                @Override
                public void error(Exception e) {
                    Log.e("Bridgee", "Erro na atribuição: " + e.getMessage());
                }
            });
        }
    }
}
```

---

## 📚 Guia Detalhado

### MatchBundle - Melhorando a Precisão

O `MatchBundle` permite enviar dados adicionais para melhorar a precisão do match:

```java
MatchBundle bundle = new MatchBundle()
    .withEmail("usuario@email.com")      // Email do usuário
    .withPhone("+5511999999999")         // Telefone do usuário
    .withName("João Silva")              // Nome do usuário
    .withGclid("gclid_value")           // Google Click ID
    .withCustomParam("user_id", "123");  // Parâmetros customizados

sdk.firstOpen(bundle, callback);
```

### Eventos Automáticos

O SDK automaticamente dispara os seguintes eventos:

| Evento | Descrição |
|--------|-----------|
| `first_open` | Primeira abertura do app |
| `campaign_details` | Detalhes da campanha de atribuição |
| `{tenant_id}_first_open` | Evento personalizado por tenant |
| `{tenant_id}_campaign_details` | Evento de campanha personalizado |

### User Properties Automáticas

O SDK define automaticamente as seguintes propriedades de usuário:

| Propriedade | Descrição |
|-------------|-----------|
| `install_source` | Fonte da instalação (UTM Source) |
| `install_medium` | Meio da instalação (UTM Medium) |
| `install_campaign` | Campanha da instalação (UTM Campaign) |

---

## 🔍 Exemplo Completo

```java
public class BridgeeManager {
    private static BridgeeSDK bridgeeSDK;
    private static final String TAG = "BridgeeManager";
    
    public static void initialize(Context context, String tenantId, String tenantKey) {
        if (bridgeeSDK == null) {
            AnalyticsProvider provider = new FirebaseAnalyticsProvider(context);
            bridgeeSDK = BridgeeSDK.getInstance(context, provider, tenantId, tenantKey, false);
        }
    }
    
    public static void trackFirstOpen() {
        if (bridgeeSDK == null) {
            Log.w(TAG, "SDK não inicializado");
            return;
        }
        
        MatchBundle bundle = new MatchBundle()
            .withCustomParam("app_version", BuildConfig.VERSION_NAME);
            
        bridgeeSDK.firstOpen(bundle, new ResponseCallback<MatchResponse>() {
            @Override
            public void ok(MatchResponse response) {
                Log.i(TAG, "✅ Atribuição bem-sucedida!");
                Log.i(TAG, "📊 UTM Source: " + response.getUtmSource());
                Log.i(TAG, "📱 UTM Medium: " + response.getUtmMedium());
                Log.i(TAG, "🎯 UTM Campaign: " + response.getUtmCampaign());
                
                // Aqui você pode executar lógica adicional baseada na atribuição
                handleAttributionSuccess(response);
            }
            
            @Override
            public void error(Exception e) {
                Log.e(TAG, "❌ Erro na atribuição: " + e.getMessage(), e);
                
                // Implementar fallback ou retry se necessário
                handleAttributionError(e);
            }
        });
    }
    
    private static void handleAttributionSuccess(MatchResponse response) {
        // Implementar lógica específica do app
    }
    
    private static void handleAttributionError(Exception error) {
        // Implementar tratamento de erro
    }
}
```

---

## ⚙️ Configuração Avançada

### Modo Dry Run

Para testes, você pode habilitar o modo dry run:

```java
BridgeeSDK sdk = BridgeeSDK.getInstance(context, provider, tenantId, tenantKey, true);
```

No modo dry run, o SDK:
- ✅ Executa toda a lógica de atribuição
- ✅ Gera logs detalhados
- ❌ **NÃO** envia eventos para o analytics provider

### Configuração via BuildConfig

```java
// No build.gradle
android {
    buildTypes {
        debug {
            buildConfigField "String", "BRIDGEE_TENANT_ID", "\"${BRIDGEE_TENANT_ID}\""
            buildConfigField "String", "BRIDGEE_TENANT_KEY", "\"${BRIDGEE_TENANT_KEY}\""
            buildConfigField "boolean", "BRIDGEE_DRY_RUN", "true"
        }
        release {
            buildConfigField "String", "BRIDGEE_TENANT_ID", "\"${BRIDGEE_TENANT_ID}\""
            buildConfigField "String", "BRIDGEE_TENANT_KEY", "\"${BRIDGEE_TENANT_KEY}\""
            buildConfigField "boolean", "BRIDGEE_DRY_RUN", "false"
        }
    }
}

// No código
BridgeeSDK.getInstance(
    context, 
    provider, 
    BuildConfig.BRIDGEE_TENANT_ID,
    BuildConfig.BRIDGEE_TENANT_KEY,
    BuildConfig.BRIDGEE_DRY_RUN
);
```

---

## 📋 Requisitos

- **Android API Level**: 21+ (Android 5.0)
- **Target SDK**: 34
- **Java**: 8+
- **Dependências**:
  - `com.android.installreferrer:installreferrer:2.2`
  - `com.google.code.gson:gson:2.10.1`

---

## 🐛 Troubleshooting

### Problemas Comuns

**1. NoClassDefFoundError: InstallReferrerClient**
```
Solução: Verifique se a dependência installreferrer está incluída
implementation 'com.android.installreferrer:installreferrer:2.2'
```

**2. Eventos não aparecem no Firebase**
```
Solução: Verifique se o modo dry run está desabilitado em produção
```

**3. Callback não é executado**
```
Solução: Verifique a conectividade de rede e as credenciais do tenant
```

### Logs de Debug

Para habilitar logs detalhados, use o filtro `BRIDGEE-SDK` no Logcat:

```bash
adb logcat -s BRIDGEE-SDK
```

---

## 🔗 Links Úteis

- 📦 [Maven Central](https://central.sonatype.com/artifact/ai.bridgee/bridgee-android-sdk)
- 📖 [Documentação Completa](https://docs.bridgee.ai)
- 🐛 [Reportar Issues](https://github.com/bridgee-ai/bridgee-android-sdk/issues)
- 💬 [Suporte Técnico](mailto:support@bridgee.ai)

---

## 📄 Licença

Este projeto está licenciado sob a Licença MIT - veja o arquivo [LICENSE](LICENSE) para detalhes.

---

## 🤝 Contribuição

Contribuições são bem-vindas! Por favor, leia nosso [Guia de Contribuição](CONTRIBUTING.md) antes de submeter pull requests.

---

**Desenvolvido com ❤️ pela equipe Bridgee.ai**  
