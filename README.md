# udesk_sdk_h5
#注意：h5接入的方式风险比较大，由于android机型特多，很多定制系统。因此开发者在开发h5接入的方式，会遇见很多防不胜防的坑。
      本文demo只做参考，给h5接入提供方便，也有未知的问题没覆盖到。
## 1 WebView setting的设置
        //支持获取手势焦点，输入用户名、密码或其他
        mwebView.requestFocusFromTouch();
        mwebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        mwebView.setScrollbarFadingEnabled(false);

        final WebSettings settings = mwebView.getSettings();
        settings.setJavaScriptEnabled(true);  //支持js
        //  设置自适应屏幕，两者合用
        settings.setUseWideViewPort(true); //将图片调整到适合webview的大小
        settings.setLoadWithOverviewMode(true); // 缩放至屏幕的大小

        //若setSupportZoom是false，则该WebView不可缩放，这个不管设置什么都不能缩放。
        settings.setSupportZoom(true);  //支持缩放，默认为true。是setBuiltInZoomControls的前提。
        settings.setBuiltInZoomControls(true); //设置内置的缩放控件。
        settings.supportMultipleWindows();  //多窗口

        settings.setAllowFileAccess(true);  //设置可以访问文件
        settings.setNeedInitialFocus(true); //当webview调用requestFocus时为webview设置节点

        settings.setJavaScriptCanOpenWindowsAutomatically(true); //支持通过JS打开新窗口
        //设置编码格式
        settings.setDefaultTextEncodingName("UTF-8");
        // 关于是否缩放
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            settings.setDisplayZoomControls(false);
        }
        settings.setLoadsImagesAutomatically(true);  //支持自动加载图片

        settings.setDomStorageEnabled(true); //开启DOM Storage
        
## 2 WebView处理下载  默认不支持下载,得添加下载事件
     mwebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                // 监听下载功能，当用户点击下载链接的时候，直接调用系统的浏览器来下载
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
        
##3 给WebView设置 setWebChromeClient 和 setWebViewClient
     mwebView.setWebChromeClient(udeskWebChromeClient);
        mwebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                UdeskWebViewActivity.this.finish();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                view.loadUrl(url);
                return true;
            }
        });
        
## 4 提供UdeskWebChromeClient 样例
  
  ``` java
  
 /**
 * Created by user on 2016/12/15.
 */

public class UdeskWebChromeClient extends WebChromeClient {
    private Activity mContext;
    private ValueCallback<Uri> uploadMessage;
    private ValueCallback<Uri[]> uploadMessageAboveL;
    private final static int FILE_CHOOSER_RESULT_CODE = 10000;
    ICloseWindow closeWindow = null;



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
        openImageChooserActivity();
    }

    // For Android  >= 3.0
    public void openFileChooser(ValueCallback valueCallback, String acceptType) {
        uploadMessage = valueCallback;
        openImageChooserActivity();
    }

    //For Android  >= 4.1
    public void openFileChooser(ValueCallback<Uri> valueCallback, String acceptType, String capture) {
        uploadMessage = valueCallback;
        openImageChooserActivity();
    }

    // For Android >= 5.0
    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        uploadMessageAboveL = filePathCallback;
        openImageChooserActivity();
        return true;
    }

    //窗口关闭事件，默认处理关闭activty界面，可以通过ICloseWindow  回到处理对应的逻辑
    @Override
    public void onCloseWindow(WebView window) {
        if (closeWindow !=null){
            closeWindow.closeActivty();
        }
        super.onCloseWindow(window);

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


    private void openImageChooserActivity() {

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

```
