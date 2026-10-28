package com.abujabal.v2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private SharedPreferences prefs;
    private SeekBar sbSpeed;
    private TextView tvSpeed;
    private Spinner spSep;
    private Switch swSep, swWord, swLetter;
    private EditText etWords;
    private final String[] seps = {"~", "~+~", "~°~", "~_~", "~~", "~.~", "~!~", "+~+"};

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("tastir", Context.MODE_PRIVATE);
        setContentView(R.layout.activity_settings);

        sbSpeed = (SeekBar) findViewById(R.id.sb_speed);
        tvSpeed = (TextView) findViewById(R.id.tv_speed);
        spSep = (Spinner) findViewById(R.id.sp_sep);
        swSep = (Switch) findViewById(R.id.sw_sep);
        swWord = (Switch) findViewById(R.id.sw_word);
        swLetter = (Switch) findViewById(R.id.sw_letter);
        etWords = (EditText) findViewById(R.id.et_words);

        ArrayAdapter<String> ad = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, seps);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSep.setAdapter(ad);

        int speed = prefs.getInt("type_speed", 200);
        sbSpeed.setMax(499);
        sbSpeed.setProgress(speed - 1);
        tvSpeed.setText(String.valueOf(speed));
        sbSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                tvSpeed.setText(String.valueOf(p + 1));
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });

        String sep = prefs.getString("separator", "~");
        for (int i = 0; i < seps.length; i++) {
            if (seps[i].equals(sep)) spSep.setSelection(i);
        }
        swSep.setChecked(prefs.getBoolean("sep_enabled", true));
        swWord.setChecked(prefs.getBoolean("word_mode", false));
        swLetter.setChecked(prefs.getBoolean("letter_mode", false));
        etWords.setText(prefs.getString("words", ""));

        findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { saveAll(); }
        });
        findViewById(R.id.btn_enable).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            }
        });
        findViewById(R.id.btn_choose).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.showInputMethodPicker();
            }
        });
    }

    private void saveAll() {
        prefs.edit()
                .putInt("type_speed", sbSpeed.getProgress() + 1)
                .putString("separator", seps[spSep.getSelectedItemPosition()])
                .putBoolean("sep_enabled", swSep.isChecked())
                .putBoolean("word_mode", swWord.isChecked())
                .putBoolean("letter_mode", swLetter.isChecked())
                .putString("words", etWords.getText().toString())
                .apply();
        Toast.makeText(this, "Saved", Toast.LENGTH_LONG).show();
    }
  }
