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
import android.os.Handler;
import android.os.Looper;
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

    private static final int TEXT = Color.WHITE;
    private static final int MUTED = Color.parseColor("#C6D5E0");
    private static final int SOFT_TEXT = Color.parseColor("#9EB2C1");
    private static final int DARK = Color.parseColor("#07182A");

    private String wallet = "jawwal";
    private String pendingCode = "";
    private String draftPhone = "";
    private String draftAmount = "";
    private String draftPin = "";

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
        showSplashScreen();
    }

    private void showSplashScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackground(gradientBackground());

        LinearLayout splashCard = new LinearLayout(this);
        splashCard.setOrientation(LinearLayout.VERTICAL);
        splashCard.setGravity(Gravity.CENTER);
        splashCard.setPadding(dp(28), dp(30), dp(28), dp(30));
        splashCard.setBackground(strokeBackground(cardColor(), borderColor(), 28));

        TextView logoMark = new TextView(this);
        logoMark.setText("⇄");
        logoMark.setTextColor(primaryColor());
        logoMark.setTextSize(56);
        logoMark.setTypeface(Typeface.DEFAULT_BOLD);
        logoMark.setGravity(Gravity.CENTER);

        TextView appName = new TextView(this);
        appName.setText("تحويل سريع");
        appName.setTextColor(TEXT);
        appName.setTextSize(30);
        appName.setTypeface(Typeface.DEFAULT_BOLD);
        appName.setGravity(Gravity.CENTER);

        TextView owner = new TextView(this);
        owner.setText("مهند المشهراوي");
        owner.setTextColor(secondaryColor());
        owner.setTextSize(22);
        owner.setTypeface(Typeface.DEFAULT_BOLD);
        owner.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText("GIS Engineer | Android App Developer");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(13);
        subtitle.setGravity(Gravity.CENTER);

        TextView loading = new TextView(this);
        loading.setText("جاري التحميل...");
        loading.setTextColor(SOFT_TEXT);
        loading.setTextSize(14);
        loading.setGravity(Gravity.CENTER);

        splashCard.addView(logoMark);
        splashCard.addView(space(8));
        splashCard.addView(appName);
        splashCard.addView(space(6));
        splashCard.addView(owner);
        splashCard.addView(space(8));
        splashCard.addView(subtitle);
        splashCard.addView(space(16));
        splashCard.addView(loading);
        root.addView(splashCard);

        splashCard.setAlpha(0f);
        splashCard.setScaleX(0.94f);
        splashCard.setScaleY(0.94f);
        splashCard.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(550).start();
        setContentView(root);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!prefs.getString(KEY_LOCK, "").isEmpty()) {
                showLockScreen();
            } else {
                showMainScreen(draftPhone, draftAmount, draftPin);
            }
        }, 1500);
    }

    private void showLockScreen() {
        LinearLayout root = baseLayout();
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(headerCard());

        LinearLayout lockCard = card();
        lockCard.addView(sectionTitle("فتح التطبيق"));
        lockCard.addView(helperText("أدخل رمز الدخول السريع للمتابعة."));
        lockCard.addView(space(12));

        EditText input = field("رمز الدخول السريع", true);
        Button enter = primaryButton("دخول");
        enter.setOnClickListener(v -> {
            if (prefs.getString(KEY_LOCK, "").equals(input.getText().toString().trim())) {
                showMainScreen(draftPhone, draftAmount, draftPin);
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

    private void showMainScreen(String phoneValue, String amountValue, String pinValue) {
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

        renderForm(phoneValue, amountValue, pinValue);
        renderFavorites();
        renderHistory();
        setContentView(wrap(root));
    }

    private void switchWallet(String nextWallet) {
        if (phoneInput != null) draftPhone = phoneInput.getText().toString();
        if (amountInput != null) draftAmount = amountInput.getText().toString();
        if (pinInput != null) draftPin = pinInput.getText().toString();
        wallet = nextWallet;
        showMainScreen(draftPhone, draftAmount, draftPin);
    }

    private View headerCard() {
        LinearLayout header = card();
        header.setGravity(Gravity.CENTER_HORIZONTAL);
        header.setPadding(dp(22), dp(22), dp(22), dp(22));

        LinearLayout iconWrap = new LinearLayout(this);
        iconWrap.setGravity(Gravity.CENTER);
        iconWrap.setPadding(dp(18), dp(10), dp(18), dp(10));
        iconWrap.setBackground(strokeBackground(innerCardColor(), borderColor(), 22));

        TextView logo = new TextView(this);
        logo.setText("⇄");
        logo.setTextSize(38);
        logo.setTextColor(primaryColor());
        logo.setTypeface(Typeface.DEFAULT_BOLD);
        iconWrap.addView(logo);

        TextView title = new TextView(this);
        title.setText("تحويل سريع");
        title.setTextColor(TEXT);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText("اختر المحفظة، أدخل البيانات، راجعها، ثم نفّذ التحويل بأوضح شكل ممكن.");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);

        header.addView(iconWrap);
        header.addView(space(10));
        header.addView(title);
        header.addView(space(6));
        header.addView(subtitle);
        return header;
    }

    private View walletSwitcherCard() {
        LinearLayout box = card();
        box.addView(sectionTitle("اختر المحفظة"));
        box.addView(helperText("عند الضغط على المحفظة يتغير لون التطبيق بالكامل ليتناسق معها."));
        box.addView(space(12));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        jawwalButton = switchButton("جوال باي");
        palpayButton = switchButton("بال باي");

        jawwalButton.setOnClickListener(v -> switchWallet("jawwal"));
        palpayButton.setOnClickListener(v -> switchWallet("palpay"));

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

    private void renderForm(String phoneValue, String amountValue, String pinValue) {
        formContainer.removeAllViews();
        formContainer.setAlpha(0f);
        formContainer.setTranslationY(dp(18));

        formContainer.addView(sectionTitle("بيانات التحويل"));
        formContainer.addView(helperText("استغلال أفضل للمساحة، تقليل الزرار، وإظهار العناصر المهمة فقط."));
        formContainer.addView(space(14));

        if ("jawwal".equals(wallet)) {
            pinInput = field("PIN جوال باي", true);
            pinInput.setText(pinValue == null || pinValue.isEmpty() ? prefs.getString(KEY_PIN, "") : pinValue);
            savePinCheck = new CheckBox(this);
            savePinCheck.setText("حفظ PIN محليًا لهذا الجهاز");
            savePinCheck.setTextColor(MUTED);
            formContainer.addView(pinInput);
            formContainer.addView(space(8));
            formContainer.addView(savePinCheck);
            formContainer.addView(space(12));
        } else {
            pinInput = null;
            savePinCheck = null;
        }

        LinearLayout phoneRow = new LinearLayout(this);
        phoneRow.setOrientation(LinearLayout.HORIZONTAL);
        phoneInput = field("رقم المستلم", false);
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        phoneInput.setText(phoneValue == null ? "" : phoneValue);

        Button pickContactButton = iconButton("👤");
        pickContactButton.setOnClickListener(v -> pickContact());
        LinearLayout.LayoutParams phoneParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        phoneParams.setMargins(0, 0, dp(8), 0);
        phoneRow.addView(phoneInput, phoneParams);
        phoneRow.addView(pickContactButton, new LinearLayout.LayoutParams(dp(56), dp(56)));

        amountInput = field("المبلغ", false);
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        amountInput.setText(amountValue == null ? "" : amountValue);

        formContainer.addView(phoneRow);
        formContainer.addView(space(12));
        formContainer.addView(amountInput);
        formContainer.addView(space(12));
        formContainer.addView(quickAmountsCard());
        formContainer.addView(space(14));

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);

        Button addFavoriteButton = softButton("إضافة للمفضلة");
        addFavoriteButton.setOnClickListener(v -> addFavorite());

        Button confirmButton = primaryButton("تأكيد التحويل");
        confirmButton.setOnClickListener(v -> confirmTransfer());

        LinearLayout.LayoutParams small = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.9f);
        small.setMargins(0, 0, dp(8), 0);
        LinearLayout.LayoutParams large = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f);

        actionRow.addView(addFavoriteButton, small);
        actionRow.addView(confirmButton, large);
        formContainer.addView(actionRow);

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
        String pin = "jawwal".equals(wallet) && pinInput != null ? pinInput.getText().toString().trim() : "";

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
            line.setBackground(strokeBackground(innerCardColor(), borderColor(), 14));
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

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button whatsappButton = softButton("واتساب");
        whatsappButton.setOnClickListener(v -> openUrl("https://wa.me/970599876261"));
        Button lockButton = softButton("رمز الدخول");
        lockButton.setOnClickListener(v -> setQuickLock());
        row.addView(whatsappButton, weighted());
        row.addView(lockButton, weighted());
        about.addView(row);
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
        root.setBackground(gradientBackground());
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
        box.setBackground(strokeBackground(cardColor(), borderColor(), 22));
        return box;
    }

    private LinearLayout innerCard() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        box.setBackground(strokeBackground(innerCardColor(), borderColor(), 18));
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
        editText.setHintTextColor(SOFT_TEXT);
        editText.setTextColor(TEXT);
        editText.setTextSize(15);
        editText.setPadding(dp(14), dp(15), dp(14), dp(15));
        editText.setBackground(strokeBackground(innerCardColor(), borderColor(), 16));
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
        button.setBackground(gradientButton(primaryColor(), secondaryColor(), 18));
        return button;
    }

    private Button softButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(TEXT);
        button.setTextSize(14);
        button.setBackground(strokeBackground(innerCardColor(), borderColor(), 16));
        return button;
    }

    private Button iconButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(18);
        button.setTextColor(TEXT);
        button.setBackground(gradientButton(primaryColor(), secondaryColor(), 18));
        return button;
    }

    private Button chipButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(TEXT);
        button.setTextSize(13);
        button.setBackground(strokeBackground(primaryColor(), primaryColor(), 100));
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
        button.setBackground(selected
                ? gradientButton(primaryColor(), secondaryColor(), 18)
                : strokeBackground(innerCardColor(), borderColor(), 18));
        button.animate().scaleX(selected ? 1.03f : 1f).scaleY(selected ? 1.03f : 1f).setDuration(160).start();
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

    private GradientDrawable gradientButton(int start, int end, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{start, end});
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private GradientDrawable gradientBackground() {
        int start = "jawwal".equals(wallet) ? Color.parseColor("#071A16") : Color.parseColor("#0A1532");
        int end = DARK;
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{start, end});
    }

    private int primaryColor() {
        return "jawwal".equals(wallet) ? Color.parseColor("#16A34A") : Color.parseColor("#2563EB");
    }

    private int secondaryColor() {
        return "jawwal".equals(wallet) ? Color.parseColor("#14D4C8") : Color.parseColor("#F59E0B");
    }

    private int cardColor() {
        return "jawwal".equals(wallet) ? Color.parseColor("#11261F") : Color.parseColor("#14233F");
    }

    private int innerCardColor() {
        return "jawwal".equals(wallet) ? Color.parseColor("#16332A") : Color.parseColor("#1B2D4E");
    }

    private int borderColor() {
        return "jawwal".equals(wallet) ? Color.parseColor("#1E5A47") : Color.parseColor("#294E7B");
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
