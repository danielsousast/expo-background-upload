import Foundation
import ExpoModulesCore

class BackgroundUploadDelegate: NSObject, URLSessionDelegate, URLSessionTaskDelegate, URLSessionDataDelegate {
  private weak var module: BackgroundUploadModule?
  
  init(module: BackgroundUploadModule) {
    self.module = module
    super.init()
  }
  
  // MARK: - URLSessionTaskDelegate
  
  func urlSession(_ session: URLSession, task: URLSessionTask, didSendBodyData bytesSent: Int64, totalBytesSent: Int64, totalBytesExpectedToSend: Int64) {
    guard let uploadId = module?.getUploadId(for: task) else { return }
    
    let progress = totalBytesExpectedToSend > 0 ? Double(totalBytesSent) / Double(totalBytesExpectedToSend) : 0.0
    
    DispatchQueue.main.async {
      self.module?.sendProgressEvent(
        uploadId: uploadId,
        progress: progress,
        bytesUploaded: totalBytesSent,
        totalBytes: totalBytesExpectedToSend
      )
    }
  }
  
  func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
    guard let uploadId = module?.getUploadId(for: task) else { return }
    
    DispatchQueue.main.async {
      if let error = error {
        self.module?.sendCompleteEvent(
          uploadId: uploadId,
          success: false,
          response: nil,
          error: error.localizedDescription,
          statusCode: 0
        )
      } else if let httpResponse = task.response as? HTTPURLResponse {
        let success = httpResponse.statusCode >= 200 && httpResponse.statusCode < 300
        
        if success {
          self.handleSuccessfulCompletion(for: task, uploadId: uploadId, statusCode: httpResponse.statusCode)
        } else {
          self.module?.sendCompleteEvent(
            uploadId: uploadId,
            success: false,
            response: nil,
            error: "HTTP Error \(httpResponse.statusCode)",
            statusCode: httpResponse.statusCode
          )
        }
      }
    }
  }
  
  // MARK: - URLSessionDataDelegate
  
  private var responseData: [URLSessionTask: Data] = [:]
  
  func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive data: Data) {
    if responseData[dataTask] == nil {
      responseData[dataTask] = Data()
    }
    responseData[dataTask]?.append(data)
  }
  
  func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive response: URLResponse, completionHandler: @escaping (URLSession.ResponseDisposition) -> Void) {
    completionHandler(.allow)
  }
  
  // MARK: - URLSessionDelegate
  
  func urlSession(_ session: URLSession, didBecomeInvalidWithError error: Error?) {
    // Handle session invalidation if needed
  }
  
  func urlSessionDidFinishEvents(forBackgroundURLSession session: URLSession) {
    // Called when all background session tasks have finished
    DispatchQueue.main.async {
      // Notify the app that background tasks are complete
      if let appDelegate = UIApplication.shared.delegate,
         let completionHandler = appDelegate.backgroundSessionCompletionHandler {
        completionHandler()
      }
    }
  }
  
  // Helper method to handle successful completion with response data
  private func handleSuccessfulCompletion(for task: URLSessionTask, uploadId: String, statusCode: Int) {
    let responseString: String?
    
    if let data = responseData[task] {
      responseString = String(data: data, encoding: .utf8)
      responseData.removeValue(forKey: task)
    } else {
      responseString = nil
    }
    
    module?.sendCompleteEvent(
      uploadId: uploadId,
      success: true,
      response: responseString,
      error: nil,
      statusCode: statusCode
    )
  }
}

// Extension to handle background completion
extension UIApplicationDelegate {
  var backgroundSessionCompletionHandler: (() -> Void)? {
    get {
      return objc_getAssociatedObject(self, &AssociatedKeys.backgroundSessionCompletionHandler) as? (() -> Void)
    }
    set {
      objc_setAssociatedObject(self, &AssociatedKeys.backgroundSessionCompletionHandler, newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
    }
  }
}

private struct AssociatedKeys {
  static var backgroundSessionCompletionHandler = "backgroundSessionCompletionHandler"
}