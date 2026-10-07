package com.yasminechat.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    private static final int AUDIO_PERMISSION = 2001;
    private static final int CAMERA_PERMISSION = 2002;
    private static final int FILE_CHOOSER = 2003;

    private PermissionRequest pendingRequest;
    private ValueCallback<Uri[]> fileCallback;

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

                    boolean wantsAudio = false;
                    boolean wantsCamera = false;

                    for (String resource : request.getResources()) {

                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                            wantsAudio = true;
                        }

                        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                            wantsCamera = true;
                        }
                    }

                    if (wantsAudio &&
                            ContextCompat.checkSelfPermission(
                                    MainActivity.this,
                                    Manifest.permission.RECORD_AUDIO
                            ) != PackageManager.PERMISSION_GRANTED) {

                        pendingRequest = request;

                        ActivityCompat.requestPermissions(
                                MainActivity.this,
                                new String[]{Manifest.permission.RECORD_AUDIO},
                                AUDIO_PERMISSION
                        );

                        return;
                    }

                    if (wantsCamera &&
                            ContextCompat.checkSelfPermission(
                                    MainActivity.this,
                                    Manifest.permission.CAMERA
                            ) != PackageManager.PERMISSION_GRANTED) {

                        pendingRequest = request;

                        ActivityCompat.requestPermissions(
                                MainActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                CAMERA_PERMISSION
                        );

                        return;
                    }

                    ArrayList<String> allowed = new ArrayList<>();

                    for (String resource : request.getResources()) {

                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)
                                && ContextCompat.checkSelfPermission(
                                MainActivity.this,
                                Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED) {

                            allowed.add(
                                    PermissionRequest.RESOURCE_AUDIO_CAPTURE
                            );
                        }

                        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)
                                && ContextCompat.checkSelfPermission(
                                MainActivity.this,
                                Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED) {

                            allowed.add(
                                    PermissionRequest.RESOURCE_VIDEO_CAPTURE
                            );
                        }
                    }

                    if (!allowed.isEmpty()) {

                        request.grant(
                                allowed.toArray(new String[0])
                        );

                    } else {

                        request.deny();
                    }
                });
            }

            @Override
            public void onPermissionRequestCanceled(
                    PermissionRequest request) {

                if (pendingRequest == request) {
                    pendingRequest = null;
                }
            }

            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {

                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }

                fileCallback = callback;

                Intent intent = new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

                intent.addCategory(
                        Intent.CATEGORY_OPENABLE
                );

                intent.setType("*/*");

                intent.putExtra(
                        Intent.EXTRA_ALLOW_MULTIPLE,
                        true
                );

                try {

                    startActivityForResult(
                            Intent.createChooser(
                                    intent,
                                    "اختيار صورة أو ملف"
                            ),
                            FILE_CHOOSER
                    );

                } catch (Exception e) {

                    fileCallback.onReceiveValue(null);
                    fileCallback = null;
                }

                return true;
            }
        });

        webView.loadUrl(
                "https://yasmine-chat.pages.dev/"
        );
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

        if (pendingRequest == null) {
            return;
        }

        PermissionRequest request = pendingRequest;
        pendingRequest = null;

        if (grantResults.length == 0 ||
                grantResults[0] != PackageManager.PERMISSION_GRANTED) {

            request.deny();
            return;
        }

        ArrayList<String> allowed = new ArrayList<>();

        for (String resource : request.getResources()) {

            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)
                    && ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED) {

                allowed.add(
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE
                );
            }

            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)
                    && ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED) {

                allowed.add(
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE
                );
            }
        }

        if (!allowed.isEmpty()) {

            request.grant(
                    allowed.toArray(new String[0])
            );

        } else {

            request.deny();
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

        if (requestCode != FILE_CHOOSER ||
                fileCallback == null) {

            return;
        }

        Uri[] results = null;

        if (resultCode == Activity.RESULT_OK &&
                data != null) {

            if (data.getClipData() != null) {

                int count =
                        data.getClipData().getItemCount();

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

        fileCallback.onReceiveValue(results);
        fileCallback = null;
    }

    @Override
    public void onBackPressed() {

        if (webView != null &&
                webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        if (pendingRequest != null) {
            pendingRequest.deny();
            pendingRequest = null;
        }

        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
