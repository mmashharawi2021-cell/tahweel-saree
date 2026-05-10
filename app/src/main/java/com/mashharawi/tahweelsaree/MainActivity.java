package com.mashharawi.tahweelsaree;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
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

    private static final int BG = Color.parseColor("#07182A");
    private static final int CARD = Color.parseColor("#16253A");
    private static final int CARD_SOFT = Color.parseColor("#1D2E46");
    private static final int TEXT = Color.WHITE;
    private static final int MUTED = Color.parseColor("#B8CCD8");
    private static final int TEAL = Color.parseColor("#14D4C8");
    private static final int BLUE = Color.parseColor("#2E6BFF");
    private static final int BORDER = Color.parseColor("#2A4964");

    private String wallet = "jawwal";
    private String pendingCode = "";
    private EditText pinInput;
    private EditText phoneInput;
    private EditText amountInput;
    private CheckBox savePinCheck;
    private LinearLayout formContainer;
    private LinearLayout favoritesContainer;
    private LinearLayout historyContainer;
    private Button jawwalButton;
    private Button palpayButton;
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
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(headerCard());

        LinearLayout lockCard = card();
        lockCard.addView(sectionTitle("فتح التطبيق"));
        EditText input = field("رمز الدخول السريع", true);
        Button enter = primaryButton("دخول");
        enter.setOnClickListener(v -> {
            if (prefs.getString(KEY_LOCK, "").equals(input.getText().toString().trim())) {
                showMainScreen();
            } else {
                toast("رمز غير صحيح");
            }
        });
        lockCard.addView(input);
        lockCard.addView(space(12));
        lockCard.addView(enter);
        root.addView(lockCard);
        setContentView(wrap(root));
    }

    private void showMainScreen() {
        LinearLayout root = baseLayout();
        root.addView(headerCard());
        root.addView(walletSwitcherCard());

        formContainer = card();
        root.addView(formContainer);

        root.addView(sectionLabel("المفضلة"));
        favoritesContainer = card();
        root.addView(favoritesContainer);

        root.addView(sectionLabel("آخر العمليات"));
        historyContainer = card();
        root.addView(historyContainer);

        root.addView(sectionLabel("عن التطبيق"));
        root.addView(aboutCard());

        renderForm();
        renderFavorites();
        renderHistory();
        setContentView(wrap(root));
    }

    private View headerCard() {
        LinearLayout header = card();
        header.setGravity(Gravity.CENTER_HORIZONTAL);
        header.setPadding(dp(20), dp(22), dp(20), dp(22));

        TextView logo = new TextView(this);
        logo.setText("⇄");
        logo.setTextSize(42);
        logo.setTextColor(TEAL);
        logo.setGravity(Gravity.CENTER);

        TextView title = new TextView(this);
        title.setText("تحويل سريع");
        title.setTextColor(TEXT);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText("تحويل سريع منظم وواضح لمحافظ USSD بدون زحمة واجهة.");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);

        header.addView(logo);
        header.addView(space(8));
        header.addView(title);
        header.addView(space(6));
        header.addView(subtitle);
        return header;
    }

    private View walletSwitcherCard() {
        LinearLayout box = card();
        box.addView(sectionTitle("اختر المحفظة"));
        box.addView(helperText("عند تبديل المحفظة تظهر الحقول المطلوبة فقط مع انتقال ناعم."));
        box.addView(space(12));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        jawwalButton = switchButton("جوال باي");
        palpayButton = switchButton("بال باي");

        jawwalButton.setOnClickListener(v -> {
            wallet = "jawwal";
            updateWalletButtons();
            renderForm();
        });
        palpayButton.setOnClickListener(v -> {
            wallet = "palpay";
            updateWalletButtons();
            renderForm();
        });

        row.addView(jawwalButton, weighted());
        row.addView(palpayButton, weighted());
        box.addView(row);
        updateWalletButtons();
        return box;
    }

    private void updateWalletButtons() {
        styleSwitch(jawwalButton, "jawwal".equals(wallet));
        styleSwitch(palpayButton, "palpay".equals(wallet));
    }

    private void renderForm() {
        formContainer.removeAllViews();
        formContainer.setAlpha(0f);
        formContainer.setTranslationY(dp(18));

        formContainer.addView(sectionTitle("بيانات التحويل"));
        formContainer.addView(helperText("أدخل البيانات ثم راجعها في شاشة التأكيد قبل التنفيذ."));
        formContainer.addView(space(12));

        if ("jawwal".equals(wallet)) {
            pinInput = field("PIN جوال باي", true);
            pinInput.setText(prefs.getString(KEY_PIN, ""));
            savePinCheck = new CheckBox(this);
            savePinCheck.setText("حفظ PIN محليًا لهذا الجهاز");
            savePinCheck.setTextColor(MUTED);
            formContainer.addView(pinInput);
            formContainer.addView(space(6));
            formContainer.addView(savePinCheck);
            formContainer.addView(space(10));
        }

        phoneInput = field("رقم المستلم", false);
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        amountInput = field("المبلغ", false);
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        Button pickContactButton = softButton("اختيار من جهات الاتصال");
        pickContactButton.setOnClickListener(v -> pickContact());

        Button addFavoriteButton = softButton("إضافة للمفضلة");
        addFavoriteButton.setOnClickListener(v -> addFavorite());

        Button confirmButton = primaryButton("تأكيد التحويل");
        confirmButton.setOnClickListener(v -> confirmTransfer());

        Button openMenuButton = softButton("فتح القائمة الأصلية");
        openMenuButton.setOnClickListener(v -> callCode("jawwal".equals(wallet) ? "*110#" : "*370#"));

        formContainer.addView(phoneInput);
        formContainer.addView(space(10));
        formContainer.addView(pickContactButton);
        formContainer.addView(space(10));
        formContainer.addView(amountInput);
        formContainer.addView(space(12));
        formContainer.addView(quickAmountsCard());
        formContainer.addView(space(12));
        formContainer.addView(addFavoriteButton);
        formContainer.addView(space(10));
        formContainer.addView(confirmButton);
        formContainer.addView(space(10));
        formContainer.addView(openMenuButton);

        formContainer.animate().alpha(1f).translationY(0f).setDuration(260).start();
    }

    private View quickAmountsCard() {
        LinearLayout box = innerCard();
        box.addView(helperText("مبالغ سريعة"));
        box.addView(space(8));

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        int[] amounts = new int[]{3, 6, 9, 12, 15, 30, 60, 90, 120, 150, 300};
        for (int value : amounts) {
            Button chip = chipButton(String.valueOf(value));
            chip.setOnClickListener(v -> amountInput.setText(String.valueOf(value)));
            row.addView(chip);
        }
        scroll.addView(row);
        box.addView(scroll);
        return box;
    }

    private void confirmTransfer() {
        String phone = clean(phoneInput.getText().toString());
        String amount = amountInput.getText().toString().trim();
        String pin = "jawwal".equals(wallet) ? pinInput.getText().toString().trim() : "";

        if (!phone.matches("05[69][0-9]{7}")) {
            toast("رقم غير صحيح");
            return;
        }
        if (amount.isEmpty()) {
            toast("أدخل مبلغًا صحيحًا");
            return;
        }
        try {
            if (Integer.parseInt(amount) <= 0) {
                toast("أدخل مبلغًا صحيحًا");
                return;
            }
        } catch (Exception e) {
            toast("أدخل مبلغًا صحيحًا");
            return;
        }
        if ("jawwal".equals(wallet) && pin.isEmpty()) {
            toast("أدخل PIN");
            return;
        }

        String code = "jawwal".equals(wallet)
                ? "*110*1*" + pin + "*" + phone + "*" + amount + "*1#"
                : "*370*1*1*" + phone + "*" + amount + "#";

        String summary = "المحفظة: " + ("jawwal".equals(wallet) ? "جوال باي" : "بال باي")
                + "\nالرقم: " + phone
                + "\nالمبلغ: " + amount
                + "\n\nراجع البيانات قبل التنفيذ.";

        new AlertDialog.Builder(this)
                .setTitle("تأكيد التحويل")
                .setMessage(summary)
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
                if (cursor.moveToFirst() && phoneInput != null) {
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
            toast("أدخل رقمًا صحيحًا أولًا");
            return;
        }
        EditText nameInput = field("اسم المفضلة", false);
        new AlertDialog.Builder(this)
                .setTitle("إضافة للمفضلة")
                .setView(nameInput)
                .setPositiveButton("حفظ", (d, w) -> {
                    String old = prefs.getString(KEY_FAVORITES, "");
                    String name = nameInput.getText().toString().trim();
                    if (name.isEmpty()) name = "جهة محفوظة";
                    prefs.edit().putString(KEY_FAVORITES, name + "|" + phone + ";;" + old).apply();
                    renderFavorites();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void renderFavorites() {
        favoritesContainer.removeAllViews();
        String raw = prefs.getString(KEY_FAVORITES, "");
        if (raw.trim().isEmpty()) {
            favoritesContainer.addView(helperText("لا توجد جهات مفضلة بعد."));
            return;
        }
        int count = 0;
        for (String item : raw.split(";;")) {
            if (item.trim().isEmpty()) continue;
            String[] parts = item.split("\\|");
            LinearLayout itemCard = innerCard();
            TextView name = new TextView(this);
            name.setText(parts[0]);
            name.setTextColor(TEXT);
            name.setTextSize(16);
            name.setTypeface(Typeface.DEFAULT_BOLD);
            TextView phone = helperText(parts.length > 1 ? parts[1] : "");
            Button use = softButton("استخدام");
            if (parts.length > 1) {
                String p = parts[1];
                use.setOnClickListener(v -> {
                    if (phoneInput != null) phoneInput.setText(p);
                    if (amountInput != null) amountInput.requestFocus();
                });
            }
            itemCard.addView(name);
            itemCard.addView(space(4));
            itemCard.addView(phone);
            itemCard.addView(space(10));
            itemCard.addView(use);
            favoritesContainer.addView(itemCard);
            favoritesContainer.addView(space(10));
            count++;
            if (count >= 6) break;
        }
    }

    private void saveHistory(String phone, String amount) {
        String entry = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date())
                + " | " + ("jawwal".equals(wallet) ? "جوال باي" : "بال باي")
                + " | " + phone
                + " | " + amount;
        String old = prefs.getString(KEY_HISTORY, "");
        prefs.edit().putString(KEY_HISTORY, entry + ";;" + old).apply();
        renderHistory();
    }

    private void renderHistory() {
        historyContainer.removeAllViews();
        String raw = prefs.getString(KEY_HISTORY, "");
        if (raw.trim().isEmpty()) {
            historyContainer.addView(helperText("لا توجد عمليات بعد."));
            return;
        }
        int count = 0;
        for (String item : raw.split(";;")) {
            if (item.trim().isEmpty()) continue;
            TextView line = helperText(item);
            line.setPadding(dp(10), dp(10), dp(10), dp(10));
            line.setBackground(strokeBackground(CARD_SOFT, BORDER, 14));
            historyContainer.addView(line);
            historyContainer.addView(space(8));
            count++;
            if (count >= 8) break;
        }
    }

    private View aboutCard() {
        LinearLayout about = card();
        about.addView(helperText("Developed by Mohanad Al-Mashharawi\nGIS Engineer | Android App Developer\nPhone/WhatsApp: 0599876261\nEmail: mmashharawi2021@gmail.com"));
        about.addView(space(12));

        Button whatsappButton = softButton("تواصل واتساب");
        whatsappButton.setOnClickListener(v -> openUrl("https://wa.me/970599876261"));
        Button lockButton = softButton("إعداد رمز دخول سريع");
        lockButton.setOnClickListener(v -> setQuickLock());

        about.addView(whatsappButton);
        about.addView(space(10));
        about.addView(lockButton);
        return about;
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
        root.setPadding(dp(16), dp(16), dp(16), dp(28));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(BG);
        return root;
    }

    private ScrollView wrap(View view) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.addView(view);
        return scrollView;
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(14);
        box.setLayoutParams(params);
        box.setBackground(strokeBackground(CARD, BORDER, 22));
        return box;
    }

    private LinearLayout innerCard() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        box.setBackground(strokeBackground(CARD_SOFT, BORDER, 18));
        return box;
    }

    private TextView sectionLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(TEXT);
        tv.setTextSize(18);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, dp(6), 0, dp(8));
        return tv;
    }

    private TextView sectionTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(TEXT);
        tv.setTextSize(17);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private TextView helperText(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(MUTED);
        tv.setTextSize(13);
        return tv;
    }

    private EditText field(String hint, boolean password) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setHintTextColor(Color.parseColor("#8EA7B6"));
        editText.setTextColor(TEXT);
        editText.setTextSize(15);
        editText.setPadding(dp(14), dp(14), dp(14), dp(14));
        editText.setBackground(strokeBackground(CARD_SOFT, BORDER, 16));
        if (password) {
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
        return editText;
    }

    private Button primaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(TEXT);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setPadding(dp(16), dp(14), dp(16), dp(14));
        button.setBackground(strokeBackground(TEAL, TEAL, 18));
        return button;
    }

    private Button softButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(TEXT);
        button.setTextSize(14);
        button.setBackground(strokeBackground(CARD_SOFT, BORDER, 16));
        return button;
    }

    private Button chipButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(TEXT);
        button.setTextSize(13);
        button.setBackground(strokeBackground(BLUE, BLUE, 100));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, dp(8), 0);
        button.setLayoutParams(params);
        return button;
    }

    private Button switchButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        return button;
    }

    private void styleSwitch(Button button, boolean selected) {
        if (button == null) return;
        button.setTextColor(TEXT);
        button.setBackground(strokeBackground(selected ? BLUE : CARD_SOFT, selected ? BLUE : BORDER, 18));
        button.animate().scaleX(selected ? 1.02f : 1f).scaleY(selected ? 1.02f : 1f).setDuration(160).start();
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(4), 0, dp(4), 0);
        return params;
    }

    private GradientDrawable strokeBackground(int fill, int stroke, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private View space(int dp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(dp)));
        return v;
    }

    private String clean(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
