// @flow
import React from 'react';
import PropTypes from 'prop-types';
import {
  findNodeHandle,
  Platform,
  NativeModules,
  ViewPropTypes,
  requireNativeComponent,
  View,
  ActivityIndicator,
  Text,
  StyleSheet,
  PermissionsAndroid,
} from 'react-native';

import type { FaceFeature } from './FaceDetector';

const requestPermissions = async (
  captureAudio: boolean,
  CameraManager: any,
  permissionDialogTitle?: string,
  permissionDialogMessage?: string,
): Promise<{ hasCameraPermissions: boolean, hasRecordAudioPermissions: boolean }> => {
  let hasCameraPermissions = false;
  let hasRecordAudioPermissions = false;

  let params = undefined;
  if (permissionDialogTitle || permissionDialogMessage) {
    params = { title: permissionDialogTitle, message: permissionDialogMessage };
  }

  if (Platform.OS === 'ios') {
    hasCameraPermissions = await CameraManager.checkVideoAuthorizationStatus();
  } else if (Platform.OS === 'android') {
    const cameraPermissionResult = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.CAMERA,
      params,
    );
    hasCameraPermissions = cameraPermissionResult === PermissionsAndroid.RESULTS.GRANTED;
  }

  if (captureAudio) {
    if (Platform.OS === 'ios') {
      hasRecordAudioPermissions = await CameraManager.checkRecordAudioAuthorizationStatus();
    } else if (Platform.OS === 'android') {
      if (await CameraManager.checkIfRecordAudioPermissionsAreDefined()) {
        const audioPermissionResult = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,
          params,
        );
        hasRecordAudioPermissions = audioPermissionResult === PermissionsAndroid.RESULTS.GRANTED;
      } else if (__DEV__) {
        // eslint-disable-next-line no-console
        console.warn(
          `The 'captureAudio' property set on RNCamera instance but 'RECORD_AUDIO' permissions not defined in the applications 'AndroidManifest.xml'. ` +
            `If you want to record audio you will have to add '<uses-permission android:name="android.permission.RECORD_AUDIO"/>' to your 'AndroidManifest.xml'. ` +
            `Otherwise you should set the 'captureAudio' property on the component instance to 'false'.`,
        );
      }
    }
  }

  return {
    hasCameraPermissions,
    hasRecordAudioPermissions,
  };
};

const styles = StyleSheet.create({
  authorizationContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  notAuthorizedText: {
    textAlign: 'center',
    fontSize: 16,
  },
});

type Orientation = 'auto' | 'landscapeLeft' | 'landscapeRight' | 'portrait' | 'portraitUpsideDown';
type OrientationNumber = 1 | 2 | 3 | 4;

type PictureOptions = {
  quality?: number,
  orientation?: Orientation | OrientationNumber,
  base64?: boolean,
  mirrorImage?: boolean,
  exif?: boolean,
  width?: number,
  fixOrientation?: boolean,
  forceUpOrientation?: boolean,
  pauseAfterCapture?: boolean,
};

type TrackedFaceFeature = FaceFeature & {
  faceID?: number,
};

type TrackedTextFeature = {
  type: string,
  bounds: {
    size: {
      width: number,
      height: number,
    },
    origin: {
      x: number,
      y: number,
    },
  },
  value: string,
  components: Array<TrackedTextFeature>,
};

type RecordingOptions = {
  maxDuration?: number,
  maxFileSize?: number,
  orientation?: Orientation,
  quality?: number | string,
  codec?: string,
  mute?: boolean,
  path?: string,
  videoBitrate?: number,
};

type EventCallbackArgumentsType = {
  nativeEvent: Object,
};

