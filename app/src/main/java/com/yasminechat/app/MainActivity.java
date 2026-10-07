package com.yasminechat.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.PermissionRequest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int FILE_CHOOSER_REQUEST_CODE = 1002;

    private PermissionRequest pendingPermissionRequest;
    private ValueCallback<Uri[]> filePathCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onPermissionRequest(final PermissionRequest request) {

                runOnUiThread(() -> {

                    boolean needAudio = false;
                    boolean needCamera = false;

                    for (String resource : request.getResources()) {

                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                            needAudio = true;
                        }

                        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                            needCamera = true;
                        }
                    }

                    boolean audioGranted =
                            ContextCompat.checkSelfPermission(
                                    MainActivity.this,
                                    Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED;

                    boolean cameraGranted =
                            ContextCompat.checkSelfPermission(
                                    MainActivity.this,
                                    Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED;

                    if ((!needAudio || audioGranted) &&
                            (!needCamera || cameraGranted)) {

                        request.grant(request.getResources());

                    } else {

                        pendingPermissionRequest = request;

                        requestRequiredPermissions(needAudio, needCamera);
                    }
                });
            }

            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {

                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }

                MainActivity.this.filePathCallback = filePathCallback;

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

                String[] acceptTypes = fileChooserParams.getAcceptTypes();

                if (acceptTypes != null && acceptTypes.length > 0) {

                    if (acceptTypes.length == 1 && !acceptTypes[0].isEmpty()) {
                        intent.setType(acceptTypes[0]);
                    } else {
                        intent.setType("*/*");
                    }
                }

                try {
                    startActivityForResult(
                            Intent.createChooser(intent, "اختيار ملف"),
                            FILE_CHOOSER_REQUEST_CODE
                    );
                } catch (Exception e) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                    MainActivity.this.filePathCallback = null;
                }

                return true;
            }
        });

        webView.loadUrl("https://yasmine-chat.pages.dev/");

        requestInitialPermissions();
    }

    private void requestInitialPermissions() {

        boolean audioNeeded =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED;

        boolean cameraNeeded =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED;

        if (audioNeeded || cameraNeeded) {

            requestRequiredPermissions(audioNeeded, cameraNeeded);
        }
    }

    private void requestRequiredPermissions(
            boolean needAudio,
            boolean needCamera) {

        java.util.ArrayList<String> permissions =
                new java.util.ArrayList<>();

        if (needAudio &&
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED) {

            permissions.add(Manifest.permission.RECORD_AUDIO);
        }

        if (needCamera &&
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED) {

            permissions.add(Manifest.permission.CAMERA);
        }

        if (!permissions.isEmpty()) {

            ActivityCompat.requestPermissions(
                    this,
                    permissions.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == PERMISSION_REQUEST_CODE) {

            if (pendingPermissionRequest != null) {

                boolean audioGranted =
                        ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED;

                boolean cameraGranted =
                        ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED;

                boolean canGrant = true;

                for (String resource :
                        pendingPermissionRequest.getResources()) {

                    if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)
                            && !audioGranted) {

                        canGrant = false;
                    }

                    if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)
                            && !cameraGranted) {

                        canGrant = false;
                    }
                }

                if (canGrant) {
                    pendingPermissionRequest.grant(
                            pendingPermissionRequest.getResources()
                    );
                } else {
                    pendingPermissionRequest.deny();
                }

                pendingPermissionRequest = null;
            }
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {

            if (filePathCallback == null) {
                return;
            }

            Uri[] results = null;

            if (resultCode == Activity.RESULT_OK && data != null) {

                if (data.getClipData() != null) {

                    int count = data.getClipData().getItemCount();

                    results = new Uri[count];

                    for (int i = 0; i < count; i++) {
                        results[i] =
                                data.getClipData()
                                        .getItemAt(i)
                                        .getUri();
                    }

                } else if (data.getData() != null) {

                    results = new Uri[]{
                            data.getData()
                    };
                }
            }

            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override
    public void onBackPressed() {

        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        if (webView != null) {
            webView.destroy();
        }

        super.onDestroy();
    }
}
