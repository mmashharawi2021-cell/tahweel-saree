package com.mashharawi.tahweelsaree;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQUEST_CALL = 7;
    private static final int REQUEST_CONTACT = 8;
    private static final String PREFS = "tahweel_saree_prefs";
    private static final String KEY_FAVORITES = "favorites";
    private static final String KEY_HISTORY = "history";
    private static final String KEY_PIN = "jawwal_pin";
    private static final String KEY_LOCK = "app_lock";

    private String wallet = "jawwal";
    private String pendingCode = "";
    private EditText pinInput;
    private EditText phoneInput;
    private EditText amountInput;
    private CheckBox savePinCheck;
    private LinearLayout formContainer;
    private LinearLayout favoritesContainer;
    private LinearLayout historyContainer;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.getString(KEY_LOCK, "").isEmpty()) {
            showLockScreen();
        } else {
            showMainScreen();
        }
    }

    private void showLockScreen() {
        LinearLayout root = baseLayout();
        EditText input = field("رمز الدخول السريع", true);
        Button enter = button("دخول");
        enter.setOnClickListener(v -> {
            if (prefs.getString(KEY_LOCK, "").equals(input.getText().toString().trim())) {
                showMainScreen();
            } else {
                toast("رمز غير صحيح");
            }
        });
        root.addView(title("تحويل سريع"));
        root.addView(input);
        root.addView(space());
        root.addView(enter);
        setContentView(wrap(root));
    }

    private void showMainScreen() {
        LinearLayout root = baseLayout();
        root.addView(title("تحويل سريع"));
        root.addView(label("تنفيذ أكواد USSD لجوال باي وبال باي بسرعة بعد شاشة تأكيد."));

        LinearLayout walletRow = new LinearLayout(this);
        walletRow.setOrientation(LinearLayout.HORIZONTAL);
        Button jawwalButton = button("جوال باي");
        Button palpayButton = button("بال باي");
        walletRow.addView(jawwalButton, weighted());
        walletRow.addView(palpayButton, weighted());
        root.addView(walletRow);

        formContainer = card();
        root.addView(formContainer);

        jawwalButton.setOnClickListener(v -> {
            wallet = "jawwal";
            renderForm();
        });
        palpayButton.setOnClickListener(v -> {
            wallet = "palpay";
            renderForm();
        });

        root.addView(section("المفضلة"));
        favoritesContainer = card();
        root.addView(favoritesContainer);

        root.addView(section("آخر العمليات"));
        historyContainer = card();
        root.addView(historyContainer);

        root.addView(section("عن التطبيق"));
        LinearLayout about = card();
        about.addView(label("Developed by Mohanad Al-Mashharawi\nGIS Engineer | Android App Developer"));
        Button whatsappButton = button("تواصل واتساب");
        whatsappButton.setOnClickListener(v -> openUrl("https://wa.me/970599876261"));
        Button lockButton = button("إعداد رمز دخول سريع");
        lockButton.setOnClickListener(v -> setQuickLock());
        about.addView(whatsappButton);
        about.addView(space());
        about.addView(lockButton);
        root.addView(about);

        renderForm();
        renderFavorites();
        renderHistory();
        setContentView(wrap(root));
    }

    private void renderForm() {
        formContainer.removeAllViews();
        formContainer.setAlpha(0f);
        formContainer.setTranslationY(24f);

        if ("jawwal".equals(wallet)) {
            pinInput = field("PIN جوال باي", true);
            pinInput.setText(prefs.getString(KEY_PIN, ""));
            savePinCheck = new CheckBox(this);
            savePinCheck.setText("حفظ PIN محليًا");
            savePinCheck.setTextColor(Color.WHITE);
            formContainer.addView(pinInput);
            formContainer.addView(savePinCheck);
        }

        phoneInput = field("رقم المستلم", false);
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        amountInput = field("المبلغ", false);
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        Button pickContactButton = button("اختيار من جهات الاتصال");
        pickContactButton.setOnClickListener(v -> pickContact());

        HorizontalScrollView quickScroll = new HorizontalScrollView(this);
        LinearLayout quickRow = new LinearLayout(this);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        int[] amounts = new int[]{3, 6, 9, 12, 15, 30, 60, 90, 120, 150, 300};
        for (int n : amounts) {
            Button b = new Button(this);
            b.setText(String.valueOf(n));
            b.setOnClickListener(v -> amountInput.setText(String.valueOf(n)));
            quickRow.addView(b);
        }
        quickScroll.addView(quickRow);

        Button addFavoriteButton = button("إضافة للمفضلة");
        addFavoriteButton.setOnClickListener(v -> addFavorite());

        Button confirmButton = button("تأكيد التحويل");
        confirmButton.setOnClickListener(v -> confirmTransfer());

        Button openMenuButton = button("فتح القائمة الأصلية");
        openMenuButton.setOnClickListener(v -> callCode("jawwal".equals(wallet) ? "*110#" : "*370#"));

        formContainer.addView(phoneInput);
        formContainer.addView(space());
        formContainer.addView(pickContactButton);
        formContainer.addView(space());
        formContainer.addView(amountInput);
        formContainer.addView(space());
        formContainer.addView(quickScroll);
        formContainer.addView(space());
        formContainer.addView(addFavoriteButton);
        formContainer.addView(space());
        formContainer.addView(confirmButton);
        formContainer.addView(space());
        formContainer.addView(openMenuButton);

        formContainer.animate().alpha(1f).translationY(0f).setDuration(220).start();
    }

    private void confirmTransfer() {
        String phone = clean(phoneInput.getText().toString());
        String amount = amountInput.getText().toString().trim();
        String pin = "jawwal".equals(wallet) ? pinInput.getText().toString().trim() : "";

        if (!phone.matches("05[69][0-9]{7}")) {
            toast("رقم غير صحيح");
            return;
        }
        if (amount.isEmpty() || Integer.parseInt(amount) <= 0) {
            toast("مبلغ غير صحيح");
            return;
        }
        if ("jawwal".equals(wallet) && pin.isEmpty()) {
            toast("أدخل PIN");
            return;
        }

        String code = "jawwal".equals(wallet)
                ? "*110*1*" + pin + "*" + phone + "*" + amount + "*1#"
                : "*370*1*1*" + phone + "*" + amount + "#";

        new AlertDialog.Builder(this)
                .setTitle("تأكيد التحويل")
                .setMessage("المحفظة: " + ("jawwal".equals(wallet) ? "جوال باي" : "بال باي") + "\nالرقم: " + phone + "\nالمبلغ: " + amount)
                .setPositiveButton("تنفيذ", (d, w) -> {
                    if ("jawwal".equals(wallet) && savePinCheck != null && savePinCheck.isChecked()) {
                        prefs.edit().putString(KEY_PIN, pin).apply();
                    }
                    saveHistory(phone, amount);
                    callCode(code);
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void callCode(String code) {
        pendingCode = code;
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL);
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + code.replace("#", "%23"))));
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + code.replace("#", "%23"))));
        }
    }

    private void pickContact() {
        try {
            startActivityForResult(new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI), REQUEST_CONTACT);
        } catch (Exception e) {
            toast("تعذر فتح جهات الاتصال");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CONTACT && resultCode == RESULT_OK && data != null) {
            Cursor cursor = getContentResolver().query(data.getData(), new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    phoneInput.setText(clean(cursor.getString(0)));
                }
                cursor.close();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALL && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            callCode(pendingCode);
        }
    }

    private void addFavorite() {
        String phone = clean(phoneInput.getText().toString());
        if (!phone.matches("05[69][0-9]{7}")) {
            toast("أدخل رقمًا صحيحًا");
            return;
        }
        EditText nameInput = field("اسم المفضلة", false);
        new AlertDialog.Builder(this)
                .setTitle("إضافة للمفضلة")
                .setView(nameInput)
                .setPositiveButton("حفظ", (d, w) -> {
                    String old = prefs.getString(KEY_FAVORITES, "");
                    prefs.edit().putString(KEY_FAVORITES, nameInput.getText().toString().trim() + "|" + phone + ";;" + old).apply();
                    renderFavorites();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void renderFavorites() {
        favoritesContainer.removeAllViews();
        String raw = prefs.getString(KEY_FAVORITES, "");
        if (raw.trim().isEmpty()) {
            favoritesContainer.addView(label("لا توجد عناصر مفضلة بعد."));
            return;
        }
        for (String item : raw.split(";;")) {
            if (item.trim().isEmpty()) continue;
            String[] parts = item.split("\\|");
            Button b = button(parts[0] + " - " + (parts.length > 1 ? parts[1] : ""));
            if (parts.length > 1) {
                String phone = parts[1];
                b.setOnClickListener(v -> phoneInput.setText(phone));
            }
            favoritesContainer.addView(b);
            favoritesContainer.addView(space());
        }
    }

    private void saveHistory(String phone, String amount) {
        String entry = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date())
                + " | " + ("jawwal".equals(wallet) ? "جوال باي" : "بال باي")
                + " | " + phone + " | " + amount;
        String old = prefs.getString(KEY_HISTORY, "");
        prefs.edit().putString(KEY_HISTORY, entry + ";;" + old).apply();
        renderHistory();
    }

    private void renderHistory() {
        historyContainer.removeAllViews();
        String raw = prefs.getString(KEY_HISTORY, "");
        if (raw.trim().isEmpty()) {
            historyContainer.addView(label("لا توجد عمليات بعد."));
            return;
        }
        int count = 0;
        for (String item : raw.split(";;")) {
            if (item.trim().isEmpty()) continue;
            historyContainer.addView(label(item));
            historyContainer.addView(space());
            count++;
            if (count >= 10) break;
        }
    }

    private void setQuickLock() {
        EditText input = field("رمز الدخول الجديد", true);
        new AlertDialog.Builder(this)
                .setTitle("إعداد رمز دخول سريع")
                .setView(input)
                .setPositiveButton("حفظ", (d, w) -> {
                    prefs.edit().putString(KEY_LOCK, input.getText().toString().trim()).apply();
                    toast("تم حفظ الرمز");
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {
        }
    }

    private LinearLayout baseLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(24));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(Color.parseColor("#07182A"));
        return root;
    }

    private ScrollView wrap(View view) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(view);
        return scrollView;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundColor(Color.parseColor("#1AFFFFFF"));
        return card;
    }

    private TextView title(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(26);
        return tv;
    }

    private TextView section(String text) {
        TextView tv = title(text);
        tv.setTextSize(18);
        tv.setPadding(0, dp(18), 0, dp(8));
        return tv;
    }

    private TextView label(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#D6E8EE"));
        tv.setTextSize(14);
        return tv;
    }

    private EditText field(String hint, boolean password) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setHintTextColor(Color.parseColor("#99FFFFFF"));
        editText.setTextColor(Color.WHITE);
        editText.setPadding(dp(14), dp(12), dp(14), dp(12));
        if (password) {
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
        return editText;
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        return button;
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(4), 0, dp(4), 0);
        return params;
    }

    private View space() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)));
        return v;
    }

    private String clean(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
