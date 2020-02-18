package com.rssignaturecapture;

import android.util.Log;
import android.view.ViewGroup;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.ThemedReactContext;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;

import android.util.Base64;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import java.lang.Boolean;

class Output {
  Bitmap bitmap;
  int xMin;
  int xMax;
  int yMin;
  int yMax;
  int width;
  int height;
  double fillRateAbsolute;
  double fillRateRelative;
  double fillAreaRate;
  public Output(Bitmap bitmap, int xMin, int xMax, int yMin, int yMax, int width, int height, double fillRateAbsolute, double fillRateRelative, double fillAreaRate) {
    this.bitmap = bitmap;
    this.xMin = xMin;
    this.xMax = xMax;
    this.yMin = yMin;
    this.yMax = yMax;
    this.width = width;
    this.height = height;
    this.fillRateAbsolute = fillRateAbsolute;
    this.fillRateRelative = fillRateRelative;
    this.fillAreaRate = fillAreaRate;
  }
  public Bitmap getBitmap() {
    return bitmap;
  }
  public int getXMin() {
    return xMin;
  }
  public int getXMax() {
    return xMax;
  }
  public int getYMin() {
    return yMin;
  }
  public int getYMax() {
    return yMax;
  }
  public int getWidth() {
    return width;
  }
  public int getHeight() {
    return height;
  }
  public double getFillRateAbsolute() {
    return fillRateAbsolute;
  }
  public double getFillRateRelative() {
    return fillRateRelative;
  }
  public double getFillAreaRate() {
    return fillAreaRate;
  }
}

public class RSSignatureCaptureMainView extends LinearLayout implements OnClickListener,RSSignatureCaptureView.SignatureCallback {
  LinearLayout buttonsLayout;
  RSSignatureCaptureView signatureView;

  Activity mActivity;
  int mOriginalOrientation;
  Boolean saveFileInExtStorage = false;
  String viewMode = "portrait";
  Boolean showBorder = true;
  Boolean showNativeButtons = true;
  Boolean showTitleLabel = true;
  int maxSize = 500;

