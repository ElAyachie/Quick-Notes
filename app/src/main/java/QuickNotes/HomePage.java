package QuickNotes;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.android.material.tabs.TabLayout;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.util.Objects;

import QuickNotes.Adapters.SectionsPageAdapter;

public class HomePage extends AppCompatActivity {
    SectionsPageAdapter mSectionsPageAdapter;
    ViewPager viewPager;
    TabLayout tabLayout;
    SharedPreferences pref;
    Boolean nightTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Check permissions.
        checkIfAllowed(this);
        // Checking for correct theme.
        pref = getSharedPreferences("MyPref", 0);
        if (pref.getBoolean("NIGHT MODE", false)) {
            setTheme(R.style.DarkTheme);
        } else if (!pref.getBoolean("NIGHT MODE", false)) {
            setTheme(R.style.AppTheme);
        }
        nightTheme = pref.getBoolean("NIGHT MODE", false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_home_page);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mSectionsPageAdapter = new SectionsPageAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        viewPager = findViewById(R.id.container);
        setupViewPager(viewPager);
        tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void setupViewPager(final ViewPager viewPager) {
        // Attach the fragments in the order that they will be seen
        SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager());
        adapter.addFragment(new Tab1Fragment(), "Collection");
        adapter.addFragment(new Tab2Fragment(), "Make Note");

        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                hideKeyboardFrom(getBaseContext(), viewPager);
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void hideKeyboardFrom(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        assert imm != null;
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsPage.class);
            startActivity(intent);
        }
        if (id == R.id.action_reminder_page) {
            Intent intent = new Intent(this, RemindersPage.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    public void onResume() {
        if (nightTheme != pref.getBoolean("NIGHT MODE", false)) {
            recreate();
        }
        super.onResume();
    }

    public void checkIfAllowed(Context context) {
        if (!(ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED)) {
            Activity activity = this;
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
    }
}

