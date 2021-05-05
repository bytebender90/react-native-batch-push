# React Native Batch

The official React Native plugin for the Batch SDK. Made with ❤️ by BAM and Batch.

<hr>

## [Link to full documentation](https://bamlab.github.io/react-native-batch-push)

## [Development setup](readme/development.md)

<hr>

## Installation

### 1. Install the React Native Batch plugin

- Install using `yarn add @bam.tech/react-native-batch`
- Or `npm i @bam.tech/react-native-batch`

### 2. Setup iOS dependencies

#### Cocoapods (Recommended)

- Go to `/ios`
- If you don't have a Podfile yet run `pod init`
- Add `pod 'Batch', '~>1.17'` to your _Podfile_
- Run `pod install`

#### Manual frameworks (Not Recommended)

If you don't use CocoaPods, you can integrate Batch SDK manually.

- Download the [SDK](https://batch.com/doc/ios/advanced/general.html#_manual-sdk-integration)
- Unzip the SDK
- Here instead of following the readme inside the Batch.embeddedframework folder you downloaded follow the below steps.
- Create a Batch folder in `{your-project}/ios/Batch`
- Copy your Batch framework inside `{your-project}/ios/Batch`
- Open your project in XCode. Right click your ".xcodeproj" and click "Add Files to {yourProjectName}…". Find the "Batch" folder you created and select it. Before clicking "Add", to the left you'll see an "Options" button. Click it, and make sure "Create Groups" and "Add to targets" for your project are both selected.

### 3. Link the plugin

| react-native version | link the plugin                                                                     |
| -------------------- | ----------------------------------------------------------------------------------- |
| `>= 0.60.0`          | - auto-linking is supported                                                         |
| `< 0.60.0`           | - From the root folder <br/> - Run `react-native link @bam.tech/react-native-batch` |

### 4. Extra steps on Android

#### a. Install Batch dependencies

```groovy
// android/build.gradle

buildscript {
    ...
    dependencies {
        ...
        classpath 'com.google.gms:google-services:4.3.4'
    }
}
```

```groovy
// android/app/build.gradle

dependencies {
    implementation platform('com.google.firebase:firebase-bom:25.12.0')
    implementation "com.google.firebase:firebase-messaging"
    ...
}

apply plugin: 'com.google.gms.google-services'
```

#### b. Configure auto-linking

Create or adapt the `react-native.config.js` file at the root of your project:

```js
// react-native.config.js

module.exports = {
  dependencies: {
    '@bam.tech/react-native-batch': {
      platforms: {
        android: {
          packageInstance: 'new RNBatchPackage(this.getApplication())',
        },
      },
    },
  },
};
```

#### c. Add your Batch key

```groovy
// android/app/build.gradle

defaultConfig {
    ...
    resValue "string", "BATCH_API_KEY", "%YOUR_BATCH_API_KEY%"
}
```

#### d. Add your Firebase config

- Add the _google-services.json_ file to `/android/app`

### 5. Extra steps on iOS

#### a. Enable Push Capabilities

- In the project window
- Go to _Capabilities_
- Toggle _Push Notifications_

#### b. Configure your Batch key

Go to the Batch dashboard, create an iOS app and upload your iOS push certificate.

Then, in `Info.plist`, provide:

```xml
<key>BatchAPIKey</key>
<string>%YOUR_BATCH_API_KEY%</string>
```

#### c. Start Batch in AppDelegate.m

In `AppDelegate.m`, start Batch:

```objective-c
#import "RNBatch.h"

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    ...
    [RNBatch start:false]; // or true if you want the do not disturb mode
    ...
    return YES;
}
```

#### d. Setup your UNUserNotificationCenterDelegate

* If you use Firebase or another framework swizzling your AppDelegate, follow the [manual integration](https://doc.batch.com/ios/advanced/manual-integration) guide
* Otherwise, add `[BatchUNUserNotificationCenterDelegate registerAsDelegate];` after the `[RNBatch start:false];` call in your `AppDelegate.m`

If you want to show foreground notifications, add the [relevant configuration](https://doc.batch.com/ios/advanced/customizing-notifications#showing-foreground-notifications-ios-10-only): `[BatchUNUserNotificationCenterDelegate sharedInstance].showForegroundNotifications = true;` after registering the delegate .

<hr>

## Usage

### Start Batch

```js
import { Batch } from '@bam.tech/react-native-batch';

Batch.start();
```

### Enabling push notifications on iOS

```js
import { BatchPush } from '@bam.tech/react-native-batch';

BatchPush.registerForRemoteNotifications();
```

<hr>

## Other

### Small push notification icon

For better results on Android 5.0 and higher, it is recommended to add a Small Icon and Notification Color.
An icon can be generated using Android Studio's asset generator: as it will be tinted and masked by the system, only the alpha channel matters and will define the shape displayed. It should be of 24x24dp size.
If your notifications shows up in the system statusbar in a white shape, this is what you need to configure.

This can be configured in the manifest as metadata in the application tag:

```xml
<!-- Assuming there is a push_icon.png in your res/drawable-{dpi} folder -->

<manifest ...>
    <application ...>
        <meta-data
            android:name="com.batch.android.push.smallicon"
            android:resource="@drawable/push_icon" />

        <!-- Notification color. ARGB but the alpha value can only be FF -->
        <meta-data
            android:name="com.batch.android.push.color"
            android:value="#FF00FF00" />
```

### Mobile landings and in-app messaging

If you set a custom `launchMode` in your `AndroidManifest.xml`, add in your `MainActivity.java`:

```java
// import android.content.Intent;
// import com.batch.android.Batch;

@Override
public void onNewIntent(Intent intent)
{
    Batch.onNewIntent(this, intent);

    super.onNewIntent(intent);
}
```

### GDPR compliance flow

#### 1. Opt out by default

##### Android

Add the following metadata in your `AndroidManifest.xml` as described in the [Android SDK documentation](https://doc.batch.com/android/advanced/opt-out#disabling-the-sdk-by-default):

```xml
<meta-data android:name="batch_opted_out_by_default" android:value="true" />
```

##### iOS

Add `BATCH_OPTED_OUT_BY_DEFAULT=true` in your `Info.plist` as described in the [iOS SDK documentation](https://doc.batch.com/ios/advanced/opt-out#disabling-the-sdk-by-default):

```xml
<key>BATCH_OPTED_OUT_BY_DEFAULT</key>
<true/>
```

#### 2. Opt in when the user agrees

```js
import { Batch } from '@bam.tech/react-native-batch';

Batch.optIn();
```

#### 3. Delete the user's data when the user wants to delete its account

As per the [SDK documentation](https://doc.batch.com/android/advanced/opt-out#opting-out) :
> This will wipe the data locally and request a remote data removal for the matching advertising ID/Custom User ID. Batch will blacklist the advertising and the Custom User ID for one month following the data removal. Batch will also discard the data sent from the Custom Data API for that specific Custom User ID. The Inbox feature will no longer work if you were relying on the Custom User ID.

```js
import { Batch } from '@bam.tech/react-native-batch';

Batch.optOutAndWipeData();
```

<hr>

## Troubleshooting

- :warning: You will need a physical device to fully test **push notifications**
