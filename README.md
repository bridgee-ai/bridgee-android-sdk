# Bridgee.ai - Guia de Implantação

## 📖 Introdução
O **Bridgee.ai** é uma solução de atribuição que conecta suas campanhas de aquisição de usuários aos eventos de instalação e abertura do seu aplicativo.  
Ele funciona em duas etapas principais:

1. **Bridgee Blink (proxy de links):** captura dados de atribuição quando o usuário clica em um link para sua loja de aplicativos.  
2. **Bridgee SDK:** atribui corretamente a instalação no primeiro uso do aplicativo, enviando os dados já enriquecidos ao **Firebase Analytics / GA4**.

---

## ⚙️ Como funciona

### 1. Bridgee Blink
Todo link que leva à loja de aplicativos deve passar por um proxy que chamamos de **Bridgee Blink**.  
Quando o usuário acessa o link Blink:
- Ele verá um pequeno "piscar" antes de ser redirecionado à loja.  
- Nesse momento, coletamos os parâmetros necessários para o **match de atribuição**.  

Exemplo de link original:
```
https://play.google.com/store/apps/details?id=com.je7ov.exampleapp
```

Exemplo de link com Bridgee Blink:
```
https://download.example.com/store/apps/details?id=com.je7ov.exampleapp
```

> O subdomínio (`download.example.com`) é definido em comum acordo com sua empresa, e precisa apontar para os servidores da Bridgee.

---

### 2. Bridgee SDK
Após a instalação e **primeira abertura do app**:
- O SDK do Bridgee dispara o evento `first_open`.  
- Nesse momento, vinculamos os **UTMs capturados no Blink** à instalação.  
- O evento é então enviado ao **Firebase Analytics (GA4)** já com a atribuição correta.  

---

## 🚀 Guia de implantação

### Passo 1 - Configurar o Blink
1. Defina um subdomínio livre, por exemplo:  
   ```
   download.example.com
   ```
2. Solicite ao time de TI da sua empresa que faça o **apontamento DNS** do subdomínio para o **IP dos servidores Bridgee** (fornecido pela nossa equipe).  
3. Após a configuração, a equipe Bridgee habilitará o Blink e fornecerá a URL final.  
4. Use a nova URL (com Blink) em todas as suas campanhas, e-mails e peças de marketing.  

---

### Passo 2 - Integrar o SDK no aplicativo Android
1. Garanta que seu aplicativo já esteja integrado ao **Firebase Analytics SDK**.  
2. Adicione a dependência do Bridgee SDK no seu `build.gradle`:  
   ```gradle
   implementation 'ai.bridgee:bridgee-android-sdk:1.0.0'
   ```
   > Consulte sempre a versão mais recente em:  
   > [Maven Repository - Bridgee SDK](https://mvnrepository.com/artifact/ai.bridgee/bridgee-android-sdk)

3. Instancie o SDK do Bridgee passando o objeto do Firebase Analytics, sua **API Key** e **API Secret**:  
   ```java
   BridgeeSdk bridgee = new BridgeeSdk(firebaseAnalytics, apikey, apisecret);
   ```

4. No evento de **primeira abertura (first_open)**, acione o SDK:  
   ```java
   bridgee.logEvent("first_open");
   ```

Pronto ✅ — a integração estará concluída.

---

## 📋 Requisitos técnicos
- App já integrado ao **Firebase Analytics**.  
- Permissão para configurar/apontar DNS para o Blink.  
- Android SDK disponível via Maven.  
- Credenciais **API Key** e **API Secret** fornecidas pela Bridgee.  

---

## 🛠 Roadmap de SDKs
Atualmente disponível:
- ✅ Android SDK

Em breve:
- ⏳ iOS SDK  
- ⏳ React Native SDK  
- ⏳ Flutter SDK  

---

## 📞 Suporte
Em caso de dúvidas ou suporte técnico, entre em contato com a equipe Bridgee.  
