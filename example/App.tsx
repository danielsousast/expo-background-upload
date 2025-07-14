import * as DocumentPicker from 'expo-document-picker';
import { BackgroundUpload } from 'background-upload';
import { useState, useEffect } from 'react';
import { Alert, Button, SafeAreaView, ScrollView, Text, View } from 'react-native';

export default function App() {
  const [uploadProgress, setUploadProgress] = useState<{[key: string]: number}>({});
  const [uploadStatus, setUploadStatus] = useState<{[key: string]: string}>({});
  const [activeUploads, setActiveUploads] = useState<Set<string>>(new Set());
  
  // Listen to upload progress and completion events
  useEffect(() => {
    const progressListener = BackgroundUpload.addListener('onUploadProgress', (event) => {
      console.log('Upload progress:', event);
      if (event?.uploadId && event?.progress !== undefined) {
        setUploadProgress(prev => ({
          ...prev,
          [event.uploadId]: event.progress
        }));
      }
    });
    
    const completeListener = BackgroundUpload.addListener('onUploadComplete', (event) => {
      console.log('Upload complete:', event);
      if (event?.uploadId) {
        setUploadStatus(prev => ({
          ...prev,
          [event.uploadId]: event?.success ? 'Success' : `Failed: ${event?.error || 'Unknown error'}`
        }));
        
        // Remove from active uploads
        setActiveUploads(prev => {
          const newSet = new Set(prev);
          newSet.delete(event.uploadId);
          return newSet;
        });
        
        if (event?.success) {
          Alert.alert('Upload Complete', `Upload ${event.uploadId} completed successfully!`);
        } else {
          Alert.alert('Upload Failed', `Upload ${event.uploadId} failed: ${event?.error || 'Unknown error'}`);
        }
      }
    });
    
    return () => {
      progressListener?.remove();
      completeListener?.remove();
    };
  }, []);
  
  const pickAndUploadFile = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: '*/*',
        copyToCacheDirectory: true,
      });
      
      if (!result.canceled && result.assets[0]) {
        const file = result.assets[0];
        
        const uploadId = await BackgroundUpload.startUploadAsync(file.uri, {
          url: 'https://httpbin.org/post', // Test endpoint
          headers: {
            'Authorization': 'Bearer your-token-here',
          },
          fieldName: 'file',
          fileName: file.name || 'upload.bin',
          contentType: file.mimeType || 'application/octet-stream',
        });
        
        // Add to active uploads
        setActiveUploads(prev => new Set([...prev, uploadId]));
        
        console.log('Upload started with ID:', uploadId);
        Alert.alert('Upload Started', `Upload ID: ${uploadId}`);
      }
    } catch (error) {
      console.error('Error picking/uploading file:', error);
      Alert.alert('Error', 'Failed to pick or upload file');
    }
  };
  
  const cancelUpload = (uploadId: string) => {
    const success = BackgroundUpload.cancelUpload(uploadId);
    if (success) {
      setActiveUploads(prev => {
        const newSet = new Set(prev);
        newSet.delete(uploadId);
        return newSet;
      });
      setUploadStatus(prev => ({
        ...prev,
        [uploadId]: 'Cancelled'
      }));
      Alert.alert('Upload Cancelled', `Upload ${uploadId} was cancelled`);
    } else {
      Alert.alert('Error', 'Failed to cancel upload');
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView style={styles.container}>
        <Text style={styles.header}>Background Upload Example</Text>
        
        <Group name="File Upload">
          <Button
            title="Pick and Upload File"
            onPress={pickAndUploadFile}
          />
          <Text style={styles.description}>
            This will pick a file and upload it in the background using WorkManager (Android) or URLSession (iOS).
          </Text>
        </Group>
        
        <Group name="Upload Progress">
          {Object.entries(uploadProgress).map(([uploadId, progress]) => (
            <View key={uploadId} style={styles.progressItem}>
              <View style={styles.progressInfo}>
                <Text style={styles.uploadId}>ID: {uploadId.substring(0, 8)}...</Text>
                <Text style={styles.progress}>Progress: {Math.round(progress * 100)}%</Text>
              </View>
              {activeUploads.has(uploadId) && (
                <Button
                  title="Cancel"
                  onPress={() => cancelUpload(uploadId)}
                  color="#FF3B30"
                />
              )}
            </View>
          ))}
          {Object.keys(uploadProgress).length === 0 && (
            <Text style={styles.noUploads}>No active uploads</Text>
          )}
        </Group>
        
        <Group name="Upload Status">
          {Object.entries(uploadStatus).map(([uploadId, status]) => (
            <View key={uploadId} style={styles.statusItem}>
              <Text style={styles.uploadId}>ID: {uploadId.substring(0, 8)}...</Text>
              <Text style={styles.status}>Status: {status}</Text>
            </View>
          ))}
          {Object.keys(uploadStatus).length === 0 && (
            <Text style={styles.noUploads}>No completed uploads</Text>
          )}
        </Group>
      </ScrollView>
    </SafeAreaView>
  );
}

function Group(props: { name: string; children: React.ReactNode }) {
  return (
    <View style={styles.group}>
      <Text style={styles.groupHeader}>{props.name}</Text>
      {props.children}
    </View>
  );
}

const styles = {
  header: {
    fontSize: 30,
    margin: 20,
    textAlign: 'center' as const,
    fontWeight: 'bold' as const,
  },
  groupHeader: {
    fontSize: 20,
    marginBottom: 20,
    fontWeight: 'bold' as const,
  },
  group: {
    margin: 20,
    backgroundColor: '#fff',
    borderRadius: 10,
    padding: 20,
  },
  container: {
    flex: 1,
    backgroundColor: '#eee',
  },
  description: {
    marginTop: 10,
    fontSize: 14,
    color: '#666',
    fontStyle: 'italic' as const,
  },
  progressItem: {
    flexDirection: 'row' as const,
    justifyContent: 'space-between' as const,
    alignItems: 'center' as const,
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  progressInfo: {
    flex: 1,
  },
  statusItem: {
    flexDirection: 'row' as const,
    justifyContent: 'space-between' as const,
    alignItems: 'center' as const,
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  uploadId: {
    fontSize: 12,
    color: '#666',
    fontFamily: 'monospace' as const,
  },
  progress: {
    fontSize: 14,
    fontWeight: 'bold' as const,
    color: '#007AFF',
  },
  status: {
    fontSize: 14,
    fontWeight: 'bold' as const,
  },
  noUploads: {
    fontSize: 14,
    color: '#999',
    fontStyle: 'italic' as const,
    textAlign: 'center' as const,
    paddingVertical: 20,
  },
};