type PropsType = typeof View.props & {
  zoom?: number,
  ratio?: string,
  focusDepth?: number,
  type?: number | string,
  onCameraReady?: Function,
  onBarCodeRead?: Function,
  onPictureSaved?: Function,
  exposureCompensation?: number,
  onGoogleVisionBarcodesDetected?: Function,
  faceDetectionMode?: number,
  flashMode?: number | string,
  barCodeTypes?: Array<string>,
  googleVisionBarcodeType?: number,
  googleVisionBarcodeMode?: number,
  whiteBalance?: number | string,
  faceDetectionLandmarks?: number,
  autoFocus?: string | boolean | number,
  autoFocusPointOfInterest?: { x: number, y: number },
  faceDetectionClassifications?: number,
  onFacesDetected?: ({ faces: Array<TrackedFaceFeature> }) => void,
  onTextRecognized?: ({ textBlocks: Array<TrackedTextFeature> }) => void,
  captureAudio?: boolean,
  useCamera2Api?: boolean,
  playSoundOnCapture?: boolean,
  videoStabilizationMode?: number | string,
  pictureSize?: string,
};

type StateType = {
  isAuthorized: boolean,
  isAuthorizationChecked: boolean,
  recordAudioPermissionStatus: RecordAudioPermissionStatus,
};

export type Status = 'READY' | 'PENDING_AUTHORIZATION' | 'NOT_AUTHORIZED';

const CameraStatus: { [key: Status]: Status } = {
  READY: 'READY',
  PENDING_AUTHORIZATION: 'PENDING_AUTHORIZATION',
  NOT_AUTHORIZED: 'NOT_AUTHORIZED',
};

export type RecordAudioPermissionStatus = 'AUTHORIZED' | 'NOT_AUTHORIZED' | 'PENDING_AUTHORIZATION';

const RecordAudioPermissionStatusEnum: {
  [key: RecordAudioPermissionStatus]: RecordAudioPermissionStatus,
} = {
  AUTHORIZED: 'AUTHORIZED',
  PENDING_AUTHORIZATION: 'PENDING_AUTHORIZATION',
  NOT_AUTHORIZED: 'NOT_AUTHORIZED',
};

const CameraManager: Object = NativeModules.RNCameraManager ||
  NativeModules.RNCameraModule || {
    stubbed: true,
    Type: {
      back: 1,
    },
    AutoFocus: {
      on: 1,
    },
    FlashMode: {
      off: 1,
    },
    WhiteBalance: {},
    BarCodeType: {},
    FaceDetection: {
      fast: 1,
      Mode: {},
      Landmarks: {
        none: 0,
      },
      Classifications: {
        none: 0,
      },
    },
    GoogleVisionBarcodeDetection: {
      BarcodeType: 0,
      BarcodeMode: 0,
    },
  };

const EventThrottleMs = 500;

const mapValues = (input, mapper) => {
  const result = {};
  Object.entries(input).map(([key, value]) => {
    result[key] = mapper(value, key);
  });
  return result;
};

export default class Camera extends React.Component<PropsType, StateType> {
  static Constants = {
    Type: CameraManager.Type,
    FlashMode: CameraManager.FlashMode,
    AutoFocus: CameraManager.AutoFocus,
    WhiteBalance: CameraManager.WhiteBalance,
    VideoQuality: CameraManager.VideoQuality,
    VideoCodec: CameraManager.VideoCodec,
    BarCodeType: CameraManager.BarCodeType,
    GoogleVisionBarcodeDetection: CameraManager.GoogleVisionBarcodeDetection,
    FaceDetection: CameraManager.FaceDetection,
    CameraStatus,
    RecordAudioPermissionStatus: RecordAudioPermissionStatusEnum,
    VideoStabilization: CameraManager.VideoStabilization,
    Orientation: {
      auto: 'auto',
      landscapeLeft: 'landscapeLeft',
      landscapeRight: 'landscapeRight',
      portrait: 'portrait',
      portraitUpsideDown: 'portraitUpsideDown',
    },
  };

  // Values under keys from this object will be transformed to native options
  static ConversionTables = {
    type: CameraManager.Type,
    flashMode: CameraManager.FlashMode,
<<<<<<< HEAD
    exposureCompensation: CameraManager.exposureCompensation,
=======
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    autoFocus: CameraManager.AutoFocus,
    whiteBalance: CameraManager.WhiteBalance,
    faceDetectionMode: (CameraManager.FaceDetection || {}).Mode,
    faceDetectionLandmarks: (CameraManager.FaceDetection || {}).Landmarks,
    faceDetectionClassifications: (CameraManager.FaceDetection || {}).Classifications,
    googleVisionBarcodeType: (CameraManager.GoogleVisionBarcodeDetection || {}).BarcodeType,
    videoStabilizationMode: CameraManager.VideoStabilization || {},
  };

