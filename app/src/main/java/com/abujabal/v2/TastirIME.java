package com.abujabal.v2;

import android.content.Context;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class TastirIME extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {

    private KeyboardView kv;
    private Keyboard kbAr, kbEn, kbSym, kbEmoji;
    private SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private Runnable holdRunnable;
    private volatile boolean autoOn = false;
    private volatile boolean holdFired = false;
    private int lettersMode = 0;
    private int mode = 0;
    private final HashSet<String> usedSentences = new HashSet<>();
    private TextView indicator;

    @Override
    public View onCreateInputView() {
        prefs = getSharedPreferences("tastir", Context.MODE_PRIVATE);
        ensureWords();
        View v = getLayoutInflater().inflate(R.layout.keyboard_view, null);
        kv = (KeyboardView) v.findViewById(R.id.keyboard_view);
        indicator = (TextView) v.findViewById(R.id.typing_indicator);
        kbAr = new Keyboard(this, R.xml.qwerty_ar);
        kbEn = new Keyboard(this, R.xml.qwerty_en);
        kbSym = new Keyboard(this, R.xml.symbols);
        kbEmoji = new Keyboard(this, R.xml.emoji);
        lettersMode = prefs.getInt("letters_mode", 0);
        mode = lettersMode;
        kv.setKeyboard(lettersMode == 0 ? kbAr : kbEn);
        kv.setOnKeyboardActionListener(this);
        return v;
    }

    private void ensureWords() {
        if (!prefs.getString("words", "").trim().isEmpty()) return;
        try {
            java.io.InputStream is = getResources().openRawResource(R.raw.words);
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) sb.append(line.trim()).append("\n");
            }
            br.close();
            prefs.edit().putString("words", sb.toString().trim()).apply();
        } catch (Exception e) {
        }
    }

    private void setKb(Keyboard k, int m) {
        mode = m;
        if (kv != null) { kv.setKeyboard(k); kv.invalidate(); }
    }

    private void backToLetters() {
        setKb(lettersMode == 0 ? kbAr : kbEn, lettersMode);
    }

    private ArrayList<String> words() {
        ArrayList<String> list = new ArrayList<>();
        String w = prefs.getString("words", "");
        if (w != null) {
            for (String s : w.split("\n")) {
                if (!s.trim().isEmpty()) list.add(s.trim());
            }
        }
        return list;
    }

    private String separator() {
        if (!prefs.getBoolean("sep_enabled", true)) return " ";
        String sep = prefs.getString("separator", "~");
        if (sep == null || sep.isEmpty()) return " ";
        return sep;
    }

    private String randomWord() {
        ArrayList<String> l = words();
        if (l.isEmpty()) return "";
        return l.get(random.nextInt(l.size()));
    }

    private String makeSentence() {
        ArrayList<String> l = words();
        if (l.isEmpty()) return "";
        for (int attempt = 0; attempt < 40; attempt++) {
            int len = 2 + random.nextInt(3);
            StringBuilder sb = new StringBuilder();
            String prev = "";
            for (int i = 0; i < len; i++) {
                String w = l.get(random.nextInt(l.size()));
                if (w.equals(prev)) { i--; continue; }
                if (i > 0) sb.append(separator());
                sb.append(w);
                prev = w;
            }
            String out = sb.toString();
            if (!usedSentences.contains(out)) {
                usedSentences.add(out);
                if (usedSentences.size() > 3000) usedSentences.clear();
                return out;
            }
        }
        return "";
    }

    private char randomLetter() {
        ArrayList<String> l = words();
        if (l.isEmpty()) return ' ';
        String w = l.get(random.nextInt(l.size()));
        if (w.isEmpty()) return ' ';
        return w.charAt(random.nextInt(w.length()));
    }

    private void commit(String s) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(s, 1);
    }

    private void sleep(int ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private int delay() {
        int speed = prefs.getInt("type_speed", 200);
        if (speed < 1) speed = 1;
        if (speed > 500) speed = 500;
        return 501 - speed;
    }

    private void flashKey(final String ch) {
        handler.post(new Runnable() {
            public void run() {
                if (indicator == null) return;
                indicator.setText(ch);
                indicator.setVisibility(View.VISIBLE);
                if (kv != null) kv.performHapticFeedback(
                        android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                handler.postDelayed(new Runnable() {
                    public void run() { indicator.setVisibility(View.GONE); }
                }, Math.max(80, delay() / 2));
            }
        });
    }

    private void startAuto() {
        if (autoOn) return;
        EditorInfo info = getCurrentInputEditorInfo();
        if (info == null || info.packageName == null) return;
        autoOn = true;
        final String pkg = info.packageName;
        Toast.makeText(this, "Auto ON", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            public void run() {
                while (autoOn) {
                    EditorInfo ei = getCurrentInputEditorInfo();
                    if (ei == null || ei.packageName == null || !pkg.equals(ei.packageName)) {
                        autoOn = false;
                        break;
                    }
                    String s = makeSentence();
                    if (s.isEmpty()) { autoOn = false; break; }
                    for (int i = 0; i < s.length() && autoOn; i++) {
                        final String ch = String.valueOf(s.charAt(i));
                        handler.post(new Runnable() { public void run() { commit(ch); } });
                        flashKey(ch);
                        sleep(delay());
                    }
                    handler.post(new Runnable() { public void run() { commit(separator()); } });
                    sleep(delay());
                }
            }
        }).start();
    }

    private void stopAuto() {
        if (autoOn) {
            autoOn = false;
            Toast.makeText(this, "Auto OFF", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPress(int primaryCode) {
        if (primaryCode == 1587) {
            holdRunnable = new Runnable() {
                public void run() { holdFired = true; startAuto(); }
            };
            handler.postDelayed(holdRunnable, 2000);
        }
    }

    @Override
    public void onRelease(int primaryCode) {
        if (holdRunnable != null) {
            handler.removeCallbacks(holdRunnable);
            holdRunnable = null;
        }
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        if (primaryCode == 1587) {
            if (holdFired) { holdFired = false; return; }
            if (autoOn) { stopAuto(); return; }
            commit("س");
            return;
        }
        if (primaryCode == -104) {
            lettersMode = (lettersMode == 0) ? 1 : 0;
            prefs.edit().putInt("letters_mode", lettersMode).apply();
            backToLetters();
            return;
        }
        if (primaryCode == -105) {
            if (mode == 2) backToLetters();
            else setKb(kbSym, 2);
            return;
        }
        if (primaryCode == -108) { setKb(kbEmoji, 4); return; }
        if (primaryCode == -107) { backToLetters(); return; }
        if (primaryCode == -1 || primaryCode == Keyboard.KEYCODE_DELETE) {
            ic.deleteSurroundingText(1, 0);
            return;
        }
        if (primaryCode == 10) {
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
            return;
        }
        if (prefs.getBoolean("word_mode", false)) {
            String w = randomWord();
            if (!w.isEmpty()) commit(w + separator());
            return;
        }
        if (prefs.getBoolean("letter_mode", false)) {
            commit(String.valueOf(randomLetter()));
            return;
        }
        commit(String.valueOf((char) primaryCode));
    }

    @Override public void onFinishInput() { autoOn = false; super.onFinishInput(); }
    @Override public void onText(CharSequence text) { commit(text.toString()); }
    @Override public void swipeLeft() {}
    @Override public void swipeRight() {}
    @Override public void swipeUp() {}
    @Override public void swipeDown() { requestHideSelf(0); }
        }
