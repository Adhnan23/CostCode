package com.costcode.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String PREFS = "costcode";
    private static final String KEY_MAP = "map";

    private CodeMapper mapper;
    private boolean decodeMode = true;

    private Button modeDecode;
    private Button modeEncode;
    private EditText input;
    private TextView resultLabel;
    private TextView result;
    private TextView warning;
    private TextView tableMessage;
    private final EditText[] rows = new EditText[10];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        modeDecode = findViewById(R.id.modeDecode);
        modeEncode = findViewById(R.id.modeEncode);
        input = findViewById(R.id.input);
        resultLabel = findViewById(R.id.resultLabel);
        result = findViewById(R.id.result);
        warning = findViewById(R.id.warning);
        tableMessage = findViewById(R.id.tableMessage);

        mapper = new CodeMapper(loadMap());
        buildTable();

        modeDecode.setOnClickListener(v -> setMode(true));
        modeEncode.setOnClickListener(v -> setMode(false));

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { refresh(); }
        });

        findViewById(R.id.copy).setOnClickListener(v -> {
            String text = result.getText().toString();
            if (text.isEmpty()) return;
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("cost", text));
            Toast.makeText(this, "Copied " + text, Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.save).setOnClickListener(v -> saveTable());
        findViewById(R.id.reset).setOnClickListener(v -> {
            for (int d = 0; d < 10; d++) rows[d].setText(CodeMapper.DEFAULTS[d]);
            tableMessage.setText("Defaults loaded. Tap Save table to keep them.");
        });

        setMode(true);
    }

    private void setMode(boolean decode) {
        decodeMode = decode;
        modeDecode.setAlpha(decode ? 1f : 0.5f);
        modeEncode.setAlpha(decode ? 0.5f : 1f);
        input.setText("");
        if (decode) {
            input.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
            input.setHint("Type the code letters, e.g. REX");
            resultLabel.setText("Number");
        } else {
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setHint("Type a number, e.g. 123.50");
            resultLabel.setText("Code");
        }
        refresh();
    }

    private void refresh() {
        String text = input.getText().toString();
        CodeMapper.Result r = decodeMode ? mapper.decode(text) : mapper.encode(text);
        result.setText(r.text);
        warning.setText(r.problems);
        warning.setVisibility(r.problems.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void buildTable() {
        LinearLayout container = findViewById(R.id.tableContainer);
        container.removeAllViews();
        for (int d = 0; d < 10; d++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            TextView label = new TextView(this);
            label.setText(String.valueOf(d));
            label.setTextSize(22);
            label.setTypeface(Typeface.DEFAULT_BOLD);
            label.setGravity(Gravity.CENTER);
            row.addView(label, new LinearLayout.LayoutParams(dp(48), LinearLayout.LayoutParams.WRAP_CONTENT));

            EditText letters = new EditText(this);
            letters.setSingleLine(true);
            letters.setHint("letters for " + d);
            letters.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
            letters.setText(mapper.lettersFor(d).toUpperCase());
            row.addView(letters, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            rows[d] = letters;
            container.addView(row);
        }
    }

    private void saveTable() {
        String[] values = new String[10];
        for (int d = 0; d < 10; d++) values[d] = rows[d].getText().toString();
        CodeMapper candidate = new CodeMapper(values);
        String conflict = candidate.findConflict();
        if (conflict != null) {
            tableMessage.setText(conflict);
            return;
        }
        mapper = candidate;
        StringBuilder joined = new StringBuilder();
        for (int d = 0; d < 10; d++) {
            if (d > 0) joined.append(',');
            joined.append(mapper.lettersFor(d));
            rows[d].setText(mapper.lettersFor(d).toUpperCase());
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_MAP, joined.toString()).apply();
        tableMessage.setText("Saved.");
        refresh();
    }

    private String[] loadMap() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String saved = prefs.getString(KEY_MAP, null);
        if (saved == null) return CodeMapper.DEFAULTS;
        String[] parts = saved.split(",", -1);
        String[] out = new String[10];
        for (int d = 0; d < 10; d++) out[d] = d < parts.length ? parts[d] : "";
        return out;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
