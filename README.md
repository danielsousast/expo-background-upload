# Background Upload

A modern native module for Expo that enables background file uploads for both Expo projects (with Dev Client and EAS Build) and React Native CLI.

## Features

- ✅ **Background Upload**: Works even when the app is in background
- ✅ **Cross-platform**: iOS (URLSession) and Android (WorkManager)
- ✅ **Real-time Progress**: Progress events from 0 to 1
- ✅ **Reliable**: Uses WorkManager on Android and URLSession on iOS
- ✅ **Customizable**: Headers, fieldName, fileName and contentType
- ✅ **TypeScript**: Fully typed

## Installation

```bash
npm install background-upload
# ou
yarn add background-upload
```

### iOS Configuration

Add the following capabilities to your `Info.plist`:

```xml
<key>UIBackgroundModes</key>
<array>
  <string>background-processing</string>
  <string>background-fetch</string>
</array>
```

### Android Configuration

Required permissions are already included in the module:

- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `READ_EXTERNAL_STORAGE`
- `WAKE_LOCK`

## Usage

### Import

```typescript
import { BackgroundUpload } from 'background-upload';
import type { UploadOptions, UploadProgressEventPayload, UploadCompleteEventPayload } from 'background-upload';
```

### Basic Upload

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
    
    console.log('Upload started:', uploadId);
  } catch (error) {
    console.error('Upload error:', error);
  }
};
```

### Monitoring Progress

```typescript
import { useEvent } from 'expo';

function MyComponent() {
  // Listen to progress events
  useEvent(BackgroundUpload, 'onUploadProgress', (event: UploadProgressEventPayload) => {
    console.log(`Upload ${event.uploadId}: ${Math.round(event.progress * 100)}%`);
    console.log(`${event.bytesUploaded} / ${event.totalBytes} bytes`);
  });
  
  // Listen to completion events
  useEvent(BackgroundUpload, 'onUploadComplete', (event: UploadCompleteEventPayload) => {
    if (event.success) {
      console.log('Upload completed:', event.response);
    } else {
      console.error('Upload failed:', event.error);
    }
  });
  
  return (
    // Your component
  );
}
```

## API

### `startUploadAsync(fileUri: string, options: UploadOptions): Promise<string>`

Starts a background upload.

**Parameters:**
- `fileUri`: URI of the file to be uploaded
- `options`: Upload configuration options

**Returns:** Promise that resolves with the unique upload ID

### `UploadOptions`

```typescript
interface UploadOptions {
  url: string;                    // Target URL (required)
  headers?: Record<string, string>; // Optional HTTP headers
  fieldName?: string;             // Form field name (default: "file")
  fileName?: string;              // File name (default: "upload")
  contentType?: string;           // Content type (default: "application/octet-stream")
}
```

### Events

#### `onUploadProgress`

Emitted during upload with progress information.

```typescript
interface UploadProgressEventPayload {
  uploadId: string;    // Upload ID
  progress: number;    // Progress from 0 to 1
  bytesUploaded: number; // Bytes uploaded
  totalBytes: number;  // Total bytes
}
```

#### `onUploadComplete`

Emitted when upload is completed (success or failure).

```typescript
interface UploadCompleteEventPayload {
  uploadId: string;     // Upload ID
  success: boolean;     // Whether upload was successful
  response?: string;    // Server response (if success)
  error?: string;       // Error message (if failure)
  statusCode?: number;  // HTTP status code
}
```

## Complete Example

See the `example/App.tsx` file for a complete usage example with file selection and progress monitoring.

## Technical Implementation

### Android
- Uses **WorkManager** to reliably manage background uploads
- Uses **OkHttp** to perform HTTP requests
- Supports uploads even when the app is closed

### iOS
- Uses **URLSession** with `backgroundSessionConfiguration`
- Implements delegates for progress and completion
- Supports background uploads with system notifications

## Limitations

- On iOS, background uploads may be paused by the system under low battery conditions
- On Android, uploads may be limited by device battery optimizations
- Very large files may be rejected by some servers

## Contributing

Contributions are welcome! Please open an issue or pull request.

## License

MIT