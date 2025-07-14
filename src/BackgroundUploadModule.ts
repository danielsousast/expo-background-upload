import { NativeModule, requireNativeModule } from 'expo';

import { BackgroundUploadModuleEvents, UploadOptions } from './BackgroundUpload.types';

declare class BackgroundUploadModule extends NativeModule<BackgroundUploadModuleEvents> {
  PI: number;
  hello(): string;
  setValueAsync(value: string): Promise<void>;
  startUploadAsync(fileUri: string, options: UploadOptions): Promise<string>;
  cancelUpload(uploadId: string): boolean;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<BackgroundUploadModule>('BackgroundUpload');
