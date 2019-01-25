## Q & A

<<<<<<< HEAD
 + [meta-data android 26](#meta-data-android-26)
 + [Manifest merger failed](#when-i-try-to-build-my-project-i-get-following-error)
 + [How can I resize captured images?](#how-can-i-resize-captured-images)

#### meta-data android 26
=======
- [meta-data android 26](#meta-data-android-26)
- [Manifest merger failed](#when-i-try-to-build-my-project-i-get-following-error)
- [How can I resize captured images?](#how-can-i-resize-captured-images)

#### meta-data android 26

>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
```
  AndroidManifest.xml:25:13-35 Error:
       Attribute meta-data#android.support.VERSION@value value=(26.0.2) from [com.android.support:exifinterface:26.0.2] Android
  Manifest.xml:25:13-35
    is also present at [com.android.support:support-v4:26.0.1] AndroidManifest.xml:28:13-35 value=(26.0.1).
          Suggestion: add 'tools:replace="android:value"' to <meta-data> element at AndroidManifest.xml:23:9-25:38 to override.
```

Add this to your AndroidManifest.xml:

- [ ]           xmlns:tools="http://schemas.android.com/tools"

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          xmlns:tools="http://schemas.android.com/tools"
```
<<<<<<< HEAD
- [ ] tools:node="replace"
=======

- [ ] tools:node="replace"

>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
```xml
<application
      android:name=".MainApplication"
      android:allowBackup="true"
      android:label="@string/app_name"
      android:icon="@mipmap/ic_launcher"
      android:theme="@style/AppTheme"
      tools:node="replace"
    >
```

---

#### When I try to build my project, I get following error:
<<<<<<< HEAD
=======

>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
```
Execution failed for task ':app:processDebugManifest'.
> Manifest merger failed : Attribute meta-data#android.support.VERSION@value value=(26.0.2) from [com.android.support:exifinterface:26.0.2] AndroidManifest.xml:25:13-35
        is also present at [com.android.support:support-v4:26.0.1] AndroidManifest.xml:28:13-35 value=(26.0.1).
        Suggestion: add 'tools:replace="android:value"' to <meta-data> element at AndroidManifest.xml:23:9-25:38 to override.
```
<<<<<<< HEAD
#### As the error message hints `com.android.support:exifinterface:26.0.2` is already found in `com.android.support:support-v4:26.0.1`
To fix this issue, modify your project's `android/app/build.gradle` as follows:
=======

#### As the error message hints `com.android.support:exifinterface:26.0.2` is already found in `com.android.support:support-v4:26.0.1`

To fix this issue, modify your project's `android/app/build.gradle` as follows:

>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
```Gradle
dependencies {
    compile (project(':react-native-camera')) {
        exclude group: "com.android.support"

        // uncomment this if also com.google.android.gms:play-services-vision versions are conflicting
        // this can happen if you use react-native-firebase
        // exclude group: "com.google.android.gms"
    }

    compile ('com.android.support:exifinterface:26.0.1') {
        force = true;
    }

    // uncomment this if you uncommented the previous line
    // compile ('com.google.android.gms:play-services-vision:11.6.0') {
    //    force = true;
    // }
}
```

---

#### How can I resize captured images?

Currently, `RNCamera` does not allow for specifying the desired resolution of the captured image, nor does it natively expose any functionality to resize images.
One way to achieve this (without any additional dependencies )is using [react-native.ImageEditor.cropImage](https://facebook.github.io/react-native/docs/imageeditor.html#cropimage).

The strategy is:

<<<<<<< HEAD
 1. Capture an image using `RNCamera`, which uses the device's max resolution.
 2. Use `react-native.ImageEditor.cropImage()` to crop the image using the image's native size as the crop size (thus maintaiing the original image), and the desired new size as the `displaySize` attribute (thus resizing the image).
 
=======
1.  Capture an image using `RNCamera`, which uses the device's max resolution.
2.  Use `react-native.ImageEditor.cropImage()` to crop the image using the image's native size as the crop size (thus maintaiing the original image), and the desired new size as the `displaySize` attribute (thus resizing the image).

>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
```javascript
import React, { Component } from 'react';
import { Button, ImageEditor } from 'react-native';
import { RNCamera } from 'react-native-camera';

class CameraComponent extends Component {
  // ...
<<<<<<< HEAD
  
=======

>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
  capturePicture = function () {
    if (this.camera) {
      // 1) Capture the image using RNCamera API
      this.camera.takePictureAsync(options)
        .then((capturedImg) => {
<<<<<<< HEAD
          // 2a) Extract a reference to the captured image, 
          //    along with its natural dimensions
          const { uri, width, height } = capturedImg;
            
=======
          // 2a) Extract a reference to the captured image,
          //    along with its natural dimensions
          const { uri, width, height } = capturedImg;

>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
          const cropData = {
            // 2b) By cropping from (0, 0) to (imgWidth, imgHeight),
            //    we maintain the original image's dimensions
            offset: { x: 0, y: 0 },
            size: { width, height },
<<<<<<< HEAD
              
            // 2c) Use the displaySize option to specify the new image size
            displaySize: { width: RESIZED_WIDTH, height: RESIZED_HEIGHT },
          };
        
=======

            // 2c) Use the displaySize option to specify the new image size
            displaySize: { width: RESIZED_WIDTH, height: RESIZED_HEIGHT },
          };

>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
          ImageEditor.cropImage(uri, cropData, (resizedImage) => {
              // resizedImage == 'file:///data/.../img.jpg'
            }, (error) => {
              console.error('Error resizing image: ', error.getMessage());
          });
        })
        .catch((error) => {
          console.log('Could not capture image.', error.getMessage());
        });
    }
<<<<<<< HEAD
  
=======

>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
  render() {
    return (
      <RNCamera
        ref={ref => (this.camera = ref)}
        // ...
      />
<<<<<<< HEAD
      
=======

>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
      <Button onPress={() => this.capturePicture()}>Capture</Button>
    );
  }
}

```
