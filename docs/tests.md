<<<<<<< HEAD

# Testing

## Jest
To test a component which use RNCamera, you need to create a react-native-camera.js file inside your __mocks__ folder on the root of your project with the following content:
```javascript
import React from 'react'


const timeout = ms => new Promise(resolve => setTimeout(resolve, ms))
=======
# Testing

## Jest

To test a component which use RNCamera, you need to create a react-native-camera.js file inside your **mocks** folder on the root of your project with the following content:

```javascript
import React from 'react';

const timeout = ms => new Promise(resolve => setTimeout(resolve, ms));
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349

export class RNCamera extends React.Component {
  static Constants = {
    Aspect: {},
    BarCodeType: {},
    Type: { back: 'back', front: 'front' },
    CaptureMode: {},
    CaptureTarget: {},
    CaptureQuality: {},
    Orientation: {},
    FlashMode: {},
    TorchMode: {},
<<<<<<< HEAD
  }

  takePictureAsync = async () => {
    await timeout(2000)
    return {
      base64: 'base64string',
    }
  }

  render() {
    return null
  }
}

export default RNCamera
```

You don't need to do anything else in your test, because Jest will use the mock in your test instead of the native module. 

### Example
We are going to create a component which uses RNCamera which two simple features:
=======
  };

  takePictureAsync = async () => {
    await timeout(2000);
    return {
      base64: 'base64string',
    };
  };

  render() {
    return null;
  }
}

export default RNCamera;
```

You don't need to do anything else in your test, because Jest will use the mock in your test instead of the native module.

### Example

We are going to create a component which uses RNCamera which two simple features:

>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
- Take a photo
- Change camera between front or back

The custom component PhotoCamera is the following:

```javascript
<<<<<<< HEAD
import React from 'react'
import { View, TouchableOpacity, StyleSheet, Dimensions } from 'react-native'
import { RNCamera } from 'react-native-camera'
import Icon from 'react-native-vector-icons/FontAwesome'

=======
import React from 'react';
import { View, TouchableOpacity, StyleSheet, Dimensions } from 'react-native';
import { RNCamera } from 'react-native-camera';
import Icon from 'react-native-vector-icons/FontAwesome';
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349

const styles = StyleSheet.create({
  container: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    backgroundColor: 'black',
  },
  preview: {
    flex: 1,
    justifyContent: 'flex-end',
    alignItems: 'center',
  },
  topButtons: {
    flex: 1,
    width: Dimensions.get('window').width,
    alignItems: 'flex-start',
  },
  bottomButtons: {
    flex: 1,
    width: Dimensions.get('window').width,
    justifyContent: 'flex-end',
    alignItems: 'center',
  },

  flipButton: {
    flex: 1,
    marginTop: 20,
    right: 20,
    alignSelf: 'flex-end',
  },
  recordingButton: {
    marginBottom: 10,
  },
<<<<<<< HEAD
})
=======
});
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349

class PhotoCamera extends React.PureComponent {
  state = {
    type: RNCamera.Constants.Type.back,
<<<<<<< HEAD
  }

  flipCamera = () =>
    this.setState({ type: this.state.type === RNCamera.Constants.Type.back ? RNCamera.Constants.Type.front : RNCamera.Constants.Type.back })

  takePhoto = async () => {
    const { onTakePhoto } = this.props
=======
  };

  flipCamera = () =>
    this.setState({
      type:
        this.state.type === RNCamera.Constants.Type.back
          ? RNCamera.Constants.Type.front
          : RNCamera.Constants.Type.back,
    });

  takePhoto = async () => {
    const { onTakePhoto } = this.props;
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    const options = {
      quality: 0.5,
      base64: true,
      width: 300,
      height: 300,
<<<<<<< HEAD
    }
    const data = await this.camera.takePictureAsync(options)
    onTakePhoto(data.base64)
  }
  render() {
    const { type } = this.state
    return (
      <View style={styles.container}>
        <RNCamera
          ref={(cam) => {
            this.camera = cam
=======
    };
    const data = await this.camera.takePictureAsync(options);
    onTakePhoto(data.base64);
  };
  render() {
    const { type } = this.state;
    return (
      <View style={styles.container}>
        <RNCamera
          ref={cam => {
            this.camera = cam;
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
          }}
          type={type}
          style={styles.preview}
        />
        <View style={styles.topButtons}>
          <TouchableOpacity onPress={this.flipCamera} style={styles.flipButton}>
            <Icon name="refresh" size={35} color="orange" />
          </TouchableOpacity>
        </View>
        <View style={styles.bottomButtons}>
          <TouchableOpacity onPress={this.takePhoto} style={styles.recordingButton}>
            <Icon name="camera" size={50} color="orange" />
          </TouchableOpacity>
        </View>
      </View>
<<<<<<< HEAD
    )
  }
}

export default PhotoCamera

=======
    );
  }
}

export default PhotoCamera;
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
```

And here is our test to check if it renders properly:

```javascript
<<<<<<< HEAD
import React from 'react'
import Adapter from 'enzyme-adapter-react-16'
import { shallow, configure } from 'enzyme'
import MyPhotoCamera from './'


configure({ adapter: new Adapter() })

describe('PhotoCamera Tests', () => {
  test('renders correctly', () => {
    const wrapper = shallow(<MyPhotoCamera />)
    expect(wrapper).toMatchSnapshot()
  })
})
=======
import React from 'react';
import Adapter from 'enzyme-adapter-react-16';
import { shallow, configure } from 'enzyme';
import MyPhotoCamera from './';

configure({ adapter: new Adapter() });

describe('PhotoCamera Tests', () => {
  test('renders correctly', () => {
    const wrapper = shallow(<MyPhotoCamera />);
    expect(wrapper).toMatchSnapshot();
  });
});
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
```

