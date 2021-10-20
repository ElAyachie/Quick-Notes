package QuickNotes;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Switch;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SettingsPage extends AppCompatActivity {
    Switch toggleThemeSwitch;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        pref = getSharedPreferences("MyPref", 0);
        editor = pref.edit();
        if (pref.getBoolean("NIGHT MODE", false)) {
            setTheme(R.style.DarkTheme);
            editor.apply();
        }
        else if (! pref.getBoolean("NIGHT MODE", false)) {
            setTheme(R.style.AppTheme);
            editor.apply();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        editor = pref.edit();
        toggleThemeSwitch = findViewById(R.id.toggleThemeSwitch);
        if (pref.getBoolean("NIGHT MODE", false)){
            toggleThemeSwitch.setChecked(true);
        }
        toggleThemeSwitch.setOnCheckedChangeListener ((buttonView, isChecked) -> {
            editor.putBoolean("NIGHT MODE", isChecked);
            editor.apply();
            recreate();
        });
        }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
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
                return true;
            }
            if (id == R.id.action_reminder_page) {
                Intent intent = new Intent(this, RemindersPage.class);
                startActivity(intent);
            }

            return super.onOptionsItemSelected(item);
        }
}
