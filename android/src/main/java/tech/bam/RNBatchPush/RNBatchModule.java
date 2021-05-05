package tech.bam.RNBatchPush;

import android.app.Activity;
import android.app.Application;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.location.Location;

import androidx.annotation.Nullable;

import android.util.Log;

import com.batch.android.Batch;
import com.batch.android.BatchActivityLifecycleHelper;
import com.batch.android.PushNotificationType;
import com.batch.android.BatchInboxFetcher;
import com.batch.android.BatchInboxNotificationContent;
import com.batch.android.BatchMessage;
import com.batch.android.BatchUserDataEditor;
import com.batch.android.Config;
import com.batch.android.json.JSONObject;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;

import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RNBatchModule extends ReactContextBaseJavaModule {
    private static final String NAME = "RNBatch";
    private static final String PLUGIN_VERSION_ENVIRONMENT_VARIABLE = "batch.plugin.version";
    private static final String PLUGIN_VERSION = "ReactNative/6.0.0-rc.0";

    private final ReactApplicationContext reactContext;

    static {
        System.setProperty("batch.plugin.version", PLUGIN_VERSION);
    }

    // REACT NATIVE PLUGIN SETUP

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();

        // Add push notification types
        final Map<String, Object> notificationTypes = new HashMap<>();
        for (PushNotificationType type : PushNotificationType.values()) {
            notificationTypes.put(type.name(), type.ordinal());
        }
        constants.put("NOTIFICATION_TYPES", notificationTypes);

        return constants;
    }

    private static boolean isInitialized = false;

    public static void initialize(Application application) {
        if (!isInitialized) {
            Resources resources = application.getResources();
            String packageName = application.getPackageName();
            String batchAPIKey = resources.getString(resources.getIdentifier("BATCH_API_KEY", "string", packageName));
            Batch.setConfig(new Config(batchAPIKey));

            application.registerActivityLifecycleCallbacks(new BatchActivityLifecycleHelper());

            isInitialized = true;
        }
    }

    public RNBatchModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    // BASE MODULE

    @ReactMethod
    public void start(final boolean doNotDisturb) {
        Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }

        if (doNotDisturb) {
            Batch.Messaging.setDoNotDisturbEnabled(true);
        }

        Batch.onStart(activity);
    }

    public void start() {
        this.start(false);
    }

    @ReactMethod
    public void optIn() {
        Batch.optIn(reactContext);
        start();
    }

    @ReactMethod
    public void optOut() {
        Batch.optOut(reactContext);
    }

    @ReactMethod
    public void optOutAndWipeData() {
        Batch.optOutAndWipeData(reactContext);
    }

    // PUSH MODULE

    @ReactMethod
    public void push_registerForRemoteNotifications() { /* No effect on android */ }

    @ReactMethod
    public void push_setNotificationTypes(Integer notifType) {
        EnumSet<PushNotificationType> pushTypes = PushNotificationType.fromValue(notifType);
        Batch.Push.setNotificationsType(pushTypes);
    }

    @ReactMethod
    public void push_clearBadge() { /* No effect on android */ }

    @ReactMethod
    public void push_dismissNotifications() { /* No effect on android */ }

    @ReactMethod
    public void push_getLastKnownPushToken(Promise promise) {
        String pushToken = Batch.Push.getLastKnownPushToken();
        promise.resolve(pushToken);
    }

    // MESSAGING MODULE

    @ReactMethod
    public void messaging_showPendingMessage() {
        Boolean test = Batch.Messaging.isDoNotDisturbEnabled();
        BatchMessage msg = Batch.Messaging.popPendingMessage();
        if (msg != null) {
            Batch.Messaging.show(getCurrentActivity(), msg);
        }
    }

    @ReactMethod
    public void messaging_setNotDisturbed(final boolean active) {
        Batch.Messaging.setDoNotDisturbEnabled(active);
    }

    @ReactMethod
    public void messaging_setTypefaceOverride(@Nullable String normalTypefaceName, @Nullable String boldTypefaceName) {
        AssetManager assetManager = this.reactContext.getAssets();
        @Nullable Typeface normalTypeface = normalTypefaceName != null ? createTypeface(normalTypefaceName, Typeface.NORMAL, assetManager) : null;
        @Nullable Typeface boldTypeface = boldTypefaceName != null ? createTypeface(boldTypefaceName, Typeface.BOLD, assetManager) : null;
        @Nullable Typeface boldTypefaceFallback = boldTypefaceName != null ? createTypeface(boldTypefaceName, Typeface.NORMAL, assetManager) : null;

        Batch.Messaging.setTypefaceOverride(normalTypeface, boldTypeface != null ? boldTypeface : boldTypefaceFallback);
    }

    // from https://github.com/facebook/react-native/blob/dc80b2dcb52fadec6a573a9dd1824393f8c29fdc/ReactAndroid/src/main/java/com/facebook/react/views/text/ReactFontManager.java#L118
    // we need to know if the typeface is found so we cannot use it directly :(
    private static final String[] FONT_EXTENSIONS = {"", "_bold", "_italic", "_bold_italic"};
    private static final String[] FONT_FILE_EXTENSIONS = {".ttf", ".otf"};
    private static final String FONTS_ASSET_PATH = "fonts/";

    private static @Nullable
    Typeface createTypeface(
            String fontFamilyName, int style, AssetManager assetManager) {
        String extension = FONT_EXTENSIONS[style];
        for (String fileExtension : FONT_FILE_EXTENSIONS) {
            String fileName =
                    new StringBuilder()
                            .append(FONTS_ASSET_PATH)
                            .append(fontFamilyName)
                            .append(extension)
                            .append(fileExtension)
                            .toString();
            try {
                return Typeface.createFromAsset(assetManager, fileName);
            } catch (RuntimeException e) {
                // unfortunately Typeface.createFromAsset throws an exception instead of returning null
                // if the typeface doesn't exist
            }
        }

        return null;
    }


    // DEBUG MODULE

    @ReactMethod
    public void debug_startDebugActivity() {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            Batch.Debug.startDebugActivity(currentActivity);
        }
    }


    // INBOX MODULE

    private static final int NOTIFICATIONS_COUNT = 100;

    @ReactMethod
    public void inbox_fetchNotifications(final Promise promise) {
        BatchInboxFetcher fetcher = Batch.Inbox.getFetcher(getCurrentActivity());
        fetcher.setFetchLimit(NOTIFICATIONS_COUNT);
        fetcher.setMaxPageSize(NOTIFICATIONS_COUNT);

        fetcher.fetchNewNotifications(new BatchInboxFetcher.OnNewNotificationsFetchedListener() {
            @Override
            public void onFetchSuccess(List<BatchInboxNotificationContent> notifications,
                                       boolean foundNewNotifications,
                                       boolean endReached) {
                WritableArray results = RNBatchInbox.getSuccessResponse(notifications);
                promise.resolve(results);
            }

            @Override
            public void onFetchFailure(String error) {
                promise.reject("InboxFetchError", error);
            }
        });
    }


    @ReactMethod
    public void inbox_fetchNotificationsForUserIdentifier(String userIdentifier, String authenticationKey, final Promise promise) {
        BatchInboxFetcher fetcher = Batch.Inbox.getFetcher(getCurrentActivity(), userIdentifier, authenticationKey);
        fetcher.setFetchLimit(NOTIFICATIONS_COUNT);
        fetcher.setMaxPageSize(NOTIFICATIONS_COUNT);

        fetcher.fetchNewNotifications(new BatchInboxFetcher.OnNewNotificationsFetchedListener() {
            @Override
            public void onFetchSuccess(List<BatchInboxNotificationContent> notifications,
                                       boolean foundNewNotifications,
                                       boolean endReached) {
                promise.resolve(RNBatchInbox.getSuccessResponse(notifications));
            }

            @Override
            public void onFetchFailure(String error) {
                promise.reject("InboxFetchError", error);
            }
        });
    }

    // USER DATA EDITOR MODULE

    @ReactMethod
    public void userData_getInstallationId(Promise promise) {
        String userId = Batch.User.getInstallationID();
        promise.resolve(userId);
    }

    @ReactMethod
    public void userData_save(ReadableArray actions) {
        BatchUserDataEditor editor = Batch.User.editor();
        for (int i = 0; i < actions.size(); i++) {
            ReadableMap action = actions.getMap(i);
            String type = action.getString("type");

            if (type.equals("setAttribute")) {
                String key = action.getString("key");
                ReadableType valueType = action.getType("value");
                switch (valueType) {
                    case Null:
                        editor.removeAttribute(key);
                        break;
                    case Boolean:
                        editor.setAttribute(key, action.getBoolean("value"));
                        break;
                    case Number:
                        editor.setAttribute(key, action.getDouble("value"));
                        break;
                    case String:
                        editor.setAttribute(key, action.getString("value"));
                        break;
                }
            } else if (type.equals("setDateAttribute")) {
                String key = action.getString("key");
                long timestamp = (long) action.getDouble("value");
                Date date = new Date(timestamp);
                editor.setAttribute(key, date);
            } else if (type.equals("removeAttribute")) {
                String key = action.getString("key");
                editor.removeAttribute(key);
            } else if (type.equals("clearAttributes")) {
                editor.clearAttributes();
            } else if (type.equals("setIdentifier")) {
                ReadableType valueType = action.getType("value");
                if (valueType.equals(ReadableType.Null)) {
                    editor.setIdentifier(null);
                } else {
                    String value = action.getString("value");
                    editor.setIdentifier(value);
                }
            } else if (type.equals("setLanguage")) {
                ReadableType valueType = action.getType("value");
                if (valueType.equals(ReadableType.Null)) {
                    editor.setLanguage(null);
                } else {
                    String value = action.getString("value");
                    editor.setLanguage(value);
                }
            } else if (type.equals("setRegion")) {
                ReadableType valueType = action.getType("value");
                if (valueType.equals(ReadableType.Null)) {
                    editor.setRegion(null);
                } else {
                    String value = action.getString("value");
                    editor.setRegion(value);
                }
            } else if (type.equals("addTag")) {
                String collection = action.getString("collection");
                String tag = action.getString("tag");
                editor.addTag(collection, tag);
            } else if (type.equals("removeTag")) {
                String collection = action.getString("collection");
                String tag = action.getString("tag");
                editor.removeTag(collection, tag);
            } else if (type.equals("clearTagCollection")) {
                String collection = action.getString("collection");
                editor.clearTagCollection(collection);
            } else if (type.equals("clearTags")) {
                editor.clearTags();
            }
        }
        editor.save();
    }

    @ReactMethod
    public void userData_trackEvent(String name, String label, ReadableMap serializedEventData) {
        Batch.User.trackEvent(name, label, RNUtils.convertSerializedEventDataToEventData(serializedEventData));
    }

    @ReactMethod
    public void userData_trackTransaction(double amount, ReadableMap data) {
        Batch.User.trackTransaction(amount, new JSONObject(data.toHashMap()));
    }

    @ReactMethod
    public void userData_trackLocation(ReadableMap serializedLocation) {
        Location nativeLocation = new Location("tech.bam.RNBatchPush");
        nativeLocation.setLatitude(serializedLocation.getDouble("latitude"));
        nativeLocation.setLongitude(serializedLocation.getDouble("longitude"));

        if (serializedLocation.hasKey("precision")) {
            nativeLocation.setAccuracy((float) serializedLocation.getDouble("precision"));
        }

        if (serializedLocation.hasKey("date")) {
            nativeLocation.setTime((long) serializedLocation.getDouble("date"));
        }

        Batch.User.trackLocation(nativeLocation);
    }
}
