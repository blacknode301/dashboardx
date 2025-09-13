package com.blacknode301.dashboardx;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.database.Cursor;
import android.widget.Toast;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.pm.PackageManager;

public class MainActivity extends Activity {

    private WebView webView;
    private static final int REQUEST_READ_CONTACTS = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        // Настройка WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true); // Для localStorage и sessionStorage
        webSettings.setAppCacheEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT); // Использовать кэш
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW); // HTTP + HTTPS

        // Чтобы все ссылки открывались в WebView, а не в браузере
        webView.setWebViewClient(new WebViewClient());

        // Подключаем JSBridge
        webView.addJavascriptInterface(new JSBridge(), "androidBridge");

        // Загружаем страницу из Node.js (например, сервер в Termux на 3000 порту)
        webView.loadUrl("http://127.0.0.1:3000/");

        setContentView(webView);
    }

    // JSBridge для вызова методов из JS
    public class JSBridge {

        @JavascriptInterface
        public void showToast(String msg) {
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
        }

        // Метод получения контактов
        @JavascriptInterface
		public String getContacts() {
			// Проверка разрешения
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
				checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {

				requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS}, REQUEST_READ_CONTACTS);
				return "[]"; // возвращаем пустой массив, если разрешение ещё не получено
			}

			JSONArray contactsArray = new JSONArray();

			try {
				Cursor cursor = MainActivity.this.getContentResolver().query(
					ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
					null, null, null, null
				);

				if (cursor != null) {
					while (cursor.moveToNext()) {
						String name = cursor.getString(cursor.getColumnIndex(
														   ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
						String number = cursor.getString(cursor.getColumnIndex(
															 ContactsContract.CommonDataKinds.Phone.NUMBER));

						// Убираем пробелы, дефисы, скобки
						number = number.replaceAll("[\\s\\-()]", "");

						// Если номер начинается с *, # — оставляем как есть
						//if (number.startsWith("*") || number.startsWith("#")) {
						// ничего не делаем
						//}
						// Если короткий номер (1–5 цифр) — оставляем как есть
						//else if (number.length() <= 5) {
						// ничего не делаем
						//}
						// Если номер начинается с 8 и длинный — заменяем на +7
						//else if (number.startsWith("8") && number.length() > 5) {
						//number = "+7" + number.substring(1);
						//}
						// Если номер не начинается с + — добавляем +7 (только для обычных национальных номеров)
						//else if (!number.startsWith("+")) {
						//number = "+7" + number;
						//}

						JSONObject contact = new JSONObject();
						contact.put("name", name);
						contact.put("number", number);
						contactsArray.put(contact);
					}
					cursor.close();
				}
			} catch (Exception e) {
				Log.e("JSBridge", "Error reading contacts", e);
			}

			return contactsArray.toString();
		}
	}
}
