<<<<<<< HEAD
import Camera from './Camera';
import RNCamera from './RNCamera';
import FaceDetector from './FaceDetector';

=======
// @flow

import Camera from './Camera';
import RNCamera, { type Status as _CameraStatus } from './RNCamera';
import FaceDetector from './FaceDetector';

export type CameraStatus = _CameraStatus;
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
export { RNCamera, FaceDetector };

export default Camera;