  static propTypes = {
    ...ViewPropTypes,
    zoom: PropTypes.number,
    ratio: PropTypes.string,
    focusDepth: PropTypes.number,
    onMountError: PropTypes.func,
    onCameraReady: PropTypes.func,
    onBarCodeRead: PropTypes.func,
    onPictureSaved: PropTypes.func,
    onGoogleVisionBarcodesDetected: PropTypes.func,
    onFacesDetected: PropTypes.func,
    onTextRecognized: PropTypes.func,
    faceDetectionMode: PropTypes.number,
    faceDetectionLandmarks: PropTypes.number,
    faceDetectionClassifications: PropTypes.number,
    barCodeTypes: PropTypes.arrayOf(PropTypes.string),
    googleVisionBarcodeType: PropTypes.number,
<<<<<<< HEAD
    type: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    flashMode: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    iso: PropTypes.number,
    exposureCompensation: PropTypes.number,
    exposureDuration: PropTypes.shape({
      value: PropTypes.number.isRequired,
      scale: PropTypes.number.isRequired
    }),
    whiteBalance: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    autoFocus: PropTypes.oneOfType([PropTypes.string, PropTypes.number, PropTypes.bool]),
=======
    googleVisionBarcodeMode: PropTypes.number,
    type: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    flashMode: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    whiteBalance: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    autoFocus: PropTypes.oneOfType([PropTypes.string, PropTypes.number, PropTypes.bool]),
    autoFocusPointOfInterest: PropTypes.shape({ x: PropTypes.number, y: PropTypes.number }),
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    permissionDialogTitle: PropTypes.string,
    permissionDialogMessage: PropTypes.string,
    notAuthorizedView: PropTypes.element,
    pendingAuthorizationView: PropTypes.element,
    captureAudio: PropTypes.bool,
    useCamera2Api: PropTypes.bool,
    playSoundOnCapture: PropTypes.bool,
    videoStabilizationMode: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    pictureSize: PropTypes.string,
    mirrorVideo: PropTypes.bool,
<<<<<<< HEAD
=======
    defaultVideoQuality: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
  };

  static defaultProps: Object = {
    zoom: 0,
    ratio: '4:3',
    focusDepth: 0,
<<<<<<< HEAD
    exposureCompensation: 0,
=======
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    type: CameraManager.Type.back,
    autoFocus: CameraManager.AutoFocus.on,
    flashMode: CameraManager.FlashMode.off,
    whiteBalance: CameraManager.WhiteBalance.auto,
    faceDetectionMode: (CameraManager.FaceDetection || {}).fast,
    barCodeTypes: Object.values(CameraManager.BarCodeType),
    googleVisionBarcodeType: ((CameraManager.GoogleVisionBarcodeDetection || {}).BarcodeType || {})
      .None,
<<<<<<< HEAD
=======
    googleVisionBarcodeMode: ((CameraManager.GoogleVisionBarcodeDetection || {}).BarcodeMode || {})
      .NORMAL,
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    faceDetectionLandmarks: ((CameraManager.FaceDetection || {}).Landmarks || {}).none,
    faceDetectionClassifications: ((CameraManager.FaceDetection || {}).Classifications || {}).none,
    permissionDialogTitle: '',
    permissionDialogMessage: '',
    notAuthorizedView: (
      <View style={styles.authorizationContainer}>
        <Text style={styles.notAuthorizedText}>Camera not authorized</Text>
      </View>
    ),
    pendingAuthorizationView: (
      <View style={styles.authorizationContainer}>
        <ActivityIndicator size="small" />
      </View>
    ),
<<<<<<< HEAD
    captureAudio: false,
=======
    captureAudio: true,
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    useCamera2Api: false,
    playSoundOnCapture: false,
    pictureSize: 'None',
    videoStabilizationMode: 0,
    mirrorVideo: false,
  };

  _cameraRef: ?Object;
  _cameraHandle: ?number;
  _lastEvents: { [string]: string };
  _lastEventsTimes: { [string]: Date };
<<<<<<< HEAD
=======
  _isMounted: boolean;
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349

