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
    private static final String REDIRECT_URI = "https://spotify-callback-three.vercel.app/api/callback";
    private static final String SCOPES = "user-modify-playback-state user-read-playback-state";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        WebView webView = findViewById(R.id.webView);
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
                System.out.println("URL cargada: " + url);
                if (url.startsWith(REDIRECT_URI)) {
                    if (url.contains("code=")) {
                        String code = url.substring(url.indexOf("code=") + 5);
                        System.out.println("Authorization Code: " + code);
                        // Aquí puedes manejar el código, por ejemplo, enviarlo a tu backend
                        exchangeCodeForToken(code, CLIENT_ID, "c3cead8258cc4cf29a467edf9018cfcf", REDIRECT_URI, new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                e.printStackTrace();
                            }


                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                if (response.isSuccessful() && response.body() != null) {
                                    String responseBody = response.body().string();
                                    Log.d("SPOTIFY_API_RESPONSE", "Respuesta de la API: " + responseBody);

                                    try {
                                        JSONObject json = new JSONObject(responseBody);
                                        String accessToken = json.getString("access_token");
                                        String refreshToken = json.optString("refresh_token", null); // Puede no estar presente

                                        Log.d("SPOTIFY_API_RESPONSE", "Access Token: " + accessToken);
                                        Log.d("SPOTIFY_API_RESPONSE", "Refresh Token: " + refreshToken);

                                        saveTokens(accessToken, refreshToken);

                                        runOnUiThread(() -> {
                                            Intent intent = new Intent(SpotifyAuthActivity.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        });
                                    } catch (org.json.JSONException e) {
                                        Log.e("SPOTIFY_API_RESPONSE", "Error al parsear el JSON: " + e.getMessage(), e);
                                    } finally {
                                        response.body().close(); // Liberar recursos
                                    }
                                } else {
                                    Log.e("SPOTIFY_API_RESPONSE", "Error al obtener el token: " + response.message());
                                }
                            }
                        });
                    } else {
                        System.out.println("Error: No se encontró el código de autorización.");
                    }
                    return true; // Detiene la carga de la URL en el WebView
                }
                return false; // Permite que el WebView cargue otras URLs
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                System.out.println("Página cargada: " + url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                System.out.println("Error al cargar la URL: " + description);
            }
        });
        String authUrl = "https://accounts.spotify.com/authorize?client_id=" + CLIENT_ID +
                "&response_type=code&redirect_uri=" + REDIRECT_URI +
                "&scope=" + SCOPES.replace(" ", "%20");
        webView.loadUrl(authUrl);
    }

    private void saveTokens(String accessToken, String refreshToken) {
        SharedPreferences.Editor editor = getSharedPreferences("SpotifyPrefs", MODE_PRIVATE).edit();
        editor.putString("access_token", accessToken);
        if (refreshToken != null) {
            editor.putString("refresh_token", refreshToken);
        }
        editor.apply();
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

    private void saveAccessToken(String accessToken) {
        getSharedPreferences("SpotifyPrefs", MODE_PRIVATE)
                .edit()
                .putString("access_token", accessToken)
                .apply();
    }

    private void getAuthState() {
        String accessToken = getSharedPreferences("SpotifyPrefs", MODE_PRIVATE)
                .getString("access_token", null);

        if (accessToken != null) {
            // Verificar si el token es válido
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://api.spotify.com/v1/me")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    // Redirigir al flujo de autenticación en caso de error
                    redirectToAuth();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        // Token válido, redirigir a la actividad principal
                        Intent intent = new Intent(SpotifyAuthActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Token inválido o caducado, redirigir al flujo de autenticación
                        redirectToAuth();
                    }
                }
            });
        } else {
            // No hay token, redirigir al flujo de autenticación
            redirectToAuth();
        }
    }



    private void redirectToAuth() {
        if (this instanceof SpotifyAuthActivity) {
            Log.d("SpotifyAuthActivity", "Ya estás en SpotifyAuthActivity.");
            return; // No redirigir si ya estás en esta actividad
        }

        // Redirigir a la página de autenticación
        Intent intent = new Intent(SpotifyAuthActivity.this, SpotifyAuthActivity.class);
        startActivity(intent);
        finish();
    }
}
