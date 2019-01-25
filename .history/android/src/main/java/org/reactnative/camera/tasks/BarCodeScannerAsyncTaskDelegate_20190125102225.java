package org.reactnative.camera.tasks;

import com.google.zxing.Result;

public interface BarCodeScannerAsyncTaskDelegate {
<<<<<<< HEAD
  void onBarCodeRead(Result barCode);
=======
  void onBarCodeRead(Result barCode, int width, int height);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
  void onBarCodeScanningTaskCompleted();
}
