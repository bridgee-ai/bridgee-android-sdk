# Bridgee Android SDK

[![Maven Central](https://img.shields.io/maven-central/v/ai.bridgee/bridgee-android-sdk)](https://central.sonatype.com/artifact/ai.bridgee/bridgee-android-sdk)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)

## 📖 Visão Geral

O **Bridgee Android SDK** é uma solução completa de atribuição que conecta suas campanhas de marketing aos eventos de instalação e primeira abertura do seu aplicativo Android. Ele resolve o problema de atribuição precisa em campanhas de aquisição de usuários, integrando-se perfeitamente com provedores de analytics como Firebase Analytics.

### 🎯 Principais Funcionalidades

- **Atribuição Precisa**: Conecta cliques em campanhas com instalações reais
- **Install Referrer**: Utiliza o Android Install Referrer para dados confiáveis
- **Integração Flexível**: Funciona com qualquer provedor de analytics
- **Callbacks Assíncronos**: Receba dados de atribuição em tempo real
- **Atribuição de Sessão Automática**: O SDK cuida da comunicação com seu provedor de analytics para associar a sessão do usuário ao canal de aquisição

---

## 🚀 Instalação

### Gradle (Recomendado)

Adicione a dependência no arquivo `build.gradle` do seu módulo:

```gradle
dependencies {
    implementation 'ai.bridgee:bridgee-android-sdk:2.3.0'
}
```

### Maven

```xml
<dependency>
    <groupId>ai.bridgee</groupId>
    <artifactId>bridgee-android-sdk</artifactId>
    <version>2.3.0</version>
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

> 💡 **Projeto de Exemplo Completo**: Confira o [bridgee-android-example](https://github.com/bridgee-ai/bridgee-android-example) para ver uma implementação completa com interface de usuário e tratamento de callbacks.

---

## 📚 Guia Detalhado

### MatchBundle — Opcional, mas recomendado

**Todos os campos do `MatchBundle` são opcionais.** O SDK consegue resolver a atribuição mesmo com um bundle vazio, usando sinais de dispositivo e rede (fingerprint) para reconciliar com o clique original. No entanto, **quanto mais dados você fornecer, maior a confiança do match** — especialmente em cenários de alto volume ou com múltiplos usuários compartilhando a mesma rede (Wi-Fi corporativo, NAT, etc.).

```java
// Bundle vazio — funciona, mas com confiança menor
sdk.firstOpen(new MatchBundle(), callback);

// Bundle enriquecido — maior confiança no match
MatchBundle bundle = new MatchBundle()
    .withEmail("usuario@email.com")      // Email do usuário
    .withPhone("+5511999999999")         // Telefone do usuário
    .withName("João Silva")              // Nome do usuário
    .withGclid("gclid_value")           // Google Click ID
    .withCustomParam("user_id", "123");  // Parâmetros customizados

sdk.firstOpen(bundle, callback);
```

> 🔗 **Propague os mesmos sinais nos blinks.** Para que a reconciliação seja máxima, os parâmetros enviados ao `firstOpen()` devem também estar presentes nas URLs de captura (os **blinks**, ex.: `https://android.seuapp.com.br/?email=...&phone=...&utm_source=...`). O servidor Bridgee compara os sinais dos dois lados (clique vs. instalação) — quanto maior a interseção, mais eficiente e preciso o match.

### Atribuição Automática da Sessão

Quando a atribuição é resolvida, o SDK se encarrega de comunicar os dados de aquisição ao seu provedor de analytics (Firebase, Amplitude, etc.), garantindo que toda a sessão do usuário fique associada ao canal de origem — sem necessidade de tratamento manual no código do app.

---

## 📱 Projeto de Exemplo

Para uma implementação completa com interface de usuário, callbacks e tratamento de erros, confira nosso projeto de exemplo:

🔗 **[bridgee-android-example](https://github.com/bridgee-ai/bridgee-android-example)**

O projeto de exemplo inclui:
- ✅ Formulário interativo para testar o SDK
- ✅ Implementação completa com callbacks
- ✅ Exibição de UTMs retornados pela API
- ✅ Tratamento de erros com diálogos informativos
- ✅ Integração com Firebase Analytics

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
- ✅ Faz chamadas à API
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

- 📱 [Projeto de Exemplo](https://github.com/bridgee-ai/bridgee-android-example) - Implementação completa com UI
- 📦 [Maven Central](https://central.sonatype.com/artifact/ai.bridgee/bridgee-android-sdk)
- 🍎 [Bridgee iOS SDK](https://github.com/bridgee-ai/bridgee-ios-sdk)
- ⚛️ [Bridgee React Native SDK](https://github.com/bridgee-ai/bridgee-react-native-sdk)
- 🐛 [Reportar Issues](https://github.com/bridgee-ai/bridgee-android-sdk/issues)
- 💬 [Suporte Técnico](mailto:support@bridgee.ai)

**Desenvolvido com ❤️ pela equipe Bridgee.ai**  
