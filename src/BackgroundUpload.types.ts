import type { StyleProp, ViewStyle } from 'react-native';

export type OnLoadEventPayload = {
  url: string;
};

export type UploadProgressEventPayload = {
  uploadId: string;
  progress: number; // 0 to 1
  bytesUploaded: number;
  totalBytes: number;
};

export type UploadCompleteEventPayload = {
  uploadId: string;
  success: boolean;
  response?: string;
  error?: string;
  statusCode?: number;
};

export type BackgroundUploadModuleEvents = {
  onChange: (params: ChangeEventPayload) => void;
  onUploadProgress: (params: UploadProgressEventPayload) => void;
  onUploadComplete: (params: UploadCompleteEventPayload) => void;
};

export type ChangeEventPayload = {
  value: string;
};

export type UploadOptions = {
  url: string;
  headers?: Record<string, string>;
  fieldName?: string;
  fileName?: string;
  contentType?: string;
};

export type BackgroundUploadViewProps = {
  url: string;
  onLoad: (event: { nativeEvent: OnLoadEventPayload }) => void;
  style?: StyleProp<ViewStyle>;
};
