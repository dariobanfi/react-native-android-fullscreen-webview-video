package com.airship.customwebview;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.facebook.react.ReactRootView;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.ThemedReactContext;

import android.view.ViewGroup.LayoutParams;

/**
 * Provides support for full-screen video on Android
 */
public class VideoWebChromeClient extends WebChromeClient {

  private final FrameLayout.LayoutParams FULLSCREEN_LAYOUT_PARAMS = new FrameLayout.LayoutParams(
      LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER);

  private WebChromeClient.CustomViewCallback mCustomViewCallback;

  private Activity mActivity;
  private View mWebView;
  private View mVideoView;
  private Boolean isVideoFullscreen;
  public Integer nbFois;
  private ViewGroup.LayoutParams paramsNotFullscreen;
  private ThemedReactContext mReactContext;

  public VideoWebChromeClient(Activity activity, WebView webView, ThemedReactContext reactContext) {
    mWebView = webView;
    mActivity = activity;
    isVideoFullscreen = false;
    nbFois = 0;
    mReactContext = reactContext;
  }

  @Override
  public void onShowCustomView(View view, CustomViewCallback callback) {
    if (mVideoView != null) {
      callback.onCustomViewHidden();
      return;
    }
    WritableMap params = Arguments.createMap();

    sendEvent(mReactContext, "VideoWillEnterFullScreen", params);
    // Store the view and it's callback for later, so we can dispose of them
    // correctly
    mVideoView = view;
    mCustomViewCallback = callback;

    view.setBackgroundColor(Color.BLACK);
    // Using SENSOR_LANDSCAPE to allow both landscape orientations
    mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

    // Entering fullscreen immersive mode
    mActivity.getWindow().getDecorView()
        .setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);

    // Setting BG Color to black
    view.setBackgroundColor(Color.BLACK);
    getRootView().addView(mVideoView, FULLSCREEN_LAYOUT_PARAMS);

    isVideoFullscreen = true;
  }

  @Override
  public void onHideCustomView() {
    if (mVideoView == null) {
      return;
    }
    mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    ((View) mWebView.getRootView()).setVisibility(View.VISIBLE);
    mVideoView.setVisibility(View.GONE);

    // Remove the custom view from its container.
    getRootView().removeView(mVideoView);
    // Pause HTML Videos when leaving WebView
    ((WebView) mWebView).loadUrl("javascript:document.getElementsByTagName('video')[0].pause()");

    // Exiting immersive mode
    mActivity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

    mVideoView = null;
    mCustomViewCallback.onCustomViewHidden();
    isVideoFullscreen = false;
    WritableMap params = Arguments.createMap();

    sendEvent(mReactContext, "VideoNotFullScreenAnymore", params);
  }

  private void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
  }

  private static ReactRootView findRootView(View parent) {
    if (parent == null) {
      return null;
    }
    if (parent instanceof ReactRootView) {
      return (ReactRootView) parent;
    }
    return findRootView((ReactRootView) parent.getParent());
  }

  private ViewGroup getRootView() {
    return ((ViewGroup) mActivity.findViewById(android.R.id.content));
  }

  /**
   * Notifies the class that the back key has been pressed by the user. This must
   * be called from the Activity's onBackPressed(), and if it returns false, the
   * activity itself should handle it. Otherwise don't do anything.
   *
   * @return Returns true if the event was handled, and false if was not (video
   *         view is not visible)
   */
  public boolean onBackPressed() {
    if (isVideoFullscreen) {
      onHideCustomView();
      return true;
    } else {
      return false;
    }
  }
}