Also, here is the complete test of the whole component with 100% coverage:

```javascript
<<<<<<< HEAD
import React from 'react'
import { TouchableOpacity } from 'react-native'
import Adapter from 'enzyme-adapter-react-16'
import { shallow, configure, mount } from 'enzyme'
import PhotoCamera from '../'


const { JSDOM } = require('jsdom')


const jsdom = new JSDOM()
const { window } = jsdom
=======
import React from 'react';
import { TouchableOpacity } from 'react-native';
import Adapter from 'enzyme-adapter-react-16';
import { shallow, configure, mount } from 'enzyme';
import PhotoCamera from '../';

const { JSDOM } = require('jsdom');

const jsdom = new JSDOM();
const { window } = jsdom;
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349

function copyProps(src, target) {
  const props = Object.getOwnPropertyNames(src)
    .filter(prop => typeof target[prop] === 'undefined')
<<<<<<< HEAD
    .map(prop => Object.getOwnPropertyDescriptor(src, prop))
  Object.defineProperties(target, props)
}

global.window = window
global.document = window.document
global.navigator = {
  userAgent: 'node.js',
}
copyProps(window, global)

// Ignore React Web errors when using React Native
// but still show relevant errors
const suppressedErrors = /(React does not recognize the.*prop on a DOM element|Unknown event handler property|is using uppercase HTML|Received `true` for a non-boolean attribute `accessible`|The tag.*is unrecognized in this browser)|is using incorrect casing|Received `true` for a non-boolean attribute `enabled`/
const realConsoleError = console.error // eslint-disable-line
// eslint-disable-next-line
console.error = message => {
  if (message.match(suppressedErrors)) {
    return
  }
  realConsoleError(message)
}

configure({ adapter: new Adapter() })

describe('PhotoCamera Tests', () => {
  test('renders correctly', () => {
    const wrapper = shallow(<PhotoCamera />)
    expect(wrapper).toMatchSnapshot()
  })
  test('initial state should be back camera', () => {
    const wrapper = shallow(<PhotoCamera />)
    expect(wrapper.state().type).toBe('back')
  })
  test('should flip the camera from back to front', () => {
    const wrapper = shallow(<PhotoCamera />)
    expect(wrapper.state().type).toBe('back')
=======
    .map(prop => Object.getOwnPropertyDescriptor(src, prop));
  Object.defineProperties(target, props);
}

global.window = window;
global.document = window.document;
global.navigator = {
  userAgent: 'node.js',
};
copyProps(window, global);

// Ignore React Web errors when using React Native
// but still show relevant errors
const suppressedErrors = /(React does not recognize the.*prop on a DOM element|Unknown event handler property|is using uppercase HTML|Received `true` for a non-boolean attribute `accessible`|The tag.*is unrecognized in this browser)|is using incorrect casing|Received `true` for a non-boolean attribute `enabled`/;
const realConsoleError = console.error; // eslint-disable-line
// eslint-disable-next-line
console.error = message => {
  if (message.match(suppressedErrors)) {
    return;
  }
  realConsoleError(message);
};

configure({ adapter: new Adapter() });

describe('PhotoCamera Tests', () => {
  test('renders correctly', () => {
    const wrapper = shallow(<PhotoCamera />);
    expect(wrapper).toMatchSnapshot();
  });
  test('initial state should be back camera', () => {
    const wrapper = shallow(<PhotoCamera />);
    expect(wrapper.state().type).toBe('back');
  });
  test('should flip the camera from back to front', () => {
    const wrapper = shallow(<PhotoCamera />);
    expect(wrapper.state().type).toBe('back');
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    wrapper
      .find(TouchableOpacity)
      .first()
      .props()
<<<<<<< HEAD
      .onPress()
    expect(wrapper.state().type).toBe('front')
  })

  test('should flip the camera from front to back if touch flip button and curren state is ', () => {
    const wrapper = shallow(<PhotoCamera />)
    wrapper.setState({
      type: 'front',
    })
    wrapper.update()
=======
      .onPress();
    expect(wrapper.state().type).toBe('front');
  });

  test('should flip the camera from front to back if touch flip button and curren state is ', () => {
    const wrapper = shallow(<PhotoCamera />);
    wrapper.setState({
      type: 'front',
    });
    wrapper.update();
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    wrapper
      .find(TouchableOpacity)
      .first()
      .props()
<<<<<<< HEAD
      .onPress()
    expect(wrapper.state().type).toBe('back')
  })

  test('should have a reference to the React Native Camera module', () => {
    const wrapper = mount(<PhotoCamera />)
    expect(wrapper.instance().camera).toBeDefined()
  })

  test('test onPress functionality', async () => {
    const onTakePhotoEvent = jest.fn(data => data)
    const wrapper = mount(<PhotoCamera onTakePhoto={onTakePhotoEvent} />)
=======
      .onPress();
    expect(wrapper.state().type).toBe('back');
  });

  test('should have a reference to the React Native Camera module', () => {
    const wrapper = mount(<PhotoCamera />);
    expect(wrapper.instance().camera).toBeDefined();
  });

  test('test onPress functionality', async () => {
    const onTakePhotoEvent = jest.fn(data => data);
    const wrapper = mount(<PhotoCamera onTakePhoto={onTakePhotoEvent} />);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    await wrapper
      .find(TouchableOpacity)
      .at(1)
      .props()
<<<<<<< HEAD
      .onPress()
    expect(onTakePhotoEvent.mock.calls.length).toBe(1)
  })
})

```
=======
      .onPress();
    expect(onTakePhotoEvent.mock.calls.length).toBe(1);
  });
});
```
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
