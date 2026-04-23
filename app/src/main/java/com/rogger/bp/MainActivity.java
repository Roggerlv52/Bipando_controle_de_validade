package com.rogger.bp;


import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.rogger.bp.data.database.FirebaseDataSource;
import com.rogger.bp.data.model.Categoria;
import com.rogger.bp.databinding.ActivityMainBinding;
import com.rogger.bp.notification.NotificationScheduler;
import com.rogger.bp.notification.NotificationUtil;
import com.rogger.bp.ui.animation.GradientAnimator;
import com.rogger.bp.ui.base.BaseActivity;
import com.rogger.bp.ui.base.Utils;
import com.rogger.bp.ui.commun.SharedPreferencesManager;
import com.rogger.bp.ui.viewmodel.CategoriaViewModel;
import com.rogger.bp.ui.viewmodel.DataViewModel;

import java.util.List;

public class MainActivity extends BaseActivity {
    private AppBarConfiguration mAppBarConfiguration;
    private FirebaseAuth mAuth;
    private ActivityMainBinding binding;
    private ImageView imgProfile;
    private TextView txtProfileName;
    private TextView txtProfileEmail;
    private FirebaseDataSource firebaseDataSource;
    private GradientAnimator animator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NotificationUtil.createChannel(this);
        mAuth = FirebaseAuth.getInstance();
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.

        View viewProfile = navigationView.getHeaderView(0);
        imgProfile = viewProfile.findViewById(R.id.img_profile_navigation);
        txtProfileName = viewProfile.findViewById(R.id.name_profile_navigation);
        txtProfileEmail = viewProfile.findViewById(R.id.txt_profile_email_navigation);

        View bgView = viewProfile.findViewById(R.id.header_background);
// 🔥 iniciar animação
        animator = new GradientAnimator(bgView);
        animator.start();

        firebaseDataSource = FirebaseDataSource.getInstance();

        List<String> userInfo = SharedPreferencesManager.getUserInfo(this);
        String profileUri = userInfo.get(2);
        if (profileUri != null) {
            txtProfileName.setText(userInfo.get(1));
            txtProfileEmail.setText(userInfo.get(3));
            Glide.with(this)
                    .load(profileUri)
                    .override(100, 100) // reduz tamanho
                    .placeholder(R.drawable.ic_launcher_foreground)   // enquanto carrega
                    .error(R.drawable.ic_person_24)         // se der erro
                    .circleCrop()                      // deixa redondo
                    .into(imgProfile);
        }
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_item_deleted_fragment, R.id.nav_category)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // ✅ Inicia o agendamento de notificações de validade
        NotificationScheduler.start(MainActivity.this);

        // 📊 Atualizar contadores no menu lateral (Navigation Drawer)
        setupNavigationDrawerCounters(navigationView);
    }

    private void setupNavigationDrawerCounters(NavigationView navigationView) {
        DataViewModel dataViewModel = new ViewModelProvider(this).get(DataViewModel.class);
        CategoriaViewModel categoriaViewModel = new ViewModelProvider(this).get(CategoriaViewModel.class);

        Menu menu = navigationView.getMenu();
        MenuItem homeItem = menu.findItem(R.id.nav_home);
        MenuItem categoryItem = menu.findItem(R.id.nav_category);
        MenuItem deletedItem = menu.findItem(R.id.nav_item_deleted_fragment);

        // Observar total de produtos ativos
        dataViewModel.getCountAtivos().observe(this, count -> {
            if (homeItem != null) {
                int total = count != null ? count : 0;
                homeItem.setTitle("Home (" + total + ")");
            }
        });

        // Observar total de categorias
        categoriaViewModel.getCountCategorias().observe(this, count -> {
            if (categoryItem != null) {
                int total = count != null ? count : 0;
                categoryItem.setTitle("Categorias (" + total + ")");
            }
        });

        // Observar total de itens deletados
        dataViewModel.getCountDeletados().observe(this, count -> {
            if (deletedItem != null) {
                int total = count != null ? count : 0;
                deletedItem.setTitle("Lixeira (" + total + ")");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_search) {
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_search);
            return true;
        }
        if (id == R.id.menu_logout) {
            mAuth.signOut();
            SharedPreferencesManager.setLoginState(this, "state", false);
            SharedPreferencesManager.clearUserInfo(this);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
        if (id == R.id.menu_add_category) {
            Utils.dialogCategory(this, "Nova categoria", (name) -> {
                CategoriaViewModel categoria = new ViewModelProvider(this)
                        .get(CategoriaViewModel.class);
                Categoria c = new Categoria();
                c.setNome(name);
                categoria.insert(c);
            });
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        firebaseDataSource.stopAllListeners();
        if (animator != null) {
            animator.stop();
        }
    }
}
