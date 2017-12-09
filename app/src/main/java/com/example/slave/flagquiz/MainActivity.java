package com.example.slave.flagquiz;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
    //Ключи для чтения данных из SharedPreferences
    public static final String CHOICES = "pref_numberOfChoices";
    public static final String REGIONS = "pref_regionsToInclude";

    private boolean phoneDevice = true; // Включение портретного режима
    private boolean preferencesChanged = true; // Настройки изменились?


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Задание по умолчанию в файле SharedPreferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        //Регистрация слушателя для изменений SharedPreferences
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(preferencesChangeListener);

        //Определения размера экрана
        int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

        //Для планшетного устройства phoneDevice присваивается false
        if(screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE || screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE){
            phoneDevice = false; //Не соответствует размерам телефона
        }

        //На телефоне разрешена только портретная ориентация
        if(phoneDevice)setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onStart() {
        super.onStart();
        MainActivityFragment quizFragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.quizFragment);
        if(preferencesChanged){
            //После задания настроек по умолчанию инициализировать MainFragment и запустить викторину

            quizFragment.updateGuessRows(PreferenceManager.getDefaultSharedPreferences(this));

            quizFragment.updateRegions(PreferenceManager.getDefaultSharedPreferences(this));

            quizFragment.resetQuiz();

            preferencesChanged = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Получение текущей ориентации устройства
        int orientation = getResources().getConfiguration().orientation;

        if(orientation == Configuration.ORIENTATION_PORTRAIT){
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        }
        else{
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
            Intent preferencesIntent = new Intent(this,SettingsActivity.class);
            startActivity(preferencesIntent);
        return super.onOptionsItemSelected(item);
    }

    private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            MainActivityFragment quizFragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.quizFragment);
            preferencesChanged  = true; //Пользователь изменил настройки
            //MainActivityFragment quizFragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.quizFragment);
            if (key.equals(CHOICES)) { // Изменилось число вариантов
                quizFragment.updateGuessRows(sharedPreferences);
                quizFragment.resetQuiz();
                }
            else if (key.equals(REGIONS)) { // Изменились регионы
                Set<String> regions =
                        sharedPreferences.getStringSet(REGIONS, null);

                if (regions != null && regions.size() > 0) {
                    quizFragment.updateRegions(sharedPreferences);
                    quizFragment.resetQuiz();
                    }
                else {
                    // Хотя бы один регион - по умолчанию Северная Америка
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    regions.add(getString(R.string.default_region));
                    editor.putStringSet(REGIONS, regions);
                    editor.apply();

                    Toast.makeText(MainActivity.this, R.string.default_region_message, Toast.LENGTH_SHORT).show();
                    }
                }

            Toast.makeText(MainActivity.this, R.string.restarting_quiz, Toast.LENGTH_SHORT).show();
        }

    };
}


