package com.example.alarmamusical;

        import android.app.Activity;
        import android.content.Intent;
        import android.content.SharedPreferences;
        import android.os.Bundle;
        import android.util.Base64;
        import android.util.Log;
        import android.webkit.CookieManager;
        import android.webkit.WebView;
        import android.webkit.WebViewClient;

        import com.spotify.android.appremote.api.ConnectionParams;
        import com.spotify.android.appremote.api.Connector;
        import com.spotify.android.appremote.api.SpotifyAppRemote;

        import org.json.JSONObject;

        import java.io.IOException;

        import okhttp3.Call;
        import okhttp3.Callback;
        import okhttp3.FormBody;
        import okhttp3.OkHttpClient;
        import okhttp3.Request;
        import okhttp3.RequestBody;
        import okhttp3.Response;

        public class SpotifyAuthActivity extends Activity {
            private static final String CLIENT_ID = "dc51fd1033aa43209b33ba5c17eb0c2a";
            private static final String REDIRECT_URI = "com.example.alarmamusical://callback";
            private static final String SCOPES = "user-modify-playback-state user-read-playback-state";
            private SpotifyAppRemote spotifyAppRemote;

            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_auth);

                connectToSpotify();

                WebView webView = findViewById(R.id.webView);
                setupWebView(webView);
            }

            private void setupWebView(WebView webView) {
                webView.getSettings().setJavaScriptEnabled(true);
                WebView.setWebContentsDebuggingEnabled(true);
                webView.getSettings().setDomStorageEnabled(true);
                webView.getSettings().setDatabaseEnabled(true);
                webView.getSettings().setSupportMultipleWindows(true);

                // Limpiar cookies y caché
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.removeAllCookies(null);
                cookieManager.flush();
                webView.clearCache(true);

                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        if (url.startsWith(REDIRECT_URI)) {
                            if (url.contains("code=")) {
                                String code = url.substring(url.indexOf("code=") + 5);
                                exchangeCodeForToken(code, CLIENT_ID, "c3cead8258cc4cf29a467edf9018cfcf", REDIRECT_URI, new Callback() {
                                    @Override
                                    public void onFailure(Call call, IOException e) {
                                        Log.e("SpotifyAuthActivity", "Error al intercambiar el código: " + e.getMessage(), e);
                                    }

                                    @Override
                                    public void onResponse(Call call, Response response) throws IOException {
                                        if (response.isSuccessful() && response.body() != null) {
                                            handleTokenResponse(response.body().string());
                                        } else {
                                            Log.e("SpotifyAuthActivity", "Error al obtener el token: " + response.message());
                                        }
                                    }
                                });
                            }
                            return true;
                        }
                        return false;
                    }
                });

                String authUrl = "https://accounts.spotify.com/authorize?client_id=" + CLIENT_ID +
                        "&response_type=code&redirect_uri=" + REDIRECT_URI +
                        "&scope=" + SCOPES.replace(" ", "%20");
                webView.loadUrl(authUrl);
            }

            private void connectToSpotify() {
                ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();

                SpotifyAppRemote.connect(this, connectionParams, new Connector.ConnectionListener() {
                    @Override
                    public void onConnected(SpotifyAppRemote remote) {
                        spotifyAppRemote = remote;
                        Log.d("SpotifyAuthActivity", "Conexión exitosa con Spotify.");
                        playTrack("spotify:track:4uLU6hMCjMI75M1A2tKUQC"); // Reproduce una canción de ejemplo
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        Log.e("SpotifyAuthActivity", "Error al conectar con Spotify: " + error.getMessage(), error);
                    }
                });
            }

            private void playTrack(String trackUri) {
                if (spotifyAppRemote != null) {
                    spotifyAppRemote.getPlayerApi().play(trackUri);
                    Log.d("SpotifyAuthActivity", "Reproduciendo: " + trackUri);
                } else {
                    Log.e("SpotifyAuthActivity", "SpotifyAppRemote no está conectado.");
                }
            }

            private void handleTokenResponse(String responseBody) {
                try {
                    JSONObject json = new JSONObject(responseBody);
                    String accessToken = json.getString("access_token");
                    String refreshToken = json.optString("refresh_token", null);

                    saveTokens(accessToken, refreshToken);

                    runOnUiThread(() -> {
                        Intent intent = new Intent(SpotifyAuthActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    });
                } catch (Exception e) {
                    Log.e("SpotifyAuthActivity", "Error al procesar la respuesta del token: " + e.getMessage(), e);
                }
            }

            private void saveTokens(String accessToken, String refreshToken) {
                SharedPreferences.Editor editor = getSharedPreferences("SpotifyPrefs", MODE_PRIVATE).edit();
                editor.putString("access_token", accessToken);
                if (refreshToken != null) {
                    editor.putString("refresh_token", refreshToken);
                }
                editor.apply();
            }

            @Override
            protected void onStop() {
                super.onStop();
                if (spotifyAppRemote != null) {
                    SpotifyAppRemote.disconnect(spotifyAppRemote);
                }
            }

            public void exchangeCodeForToken(String code, String clientId, String clientSecret, String redirectUri, Callback callback) {
                String credentials = clientId + ":" + clientSecret;
                String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

                RequestBody body = new FormBody.Builder()
                        .add("grant_type", "authorization_code")
                        .add("code", code)
                        .add("redirect_uri", redirectUri)
                        .build();

                Request request = new Request.Builder()
                        .url("https://accounts.spotify.com/api/token")
                        .addHeader("Authorization", basicAuth)
                        .post(body)
                        .build();

                OkHttpClient client = new OkHttpClient();
                client.newCall(request).enqueue(callback);
            }
        }