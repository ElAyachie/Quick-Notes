package com.myapp.quicknotes.ui.common;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;

// Small helpers shared by the screens.
public final class Screens {
    private Screens() {
    }

    // Opens a screen, but only while `from` is still the one showing. A fast double tap would
    // otherwise open the same screen twice.
    public static void open(Fragment fragment, @IdRes int from, @IdRes int destination,
                            @Nullable Bundle args) {
        NavController navController = NavHostFragment.findNavController(fragment);
        NavDestination current = navController.getCurrentDestination();
        if (current != null && current.getId() == from) {
            navController.navigate(destination, args);
        }
    }

    public static void hideKeyboard(View view) {
        InputMethodManager keyboard = (InputMethodManager)
                view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        keyboard.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
