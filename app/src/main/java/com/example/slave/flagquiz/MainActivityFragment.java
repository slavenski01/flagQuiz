package com.example.slave.flagquiz;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


import java.io.IOException;

import java.io.InputStream;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


public class MainActivityFragment extends Fragment {

    //Строка, используемая при регистрации сообщений об ошибках
        private static final String TAG = "FlagQuiz Activity";
        private static final int FLAGS_IN_QUIZ = 10;
        private List<String> fileNameList; //Имена файлов с флагами
        private List<String> quizCountriesList; //Страны текущей викторины
        private Set<String> regionSet; //Регионы текущей викторины
        private String correctAnswer; //Правильная страна для текущего флага
        private int totalGuesses; //Количество попыток
        private int correctAnswers; //Количество правильных ответов
        private int guessRows; //Количество строк с кнопками вариантов
        private SecureRandom random; //Генератор случайных чисел
        private Handler handler; //Для задержки загрузки следующего флага
        private Animation shakeAnimation; //Анимация неправильного ответа

        private LinearLayout quizLinearLayout; //Макет с викториной


        private TextView questionNumberTextView; //Номер текущего вопроса
        private ImageView flagImageView; //Для вывода флага
        private LinearLayout[] guessLinearLayouts; //Строки с кнопками
        private TextView answerTextView; //Для правильного ответа


