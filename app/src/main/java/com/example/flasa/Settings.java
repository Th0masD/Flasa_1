package com.example.flasa;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;

import android.os.Bundle;

import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

public class Settings extends DeviceConnectActivity {


    private static final String PREFS_NAME = "MojeNastavenia";
    private static final String KEY_CISLO1 = "cislo1";
    private static final String KEY_CISLO2 = "cislo2";

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        Button btn1 = findViewById(R.id.tel_cislo);
        Button btn2 = findViewById(R.id.gps_nastavenie);

        btn1.setOnClickListener(v -> zobrazDialog(1));
        btn2.setOnClickListener(v -> zobrazDialog(2));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // zapne šípku späť
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // zavrie aktuálnu aktivitu
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void zobrazDialog(int typ) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Zadaj číslo");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);

        // Načítanie uloženého čísla
        String ulozene;
        if (typ == 1) {
            ulozene = preferences.getString(KEY_CISLO1, "");
        } else {
            ulozene = preferences.getString(KEY_CISLO2, "");
        }

        if (!ulozene.isEmpty()) {
            input.setText(ulozene);
        }

        builder.setView(input);

        builder.setPositiveButton("Uložiť", (dialog, which) -> {

            String text = input.getText().toString();


            if (!text.isEmpty()) {

                //   int cislo = Integer.parseInt(text);
                SharedPreferences.Editor editor = preferences.edit();

                if (typ == 1) {
                    editor.putString(KEY_CISLO1, text);
                    Toast.makeText(this, "Číslo 1 uložené: " + text, Toast.LENGTH_SHORT).show();
                } else {
                    editor.putString(KEY_CISLO2, text);
                    Toast.makeText(this, "Číslo 2 uložené: " + text, Toast.LENGTH_SHORT).show();
                }

                editor.apply();
            }
        });

        builder.setNegativeButton("Zrušiť", null);
        builder.show();
    }
}
