package com.example.horse_racing_betting;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.horse_racing_betting.fragment.StartFragment;
import com.example.horse_racing_betting.viewmodel.GameViewModel;

public class MainActivity extends AppCompatActivity {
    private GameViewModel gameViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gameViewModel = new ViewModelProvider(this).get(GameViewModel.class);

        if (savedInstanceState == null) {
            replaceFragment(new StartFragment());
        }
    }

    public void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    public GameViewModel getGameViewModel() {
        return gameViewModel;
    }
}