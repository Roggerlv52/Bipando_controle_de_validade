package com.rogger.bipando;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.rogger.bipando.databinding.ActivityMainBinding;
import com.rogger.bipando.ui.base.BaseActivity;
import com.rogger.bipando.ui.commun.ImageCallback;
import com.rogger.bipando.ui.commun.SharedPreferencesManager;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.util.List;

public class MainActivity extends BaseActivity {
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private ImageView imgProfile;
    private TextView txtProfileName;
    private TextView txtProfileEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        /*
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .setAnchorView(R.id.fab).show();
            }
        });
       
         */
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.

        View viewProfile = navigationView.getHeaderView(0);
        imgProfile = viewProfile.findViewById(R.id.img_profile_navigation);
        txtProfileName = viewProfile.findViewById(R.id.name_profile_navigarion);
        txtProfileEmail = viewProfile.findViewById(R.id.txt_profile_email_navigation);
        FirebaseUser use = mAuth.getCurrentUser();
        if (use != null) {
            List<String> userInfo = SharedPreferencesManager.getUserInfo(this);
            String profileUri = userInfo.get(1);
            if (profileUri != null) {
                txtProfileName.setText(userInfo.get(0));
                Picasso.get()
                        .load(profileUri)
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .into(imgProfile);
            } else {
                Uri photoUrl = use.getPhotoUrl();
                String n = use.getDisplayName();
                txtProfileName.setText(n);
                Picasso.get()
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .into(imgProfile);
                assert photoUrl != null && n != null;
                SharedPreferencesManager.saveUserInfo(this,n, photoUrl.toString());
            }

        }
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_deleted)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_logout) {
            mAuth.signOut();
            SharedPreferencesManager.setloginState(this, "state", false);
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

}