  constructor(props: PropsType) {
    super(props);
    this._lastEvents = {};
    this._lastEventsTimes = {};
<<<<<<< HEAD
    this.state = {
      isAuthorized: false,
      isAuthorizationChecked: false,
=======
    this._isMounted = true;
    this.state = {
      isAuthorized: false,
      isAuthorizationChecked: false,
      recordAudioPermissionStatus: RecordAudioPermissionStatusEnum.PENDING_AUTHORIZATION,
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    };
  }

  async takePictureAsync(options?: PictureOptions) {
    if (!options) {
      options = {};
    }
    if (!options.quality) {
      options.quality = 1;
    }
<<<<<<< HEAD
    if (options.orientation) {
      options.orientation = CameraManager.Orientation[options.orientation];
    }
=======

    if (options.orientation) {
      if (typeof options.orientation !== 'number') {
        const { orientation } = options;
        options.orientation = CameraManager.Orientation[orientation];
        if (__DEV__) {
          if (typeof options.orientation !== 'number') {
            // eslint-disable-next-line no-console
            console.warn(`Orientation '${orientation}' is invalid.`);
          }
        }
      }
    }

    if (options.pauseAfterCapture === undefined) {
      options.pauseAfterCapture = false;
    }

>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    return await CameraManager.takePicture(options, this._cameraHandle);
  }

  async getSupportedRatiosAsync() {
    if (Platform.OS === 'android') {
      return await CameraManager.getSupportedRatios(this._cameraHandle);
    } else {
      throw new Error('Ratio is not supported on iOS');
    }
  }

<<<<<<< HEAD
  async getMinAndMaxExposureCompensationAsync() {
    return await CameraManager.getMinAndMaxExposureCompensation(this._cameraHandle);
  }

  async hasCamera2Api() {
    return await CameraManager.hasCamera2Api(this._cameraHandle);
  }

  async getSdkVersion() {
    return await CameraManager.getSdkVersion(this._cameraHandle);
  }

  getAvailablePictureSizes = async (): string[] => {
=======
  getAvailablePictureSizes = async (): string[] => {
    //$FlowFixMe
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    return await CameraManager.getAvailablePictureSizes(this.props.ratio, this._cameraHandle);
  };

  async recordAsync(options?: RecordingOptions) {
    if (!options || typeof options !== 'object') {
      options = {};
    } else if (typeof options.quality === 'string') {
      options.quality = Camera.Constants.VideoQuality[options.quality];
    }
<<<<<<< HEAD
=======
    if (options.orientation) {
      if (typeof options.orientation !== 'number') {
        const { orientation } = options;
        options.orientation = CameraManager.Orientation[orientation];
        if (__DEV__) {
          if (typeof options.orientation !== 'number') {
            // eslint-disable-next-line no-console
            console.warn(`Orientation '${orientation}' is invalid.`);
          }
        }
      }
    }

    if (__DEV__) {
      if (options.videoBitrate && typeof options.videoBitrate !== 'number') {
        // eslint-disable-next-line no-console
        console.warn('Video Bitrate should be a positive integer');
      }
    }

    const { recordAudioPermissionStatus } = this.state;
    const { captureAudio } = this.props;

    if (
      !captureAudio ||
      recordAudioPermissionStatus !== RecordAudioPermissionStatusEnum.AUTHORIZED
    ) {
      options.mute = true;
    }

    if (__DEV__) {
      if (
        (!options.mute || captureAudio) &&
        recordAudioPermissionStatus !== RecordAudioPermissionStatusEnum.AUTHORIZED
      ) {
        // eslint-disable-next-line no-console
        console.warn('Recording with audio not possible. Permissions are missing.');
      }
    }

>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    return await CameraManager.record(options, this._cameraHandle);
  }

  stopRecording() {
    CameraManager.stopRecording(this._cameraHandle);
  }

  pausePreview() {
    CameraManager.pausePreview(this._cameraHandle);
  }

<<<<<<< HEAD
=======
  isRecording() {
    return CameraManager.isRecording(this._cameraHandle);
  }

>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
  resumePreview() {
    CameraManager.resumePreview(this._cameraHandle);
  }

  _onMountError = ({ nativeEvent }: EventCallbackArgumentsType) => {
    if (this.props.onMountError) {
      this.props.onMountError(nativeEvent);
    }
  };

  _onCameraReady = () => {
    if (this.props.onCameraReady) {
      this.props.onCameraReady();
    }
  };

<<<<<<< HEAD
  _onPictureSaved = ({ nativeEvent }) => {
=======
  _onPictureSaved = ({ nativeEvent }: EventCallbackArgumentsType) => {
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    if (this.props.onPictureSaved) {
      this.props.onPictureSaved(nativeEvent);
    }
  };

  _onObjectDetected = (callback: ?Function) => ({ nativeEvent }: EventCallbackArgumentsType) => {
    const { type } = nativeEvent;

    if (
      this._lastEvents[type] &&
      this._lastEventsTimes[type] &&
      JSON.stringify(nativeEvent) === this._lastEvents[type] &&
      new Date() - this._lastEventsTimes[type] < EventThrottleMs
    ) {
      return;
    }

    if (callback) {
      callback(nativeEvent);
      this._lastEventsTimes[type] = new Date();
      this._lastEvents[type] = JSON.stringify(nativeEvent);
    }
  };

  _setReference = (ref: ?Object) => {
    if (ref) {
      this._cameraRef = ref;
      this._cameraHandle = findNodeHandle(ref);
    } else {
      this._cameraRef = null;
      this._cameraHandle = null;
    }
  };

<<<<<<< HEAD
  async componentWillMount() {
    const hasVideoAndAudio = this.props.captureAudio;
    const isAuthorized = await requestPermissions(
      hasVideoAndAudio,
=======
  componentWillUnmount() {
    this._isMounted = false;
  }

  async componentDidMount() {
    const { hasCameraPermissions, hasRecordAudioPermissions } = await requestPermissions(
      this.props.captureAudio,
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
      CameraManager,
      this.props.permissionDialogTitle,
      this.props.permissionDialogMessage,
    );
<<<<<<< HEAD
    this.setState({ isAuthorized, isAuthorizationChecked: true });
=======
    if (this._isMounted === false) {
      return;
    }

    const recordAudioPermissionStatus = hasRecordAudioPermissions
      ? RecordAudioPermissionStatusEnum.AUTHORIZED
      : RecordAudioPermissionStatusEnum.NOT_AUTHORIZED;

    this.setState({
      isAuthorized: hasCameraPermissions,
      isAuthorizationChecked: true,
      recordAudioPermissionStatus,
    });
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
  }

  getStatus = (): Status => {
    const { isAuthorized, isAuthorizationChecked } = this.state;
    if (isAuthorizationChecked === false) {
      return CameraStatus.PENDING_AUTHORIZATION;
    }
    return isAuthorized ? CameraStatus.READY : CameraStatus.NOT_AUTHORIZED;
  };

  // FaCC = Function as Child Component;
  hasFaCC = (): * => typeof this.props.children === 'function';

  renderChildren = (): * => {
    if (this.hasFaCC()) {
<<<<<<< HEAD
      return this.props.children({ camera: this, status: this.getStatus() });
=======
      return this.props.children({
        camera: this,
        status: this.getStatus(),
        recordAudioPermissionStatus: this.state.recordAudioPermissionStatus,
      });
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    }
    return this.props.children;
  };

  render() {
<<<<<<< HEAD
    const nativeProps = this._convertNativeProps(this.props);

    if (this.state.isAuthorized || this.hasFaCC()) {
      return (
        <RNCamera
          {...nativeProps}
          ref={this._setReference}
          onMountError={this._onMountError}
          onCameraReady={this._onCameraReady}
          onGoogleVisionBarcodesDetected={this._onObjectDetected(
            this.props.onGoogleVisionBarcodesDetected,
          )}
          onBarCodeRead={this._onObjectDetected(this.props.onBarCodeRead)}
          onFacesDetected={this._onObjectDetected(this.props.onFacesDetected)}
          onTextRecognized={this._onObjectDetected(this.props.onTextRecognized)}
          onPictureSaved={this._onPictureSaved}
        >
          {this.renderChildren()}
        </RNCamera>
=======
    const { style, ...nativeProps } = this._convertNativeProps(this.props);

    if (this.state.isAuthorized || this.hasFaCC()) {
      return (
        <View style={style}>
          <RNCamera
            {...nativeProps}
            style={StyleSheet.absoluteFill}
            ref={this._setReference}
            onMountError={this._onMountError}
            onCameraReady={this._onCameraReady}
            onGoogleVisionBarcodesDetected={this._onObjectDetected(
              this.props.onGoogleVisionBarcodesDetected,
            )}
            onBarCodeRead={this._onObjectDetected(this.props.onBarCodeRead)}
            onFacesDetected={this._onObjectDetected(this.props.onFacesDetected)}
            onTextRecognized={this._onObjectDetected(this.props.onTextRecognized)}
            onPictureSaved={this._onPictureSaved}
          />
          {this.renderChildren()}
        </View>
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
      );
    } else if (!this.state.isAuthorizationChecked) {
      return this.props.pendingAuthorizationView;
    } else {
      return this.props.notAuthorizedView;
    }
  }

<<<<<<< HEAD
  _convertNativeProps(props: PropsType) {
=======
  _convertNativeProps({ children, ...props }: PropsType) {
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    const newProps = mapValues(props, this._convertProp);

    if (props.onBarCodeRead) {
      newProps.barCodeScannerEnabled = true;
    }

    if (props.onGoogleVisionBarcodesDetected) {
      newProps.googleVisionBarcodeDetectorEnabled = true;
    }

    if (props.onFacesDetected) {
      newProps.faceDetectorEnabled = true;
    }

    if (props.onTextRecognized) {
      newProps.textRecognizerEnabled = true;
    }

    if (Platform.OS === 'ios') {
      delete newProps.googleVisionBarcodeType;
<<<<<<< HEAD
      delete newProps.googleVisionBarcodeDetectorEnabled;
      delete newProps.ratio;
      delete newProps.textRecognizerEnabled;
=======
      delete newProps.googleVisionBarcodeMode;
      delete newProps.googleVisionBarcodeDetectorEnabled;
      delete newProps.ratio;
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    }

    return newProps;
  }

<<<<<<< HEAD
  getSupportedISOValues() {
    if (Platform.OS === 'android') {
      const props = this._convertNativeProps(this.props);
      return CameraManager.getSupportedISOValues({
        type: props.type
      });
    }

    //On iOS we can use any value in a range.
    return Promise.resolve([]);
  }

  getSupportedISORange() {
    if (Platform.OS === 'android') {
      const props = this._convertNativeProps(this.props);
      return CameraManager.getSupportedISORange({
        type: props.type
      });
    }

    return CameraManager.getSupportedISORange();
  }

  async getSupportedExposureCompensationRange() {
    if (Platform.OS === 'android') {
      const props = this._convertNativeProps(this.props);
      return await CameraManager.getSupportedExposureCompensationRange({
        type: props.type
      });
    }
    return CameraManager.getSupportedExposureCompensationRange();
  }

  getSupportedExposureDurationRange() {
    if (Platform.OS === 'ios') {
      return CameraManager.getSupportedExposureDurationRange();
    }

    //Not supported on Android:
    return Promise.resolve({});
  }

=======
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
  _convertProp(value: *, key: string): * {
    if (typeof value === 'string' && Camera.ConversionTables[key]) {
      return Camera.ConversionTables[key][value];
    }

    return value;
  }
}

export const Constants = Camera.Constants;

const RNCamera = requireNativeComponent('RNCamera', Camera, {
  nativeOnly: {
    accessibilityComponentType: true,
    accessibilityLabel: true,
    accessibilityLiveRegion: true,
    barCodeScannerEnabled: true,
    googleVisionBarcodeDetectorEnabled: true,
    faceDetectorEnabled: true,
    textRecognizerEnabled: true,
    importantForAccessibility: true,
    onBarCodeRead: true,
    onGoogleVisionBarcodesDetected: true,
    onCameraReady: true,
    onPictureSaved: true,
    onFaceDetected: true,
    onLayout: true,
    onMountError: true,
    renderToHardwareTextureAndroid: true,
    testID: true,
  },
<<<<<<< HEAD
});
=======
});
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
