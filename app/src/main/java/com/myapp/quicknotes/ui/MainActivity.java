package com.myapp.quicknotes.ui;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.MenuProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.myapp.quicknotes.R;
import com.myapp.quicknotes.databinding.ActivityMainBinding;

// The app's only activity. It owns the toolbar; every screen is a fragment in the navigation graph.
public class MainActivity extends AppCompatActivity {
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // The app bar is dark red in both themes, so the status bar icons stay light.
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        keepClearOfSystemBars(binding);

        setSupportActionBar(binding.toolbar);
        Drawable overflowIcon = binding.toolbar.getOverflowIcon();
        if (overflowIcon != null) {
            overflowIcon.setTint(ContextCompat.getColor(this, R.color.on_app_bar));
        }

        NavHostFragment navHost = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host);
        navController = navHost.getNavController();
        NavigationUI.setupActionBarWithNavController(this, navController,
                new AppBarConfiguration.Builder(R.id.homeFragment).build());

        addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                inflater.inflate(R.menu.main, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                // The menu item ids are the ids of the screens they open.
                return NavigationUI.onNavDestinationSelected(item, navController);
            }
        });
    }

    // The toolbar's back arrow behaves like the system back button, so a screen that intercepts
    // back (the editor asking about unsaved changes) is asked either way.
    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    // The window draws behind the status and navigation bars: the app bar grows to sit under the
    // status bar, and the content stops above the navigation bar or the keyboard.
    private void keepClearOfSystemBars(ActivityMainBinding binding) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
            binding.appBar.setPadding(bars.left, bars.top, bars.right, 0);
            binding.navHost.setPadding(bars.left, 0, bars.right,
                    Math.max(bars.bottom, keyboard.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