  public RSSignatureCaptureMainView(Context context, Activity activity) {
    super(context);
    Log.d("React:", "RSSignatureCaptureMainView(Contructtor)");
    mOriginalOrientation = activity.getRequestedOrientation();
    mActivity = activity;

    this.setOrientation(LinearLayout.VERTICAL);
    this.signatureView = new RSSignatureCaptureView(context,this);
    // add the buttons and signature views
    this.buttonsLayout = this.buttonsLayout();
    this.addView(this.buttonsLayout);
    this.addView(signatureView);

    setLayoutParams(new android.view.ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));
  }

  public RSSignatureCaptureView getSignatureView() {
    return signatureView;
  }

  public void setSaveFileInExtStorage(Boolean saveFileInExtStorage) {
    this.saveFileInExtStorage = saveFileInExtStorage;
  }

  public void setViewMode(String viewMode) {
    this.viewMode = viewMode;

    if (viewMode.equalsIgnoreCase("portrait")) {
      mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    } else if (viewMode.equalsIgnoreCase("landscape")) {
      mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }
  }

  public void setShowNativeButtons(Boolean showNativeButtons) {
    this.showNativeButtons = showNativeButtons;
    if (showNativeButtons) {
      Log.d("Added Native Buttons", "Native Buttons:" + showNativeButtons);
      buttonsLayout.setVisibility(View.VISIBLE);
    } else {
      buttonsLayout.setVisibility(View.GONE);
    }
  }

  public void setMaxSize(int size) {
    this.maxSize = size;
  }


  private LinearLayout buttonsLayout() {

    // create the UI programatically
    LinearLayout linearLayout = new LinearLayout(this.getContext());
    Button saveBtn = new Button(this.getContext());
    Button clearBtn = new Button(this.getContext());

    // set orientation
    linearLayout.setOrientation(LinearLayout.HORIZONTAL);
    linearLayout.setBackgroundColor(Color.WHITE);

    // set texts, tags and OnClickListener
    saveBtn.setText("Save");
    saveBtn.setTag("Save");
    saveBtn.setOnClickListener(this);

    clearBtn.setText("Reset");
    clearBtn.setTag("Reset");
    clearBtn.setOnClickListener(this);

    linearLayout.addView(saveBtn);
    linearLayout.addView(clearBtn);

    // return the whoe layout
    return linearLayout;
  }

  // the on click listener of 'save' and 'clear' buttons
  @Override public void onClick(View v) {
    String tag = v.getTag().toString().trim();

    // save the signature
    if (tag.equalsIgnoreCase("save")) {
      this.saveImage();
    }

    // empty the canvas
    else if (tag.equalsIgnoreCase("Reset")) {
      this.signatureView.clearSignature();
    }
  }

  /**
   * save the signature to an sd card directory
   */
  final void saveImage() {

    String root = Environment.getExternalStorageDirectory().toString();

    // the directory where the signature will be saved
    File myDir = new File(root + "/saved_signature");

    // make the directory if it does not exist yet
    if (!myDir.exists()) {
      myDir.mkdirs();
    }

    // set the file name of your choice
    String fname = "signature.png";

    // in our case, we delete the previous file, you can remove this
    File file = new File(myDir, fname);
    if (file.exists()) {
      file.delete();
    }

    try {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      Bitmap signature = this.signatureView.getSignature();
      Output output = cropBitmapTransparency(signature);
      Bitmap bitmap = output.getBitmap();
      String encoded = null;
      if(bitmap != null) {
        bitmap = getResizedBitmap(bitmap);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        encoded = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT);
      }

      WritableMap event = Arguments.createMap();
      if(encoded == null) {
        event.putNull("data");
      } else {
        event.putString("data", encoded);
      }
      event.putInt("xMin", output.getXMin());
      event.putInt("xMax", output.getXMax());
      event.putInt("yMin", output.getYMin());
      event.putInt("yMax", output.getYMax());
      event.putInt("width", output.getWidth());
      event.putInt("height", output.getHeight());
      event.putDouble("fillRateAbsolute", output.getFillRateAbsolute());
      event.putDouble("fillRateRelative", output.getFillRateRelative());
      event.putDouble("fillAreaRate", output.getFillAreaRate());
      ReactContext reactContext = (ReactContext) getContext();
      reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), "topChange", event);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public Output cropBitmapTransparency(Bitmap sourceBitmap)
  {
    int minX = sourceBitmap.getWidth();
    int minY = sourceBitmap.getHeight();
    int maxX = -1;
    int maxY = -1;

    int imgWidth = sourceBitmap.getWidth();
    int imgHeight = sourceBitmap.getHeight();

    int filledPixel = 0;
    int filledWidth = 0;
    int filledHeight = 0;

    for(int y = 0; y < imgHeight; y++)
    {
      for(int x = 0; x < imgWidth; x++)
      {
        int alpha = (sourceBitmap.getPixel(x, y) >> 24) & 255;
        if(alpha > 0)   // pixel is not 100% transparent
        {
          filledPixel++;
          if(x < minX) {
            minX = x;
          }
          if(x > maxX) {
            maxX = x;
          }
          if(y < minY) {
            minY = y;
          }
          if(y > maxY) {
            maxY = y;
          }
        }
      }
    }
    if(filledPixel > 0) {
      filledWidth = maxX-minX+1;
      filledHeight = maxY-minY+1;
    }

    Bitmap bitmap;

    if((maxX < minX) || (maxY < minY)) {
      bitmap = null;
    } else {
      bitmap = Bitmap.createBitmap(sourceBitmap, minX, minY, (maxX - minX) + 1, (maxY - minY) + 1);
    }

    Output output = new Output(
            bitmap,
            minX,
            maxX,
            minY,
            maxY,
            filledWidth,
            filledHeight,
            (double) filledPixel / (imgWidth*imgHeight),
            filledPixel > 0 ? (double) filledPixel / (filledWidth*filledHeight) : 0,
            (double) (filledWidth*filledHeight) / (imgWidth*imgHeight)
    );

    return output;
  }


  public Bitmap getResizedBitmap(Bitmap image) {
    Log.d("React Signature","maxSize:"+maxSize);
    int width = image.getWidth();
    int height = image.getHeight();

    if(width <= maxSize) {
      return image;
    }

    float bitmapRatio = (float) width / (float) height;
    if (bitmapRatio > 1) {
      width = maxSize;
      height = (int) (width / bitmapRatio);
    } else {
      height = maxSize;
      width = (int) (height * bitmapRatio);
    }

    return Bitmap.createScaledBitmap(image, width, height, true);
  }


  public void reset() {
    if (this.signatureView != null) {
      this.signatureView.clearSignature();
    }
  }

  @Override public void onDragged() {
    WritableMap event = Arguments.createMap();
    event.putBoolean("dragged", true);
    ReactContext reactContext = (ReactContext) getContext();
    reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), "topChange", event);

  }
}
