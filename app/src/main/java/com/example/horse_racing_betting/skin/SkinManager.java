package com.example.horse_racing_betting.skin;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;

import com.example.horse_racing_betting.R;

public class SkinManager {
    public enum Skin { DEFAULT, BLUE, RED }

    private static final String PREFS_NAME = "SkinPrefs";
    private static final String KEY_SKIN = "skin";

    private static SkinManager instance;
    private final Context appContext;
    private final SharedPreferences prefs;
    private Skin currentSkin;

    private SkinManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int saved = prefs.getInt(KEY_SKIN, 0);
        this.currentSkin = Skin.values()[Math.max(0, Math.min(saved, Skin.values().length - 1))];
    }

    public static synchronized SkinManager getInstance(Context context) {
        if (instance == null) instance = new SkinManager(context);
        return instance;
    }

    public Skin getCurrentSkin() { return currentSkin; }

    public void setCurrentSkin(Skin skin) {
        this.currentSkin = skin;
        prefs.edit().putInt(KEY_SKIN, skin.ordinal()).apply();
    }

    public int getHorseIconRes(@IntRange(from=1,to=4) int horseNumber) {
        switch (horseNumber) {
            case 1: return R.drawable.ic_horse_1;
            case 2: return R.drawable.ic_horse_2;
            case 3: return R.drawable.ic_horse_3;
            case 4: return R.drawable.ic_horse_4;
            default: return R.drawable.ic_horse_1;
        }
    }

    public void applyHorseIcon(ImageView view, @IntRange(from=1,to=4) int horseNumber) {
        view.setImageResource(getHorseIconRes(horseNumber));
        Integer tint = getTintForCurrentSkin();
        if (tint != null) {
            view.setColorFilter(tint, PorterDuff.Mode.SRC_IN);
        } else {
            view.clearColorFilter();
        }
    }

    private Integer getTintForCurrentSkin() {
        switch (currentSkin) {
            case BLUE: return 0xFF1976D2; // blue
            case RED: return 0xFFD32F2F;  // red
            case DEFAULT:
            default:
                return null; // no tint
        }
    }
}
