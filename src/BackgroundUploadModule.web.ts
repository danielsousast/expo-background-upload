import { registerWebModule, NativeModule } from 'expo';

import { BackgroundUploadModuleEvents, UploadOptions } from './BackgroundUpload.types';

class BackgroundUploadModule extends NativeModule<BackgroundUploadModuleEvents> {
  PI = Math.PI;
  
  async setValueAsync(value: string): Promise<void> {
    this.emit('onChange', { value });
  }
  
  hello() {
    return 'Hello world! 👋';
  }
  
  async startUploadAsync(fileUri: string, options: UploadOptions): Promise<string> {
    // Web implementation - simulate upload for development
    const uploadId = Math.random().toString(36).substr(2, 9);
    
    console.warn('BackgroundUpload: Web platform detected. Upload simulation started.');
    
    // Simulate progress events
    let progress = 0;
    const interval = setInterval(() => {
      progress += 0.1;
      this.emit('onUploadProgress', {
        uploadId,
        progress: Math.min(progress, 1),
        bytesUploaded: Math.floor(progress * 1000000),
        totalBytes: 1000000
      });
      
      if (progress >= 1) {
        clearInterval(interval);
        // Simulate completion
        setTimeout(() => {
          this.emit('onUploadComplete', {
            uploadId,
            success: true,
            response: 'Simulated upload complete',
            statusCode: 200
          });
        }, 100);
      }
    }, 200);
    
    return uploadId;
  }
  
  cancelUpload(uploadId: string): boolean {
    console.warn('BackgroundUpload: Web platform detected. Cancel simulation.');
    // In a real implementation, you would cancel the actual upload
    return true;
  }
}

export default registerWebModule(BackgroundUploadModule, 'BackgroundUploadModule');
