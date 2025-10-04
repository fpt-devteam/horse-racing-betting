package com.example.horse_racing_betting.skin;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.ImageView;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.horse_racing_betting.R;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class SkinManager {

    private static final String PREFS_NAME = "SkinPrefs";
    private static final String KEY_SKIN = "skin";

    private static volatile SkinManager instance;

    private final Context appContext;
    private final SharedPreferences prefs;

    private SkinManager(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static SkinManager getInstance(@NonNull Context context) {
        SkinManager local = instance;
        if (local == null) {
            synchronized (SkinManager.class) {
                local = instance;
                if (local == null) {
                    instance = local = new SkinManager(context);
                }
            }
        }
        return local;
    }

    public int getHorseIconRes(@IntRange(from = 1) int horseNumber) {
        switch (horseNumber) {
            case 1: return R.drawable.black_horse_tile00;
            case 2: return R.drawable.yellow_horse_tile00;
            case 3: return R.drawable.brown_horse_tile00;
            case 4: return R.drawable.white_horse_tile00;
            default: return R.drawable.ic_horse_1;
        }
    }

    public void applyHorseIcon(@Nullable ImageView view, @IntRange(from = 1) int horseNumber) {
        if (view == null) return;
        view.setImageResource(getHorseIconRes(horseNumber));
    }
}
