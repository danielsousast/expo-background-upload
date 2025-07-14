// Reexport the native module. On web, it will be resolved to BackgroundUploadModule.web.ts
// and on native platforms to BackgroundUploadModule.ts
export { default } from './BackgroundUploadModule';
export { default as BackgroundUploadView } from './BackgroundUploadView';
export * from './BackgroundUpload.types';

// Re-export the module for easier access
import BackgroundUploadModule from './BackgroundUploadModule';

export const BackgroundUpload = {
  startUploadAsync: BackgroundUploadModule.startUploadAsync.bind(BackgroundUploadModule),
  cancelUpload: BackgroundUploadModule.cancelUpload.bind(BackgroundUploadModule),
  addListener: BackgroundUploadModule.addListener?.bind(BackgroundUploadModule),
  removeListener: BackgroundUploadModule.removeListener?.bind(BackgroundUploadModule),
  removeAllListeners: BackgroundUploadModule.removeAllListeners?.bind(BackgroundUploadModule),
};
