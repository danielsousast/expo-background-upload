import ExpoModulesCore
import Foundation

public class BackgroundUploadModule: Module {
  private var backgroundSession: URLSession?
  private var activeUploads: [String: URLSessionUploadTask] = [:]
  private var uploadOptions: [String: [String: Any]] = [:]
  
  public func definition() -> ModuleDefinition {
    Name("BackgroundUpload")

    Events("onUploadProgress", "onUploadComplete")
    
    OnCreate {
      setupBackgroundSession()
    }

    AsyncFunction("startUploadAsync") { (fileUri: String, options: [String: Any], promise: Promise) in
      do {
        let uploadId = UUID().uuidString
        
        guard let url = options["url"] as? String,
              let uploadURL = URL(string: url) else {
          promise.reject("INVALID_URL", "Invalid or missing URL")
          return
        }
        
        guard let fileURL = self.getFileURL(from: fileUri) else {
          promise.reject("INVALID_FILE", "Invalid file URI")
          return
        }
        
        let headers = options["headers"] as? [String: String] ?? [:]
        let fieldName = options["fieldName"] as? String ?? "file"
        let fileName = options["fileName"] as? String ?? "upload"
        let contentType = options["contentType"] as? String ?? "application/octet-stream"
        
        // Store upload options for later use
        self.uploadOptions[uploadId] = options
        
        let task = try self.createUploadTask(
          uploadId: uploadId,
          fileURL: fileURL,
          uploadURL: uploadURL,
          headers: headers,
          fieldName: fieldName,
          fileName: fileName,
          contentType: contentType
        )
        
        self.activeUploads[uploadId] = task
        task.resume()
        
        promise.resolve(uploadId)
      } catch {
        promise.reject("UPLOAD_ERROR", error.localizedDescription)
      }
    }
    
    Function("cancelUpload") { (uploadId: String) -> Bool in
      if let task = self.activeUploads[uploadId] {
        task.cancel()
        self.activeUploads.removeValue(forKey: uploadId)
        self.uploadOptions.removeValue(forKey: uploadId)
        return true
      }
      return false
    }
  }
  
  private func setupBackgroundSession() {
    let config = URLSessionConfiguration.background(withIdentifier: "expo.modules.backgroundupload.session")
    config.isDiscretionary = false
    config.sessionSendsLaunchEvents = true
    
    backgroundSession = URLSession(
      configuration: config,
      delegate: BackgroundUploadDelegate(module: self),
      delegateQueue: nil
    )
  }
  
  private func getFileURL(from uriString: String) -> URL? {
    if uriString.hasPrefix("file://") {
      return URL(string: uriString)
    } else if uriString.hasPrefix("/") {
      return URL(fileURLWithPath: uriString)
    } else {
      return URL(string: uriString)
    }
  }
  
  private func createUploadTask(
    uploadId: String,
    fileURL: URL,
    uploadURL: URL,
    headers: [String: String],
    fieldName: String,
    fileName: String,
    contentType: String
  ) throws -> URLSessionUploadTask {
    
    var request = URLRequest(url: uploadURL)
    request.httpMethod = "POST"
    
    // Add custom headers
    for (key, value) in headers {
      request.setValue(value, forHTTPHeaderField: key)
    }
    
    // Create multipart form data
    let boundary = "Boundary-\(UUID().uuidString)"
    request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
    
    let multipartData = try createMultipartData(
      fileURL: fileURL,
      boundary: boundary,
      fieldName: fieldName,
      fileName: fileName,
      contentType: contentType
    )
    
    // Write multipart data to temporary file
    let tempURL = FileManager.default.temporaryDirectory.appendingPathComponent("upload_\(uploadId).tmp")
    try multipartData.write(to: tempURL)
    
    guard let session = backgroundSession else {
      throw NSError(domain: "BackgroundUpload", code: -1, userInfo: [NSLocalizedDescriptionKey: "Background session not initialized"])
    }
    
    return session.uploadTask(with: request, fromFile: tempURL)
  }
  
  private func createMultipartData(
    fileURL: URL,
    boundary: String,
    fieldName: String,
    fileName: String,
    contentType: String
  ) throws -> Data {
    var data = Data()
    
    // Add file data
    data.append("--\(boundary)\r\n".data(using: .utf8)!)
    data.append("Content-Disposition: form-data; name=\"\(fieldName)\"; filename=\"\(fileName)\"\r\n".data(using: .utf8)!)
    data.append("Content-Type: \(contentType)\r\n\r\n".data(using: .utf8)!)
    
    let fileData = try Data(contentsOf: fileURL)
    data.append(fileData)
    
    data.append("\r\n--\(boundary)--\r\n".data(using: .utf8)!)
    
    return data
  }
  
  internal func sendProgressEvent(uploadId: String, progress: Double, bytesUploaded: Int64, totalBytes: Int64) {
    sendEvent("onUploadProgress", [
      "uploadId": uploadId,
      "progress": progress,
      "bytesUploaded": bytesUploaded,
      "totalBytes": totalBytes
    ])
  }
  
  internal func sendCompleteEvent(uploadId: String, success: Bool, response: String?, error: String?, statusCode: Int) {
    var eventData: [String: Any] = [
      "uploadId": uploadId,
      "success": success,
      "statusCode": statusCode
    ]
    
    if let response = response {
      eventData["response"] = response
    }
    
    if let error = error {
      eventData["error"] = error
    }
    
    sendEvent("onUploadComplete", eventData)
    
    // Cleanup
    activeUploads.removeValue(forKey: uploadId)
    uploadOptions.removeValue(forKey: uploadId)
  }
  
  internal func getUploadId(for task: URLSessionTask) -> String? {
    return activeUploads.first { $0.value == task }?.key
  }
}