        // Настройка MainActivityFragment при создании представления
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);
            View view = inflater.inflate(R.layout.fragment_main, container, false);

            fileNameList = new ArrayList<>(); // Оператор <>
            //String[] fileNameArray =
            quizCountriesList = new ArrayList<>();
            random = new SecureRandom();
            handler = new Handler();

            // Загрузка анимации для неправильных ответов
            shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.incorrect_shake);
            shakeAnimation.setRepeatCount(3); // Анимация повторяется 3 раза

            // Получение ссылок на компоненты графического интерфейса
            quizLinearLayout = (LinearLayout) view.findViewById(R.id.quizLinearLayout);
            questionNumberTextView = (TextView) view.findViewById(R.id.questionNumberTextView);
            flagImageView = (ImageView) view.findViewById(R.id.flagImageView);
            guessLinearLayouts = new LinearLayout[4];
            guessLinearLayouts[0] = (LinearLayout) view.findViewById(R.id.row1LinearLayout);
            guessLinearLayouts[1] = (LinearLayout) view.findViewById(R.id.row2LinearLayout);
            guessLinearLayouts[2] = (LinearLayout) view.findViewById(R.id.row3LinearLayout);
            guessLinearLayouts[3] = (LinearLayout) view.findViewById(R.id.row4LinearLayout);
            answerTextView = (TextView) view.findViewById(R.id.answerTextView);

            // Настройка слушателей для кнопок ответов
            for (LinearLayout row : guessLinearLayouts) {
                for (int column = 0; column < row.getChildCount(); column++) {
                        Button button = (Button) row.getChildAt(column);
                        button.setOnClickListener(guessButtonListener);
                    }
                }

            // Назначение текста questionNumberTextView
            questionNumberTextView.setText(getString(R.string.question, 1, FLAGS_IN_QUIZ));
            return view; // Возвращает представление фрагмента для вывода
        }

        //Обновление guessRows на основании значения SharedPreferences
        public void updateGuessRows(SharedPreferences sharedPreferences) {
            guessRows = Integer.parseInt(sharedPreferences.getString(MainActivity.CHOICES,null)) / 2;//Получаем значение из файла preferences.xml по ключу CHOISES


            //Все активности затухают и не занимают место
            for(LinearLayout layout:guessLinearLayouts){
                layout.setVisibility(View.GONE);
            }

            //Активности в количестве guessRows становятся видимыми
            for(int row = 0; row < guessRows; row++){
                guessLinearLayouts[row].setVisibility(View.VISIBLE);
            }
        }

        // Обновление выбранных регионов по данным из SharedPreferences
        public void updateRegions(SharedPreferences sharedPreferences) {
            regionSet = sharedPreferences.getStringSet(MainActivity.REGIONS, null);//Получаем значение из файла preferences.xml по ключу REGIONS
        }

        //Настройка и запуск следующей серии вопросов
        public void resetQuiz() {
            AssetManager assets = getActivity().getAssets();
            fileNameList.clear();

            // Использование AssetManager для получения имен файлов изображений
            try {
                for(String region:regionSet){
                    String[] paths = assets.list(region);
                    for(String path:regionSet){
                        fileNameList.add(path.replace(".png", ""));
                    }
                }
            }
            catch(IOException ex) {
               Log.e(TAG, "Error loading image file names", ex);
            }



            correctAnswers = 0;
            totalGuesses = 0;

            quizCountriesList.clear();

                int flagCounter = 1;
                int numberOfFlags = fileNameList.size();

                // Добавление FLAGS_IN_QUIZ случайных файлов в quizCountriesList
                while (flagCounter <= FLAGS_IN_QUIZ) {
                    int randomIndex = random.nextInt(numberOfFlags);

                    // Получение случайного имени файла
                    String filename = fileNameList.get(randomIndex);

                    //Если регион включен, но еще не был выбран
                    if (!quizCountriesList.contains(filename)) {
                        quizCountriesList.add(filename);

                    }
                    ++flagCounter;
                }

            loadNextFlag(); //Запустить викторину с загрузкой первого флага
        }

        // Загрузка следующего флага после правильного ответа
        private void loadNextFlag() {
            String nextImage = quizCountriesList.remove(0);
            correctAnswer = nextImage;
            answerTextView.setText("");

            questionNumberTextView.setText(getString(R.string.question, correctAnswers+1, FLAGS_IN_QUIZ));
            String region = nextImage.substring(0, nextImage.indexOf('-'));

            AssetManager assets = getActivity().getAssets();
            try {
                InputStream stream = assets.open(region + "/" + nextImage + ".png");
                Drawable flag = Drawable.createFromStream(stream, nextImage);
                flagImageView.setImageDrawable(flag);
                animate(false);
            }
            catch(IOException exception) {
                Log.e(TAG, "Error loading " + nextImage, exception);
            }

            Collections.shuffle(fileNameList);//Перестановка имен файлов

            //Помещение правильного ответа в конец fileNameList
            int correct = fileNameList.indexOf(correctAnswer);
            fileNameList.add(fileNameList.remove(correct));

            // Добавление 2, 4, 6 или 8 кнопок в зависимости от значения guessRows
            for (int row = 0; row < guessRows; row++) {
                // Размещение кнопок в currentTableRow
                for (int column = 0; column < guessLinearLayouts[row].getChildCount(); column++) {
                    // Получение ссылки на Button
                    Button newGuessButton =
                            (Button) guessLinearLayouts[row].getChildAt(column);
                    newGuessButton.setEnabled(true);

                    // Назначение названия страны текстом newGuessButton
                    String filename = fileNameList.get((row * 2) + column);
                    newGuessButton.setText(getCountryName(filename));
                }
            }

            // Случайная замена одной кнопки правильным ответом
            int row = random.nextInt(guessRows); //Выбор случайной строки
            int column = random.nextInt(2);
            LinearLayout randomRow = guessLinearLayouts[row];
            String countryName = getCountryName(correctAnswer);
        }

        // Метод разбирает имя файла с флагом и возвращает название страны
        private String getCountryName(String name) {
            return name.substring(name.indexOf('-') + 1).replace('_', ' ');
        }


        // Весь макет quizLinearLayout появляется или исчезает с экрана
        private void animate(boolean animateOut) {
            //Здесь должна быть анимация
        }

        // Вызывается при нажатии кнопки ответа
        private View.OnClickListener guessButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        };

        private void disableButtons() {
            for (int row = 0; row < guessRows; row++) {
                LinearLayout guessRow = guessLinearLayouts[row];
                for (int i = 0; i < guessRow.getChildCount(); i++)
                    guessRow.getChildAt(i).setEnabled(false);
                }
        }

    }
