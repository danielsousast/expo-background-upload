# Background Upload

Um módulo nativo moderno para Expo que permite uploads de arquivos em background tanto em projetos Expo (com Dev Client e EAS Build) quanto em React Native CLI.

## Características

- ✅ **Upload em Background**: Funciona mesmo quando o app está em background
- ✅ **Multiplataforma**: iOS (URLSession) e Android (WorkManager)
- ✅ **Progresso em Tempo Real**: Eventos de progresso de 0 a 1
- ✅ **Confiável**: Usa WorkManager no Android e URLSession no iOS
- ✅ **Customizável**: Headers, fieldName, fileName e contentType
- ✅ **TypeScript**: Totalmente tipado

## Instalação

```bash
npm install background-upload
# ou
yarn add background-upload
```

### Configuração iOS

Adicione as seguintes capacidades no seu `Info.plist`:

```xml
<key>UIBackgroundModes</key>
<array>
  <string>background-processing</string>
  <string>background-fetch</string>
</array>
```

### Configuração Android

As permissões necessárias já estão incluídas no módulo:

- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `READ_EXTERNAL_STORAGE`
- `WAKE_LOCK`

## Uso

### Importação

```typescript
import { BackgroundUpload } from 'background-upload';
import type { UploadOptions, UploadProgressEventPayload, UploadCompleteEventPayload } from 'background-upload';
```

### Upload Básico

```typescript
const uploadFile = async () => {
  try {
    const uploadId = await BackgroundUpload.startUploadAsync(fileUri, {
      url: 'https://your-server.com/upload',
      headers: {
        'Authorization': 'Bearer your-token',
        'X-Custom-Header': 'value'
      },
      fieldName: 'file',
      fileName: 'document.pdf',
      contentType: 'application/pdf'
    });
    
    console.log('Upload iniciado:', uploadId);
  } catch (error) {
    console.error('Erro no upload:', error);
  }
};
```

### Monitorando Progresso

```typescript
import { useEvent } from 'expo';

function MyComponent() {
  // Escutar eventos de progresso
  useEvent(BackgroundUpload, 'onUploadProgress', (event: UploadProgressEventPayload) => {
    console.log(`Upload ${event.uploadId}: ${Math.round(event.progress * 100)}%`);
    console.log(`${event.bytesUploaded} / ${event.totalBytes} bytes`);
  });
  
  // Escutar eventos de conclusão
  useEvent(BackgroundUpload, 'onUploadComplete', (event: UploadCompleteEventPayload) => {
    if (event.success) {
      console.log('Upload concluído:', event.response);
    } else {
      console.error('Upload falhou:', event.error);
    }
  });
  
  return (
    // Seu componente
  );
}
```

## API

### `startUploadAsync(fileUri: string, options: UploadOptions): Promise<string>`

Inicia um upload em background.

**Parâmetros:**
- `fileUri`: URI do arquivo a ser enviado
- `options`: Opções de configuração do upload

**Retorna:** Promise que resolve com o ID único do upload

### `UploadOptions`

```typescript
interface UploadOptions {
  url: string;                    // URL de destino (obrigatório)
  headers?: Record<string, string>; // Headers HTTP opcionais
  fieldName?: string;             // Nome do campo no form (padrão: "file")
  fileName?: string;              // Nome do arquivo (padrão: "upload")
  contentType?: string;           // Tipo de conteúdo (padrão: "application/octet-stream")
}
```

### Eventos

#### `onUploadProgress`

Emitido durante o upload com informações de progresso.

```typescript
interface UploadProgressEventPayload {
  uploadId: string;    // ID do upload
  progress: number;    // Progresso de 0 a 1
  bytesUploaded: number; // Bytes enviados
  totalBytes: number;  // Total de bytes
}
```

#### `onUploadComplete`

Emitido quando o upload é concluído (sucesso ou falha).

```typescript
interface UploadCompleteEventPayload {
  uploadId: string;     // ID do upload
  success: boolean;     // Se o upload foi bem-sucedido
  response?: string;    // Resposta do servidor (se sucesso)
  error?: string;       // Mensagem de erro (se falha)
  statusCode?: number;  // Código de status HTTP
}
```

## Exemplo Completo

Veja o arquivo `example/App.tsx` para um exemplo completo de uso com seleção de arquivos e monitoramento de progresso.

## Implementação Técnica

### Android
- Usa **WorkManager** para gerenciar uploads em background de forma confiável
- Usa **OkHttp** para realizar as requisições HTTP
- Suporta uploads mesmo quando o app está fechado

### iOS
- Usa **URLSession** com `backgroundSessionConfiguration`
- Implementa delegates para progresso e finalização
- Suporta uploads em background com notificações do sistema

## Limitações

- No iOS, uploads em background podem ser pausados pelo sistema em condições de baixa bateria
- No Android, uploads podem ser limitados por otimizações de bateria do dispositivo
- Arquivos muito grandes podem ser rejeitados por alguns servidores

## Contribuição

Contribuições são bem-vindas! Por favor, abra uma issue ou pull request.

## Licença

MIT