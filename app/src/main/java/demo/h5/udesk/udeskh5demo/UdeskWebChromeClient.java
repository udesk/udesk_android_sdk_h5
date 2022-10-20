package demo.h5.udesk.udeskh5demo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebStorage;
import android.webkit.WebView;

import java.io.File;


/**
 * Created by user on 2016/12/15.
 */

public class UdeskWebChromeClient extends WebChromeClient {
    private Activity mContext;
    private ValueCallback<Uri> uploadMessage;
    private ValueCallback<Uri[]> uploadMessageAboveL;
    private final static int FILE_CHOOSER_RESULT_CODE = 10000;
    ICloseWindow closeWindow = null;

    private Uri photoUri;
    private Uri videoUri;
    private final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 10001;
    private final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 10002;

    public UdeskWebChromeClient(Activity context,ICloseWindow closeWindow) {
        mContext = context;
        this.closeWindow = closeWindow;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
    }


    // For Android < 3.0
    public void openFileChooser(ValueCallback<Uri> valueCallback) {
        uploadMessage = valueCallback;
        openFileChooserActivity();
    }

    // For Android  >= 3.0
    public void openFileChooser(ValueCallback valueCallback, String acceptType) {
        uploadMessage = valueCallback;
        if (acceptType.equals("video/*")){
            videoUri = takeVedio(videoUri, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
        }else if (acceptType.equals("image/*")){
            photoUri = takePhoto(photoUri, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        }else {
            openFileChooserActivity();
        }
    }

    //For Android  >= 4.1
    public void openFileChooser(ValueCallback<Uri> valueCallback, String acceptType, String capture) {

        uploadMessage = valueCallback;
        if (acceptType.equals("video/*")){
            videoUri = takeVedio(videoUri, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
        }else if (acceptType.equals("image/*")){
            photoUri = takePhoto(photoUri, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        }else {
            openFileChooserActivity();
        }

    }

    // For Android >= 5.0
    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        uploadMessageAboveL = filePathCallback;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            String[] acceptTypes = fileChooserParams.getAcceptTypes();
            if (acceptTypes.length > 0){
                String acceptType = acceptTypes[0];
                if (acceptType.equals("video/*")){
                    videoUri = takeVedio(videoUri, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
                }else if (acceptType.equals("image/*")){
                    photoUri = takePhoto(photoUri, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
                }else {
                    openFileChooserActivity();
                }
            }else {
                openFileChooserActivity();
            }
        }else {
            openFileChooserActivity();
        }
        return true;
    }

    //窗口关闭事件，默认处理关闭activty界面，可以通过ICloseWindow  回到处理对应的逻辑
    @Override
    public void onCloseWindow(WebView window) {
        if (closeWindow !=null){
//            closeWindow.closeActivty();
        }
//        super.onCloseWindow(window);

    }

    @Override
    //扩容
    public void onReachedMaxAppCacheSize(long requiredStorage, long quota, WebStorage.QuotaUpdater quotaUpdater) {
        quotaUpdater.updateQuota(requiredStorage*2);
    }

    @Override
    public void onConsoleMessage(String message, int lineNumber, String sourceID) {
        Log.e("h5端的log", String.format("%s -- From line %s of %s", message, lineNumber, sourceID));
    }


    private void openFileChooserActivity() {

        Intent i=createFileItent();
        mContext.startActivityForResult(Intent.createChooser(i, "Image Chooser"), FILE_CHOOSER_RESULT_CODE);
    }

    /**
     * 创建选择图库的intent
     * @return
     */
    private Intent createFileItent(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        return intent;
    }


    protected Uri takeVedio(Uri uri, int code) {
        try {
            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            File file = FileUtil.getOutputVideoMediaFile(mContext);
            uri = FileUtil.getOutputMediaFileUri(mContext,file);
            if (Build.VERSION.SDK_INT >= 24) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);

            intent.addCategory(Intent.CATEGORY_DEFAULT);
            mContext.startActivityForResult(intent, code);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uri;
    }

    protected Uri takePhoto(Uri uri, int code) {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File file = FileUtil.getOutputMediaFile(mContext);
            uri = FileUtil.getOutputMediaFileUri(mContext,file);
            if (Build.VERSION.SDK_INT >= 24) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            mContext.startActivityForResult(intent, code);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uri;
    }

    public  void onActivityResult(int requestCode, int resultCode, Intent data){

            if (requestCode == FILE_CHOOSER_RESULT_CODE) {
                if (null == uploadMessage&& null == uploadMessageAboveL){
                    return;
                }
                //上传文件 点取消需要如下设置。 否则再次点击上传文件没反应
                if (data == null){
                    if (uploadMessage != null){
                        uploadMessage.onReceiveValue(null);
                        uploadMessage = null;
                    }
                    if (uploadMessageAboveL != null){
                        uploadMessageAboveL.onReceiveValue(null);
                        uploadMessageAboveL = null;
                    }
                    return;
                }
                if (uploadMessageAboveL != null) {//5.0以上
                    onActivityResultAboveL(requestCode, resultCode, data);
                }else if(uploadMessage != null) {
                    if (data != null &&  resultCode == Activity.RESULT_OK ){
                        Uri result = data.getData();
                        Log.e("xxx","5.0-result="+result);
                        uploadMessage.onReceiveValue(result);
                        uploadMessage = null;
                    }

                }

            }else  if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE){
                if ( photoUri!= null) {
                    if (uploadMessageAboveL != null){
                        Uri[] results =new Uri[1];
                        results[0] = photoUri;
                        uploadMessageAboveL.onReceiveValue(results);
                        uploadMessageAboveL = null;
                    }else if (uploadMessage != null){
                        uploadMessage.onReceiveValue(photoUri);
                        uploadMessage = null;
                    }
                }
            }else  if (requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE){
                if (videoUri != null) {
                    if (uploadMessageAboveL != null){
                        Uri[] results =new Uri[1];
                        results[0] = videoUri;
                        uploadMessageAboveL.onReceiveValue(results);
                        uploadMessageAboveL = null;
                    }else if (uploadMessage != null){
                        uploadMessage.onReceiveValue(videoUri);
                        uploadMessage = null;
                    }

                }
            }

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void onActivityResultAboveL(int requestCode, int resultCode, Intent intent) {
        Log.e("xxx","5.0+ 返回了");
        if (requestCode != FILE_CHOOSER_RESULT_CODE || uploadMessageAboveL == null){
            return;
        }
        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            if (intent != null) {
                String dataString = intent.getDataString();
                ClipData clipData = intent.getClipData();
                if (clipData != null) {
                    results = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        results[i] = item.getUri();
                    }
                }
                if (dataString != null)
                    results = new Uri[]{Uri.parse(dataString)};
            }
        }
        uploadMessageAboveL.onReceiveValue(results);
        uploadMessageAboveL = null;
    }

}
