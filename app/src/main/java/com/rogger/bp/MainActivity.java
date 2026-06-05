package com.rogger.bp;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.rogger.bp.data.image.notification.ImageSyncScheduler;
import com.rogger.bp.databinding.ActivityMainBinding;
import com.rogger.bp.notification.NotificationScheduler;
import com.rogger.bp.notification.NotificationUtil;
import com.rogger.bp.ui.animation.GradientAnimator;
import com.rogger.bp.ui.base.BaseActivity;
import com.rogger.bp.ui.commun.SharedPreferencesManager;
import com.rogger.bp.ui.login.view.LoginActivity;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends BaseActivity {
    private AppBarConfiguration mAppBarConfiguration;
    private FirebaseAuth mAuth;
    private ActivityMainBinding binding;
    private ImageView imgProfile;
    private TextView txtProfileName;
    private TextView txtProfileEmail;
    private GradientAnimator animator;

    private ListenerRegistration listenerHome;
    private ListenerRegistration listenerCategory;
    private ListenerRegistration listenerDeleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ✅ Inicia a sincronização em segundo plano de imagens salvas em modo offline
        ImageSyncScheduler.INSTANCE.start(this);

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
        // 🔥 Correção para Android 11: Desativa aceleração de hardware para evitar erro de Canvas muito grande
        imgProfile.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        txtProfileName = viewProfile.findViewById(R.id.name_profile_navigation);
        txtProfileEmail = viewProfile.findViewById(R.id.txt_profile_email_navigation);

        View bgView = viewProfile.findViewById(R.id.header_background);
        // 🔥 Correção para Android 11: Desativa aceleração de hardware no fundo do header
        if (bgView != null) {
            bgView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        // 🔥 iniciar animação
        animator = new GradientAnimator(bgView);
        animator.start();

        List<String> userInfo = SharedPreferencesManager.getUserInfo(this);
        String profileUri = userInfo.get(2);
        if (profileUri != null) {
            txtProfileName.setText(userInfo.get(1));
            txtProfileEmail.setText(userInfo.get(3));
            Glide.with(this)
                    .asBitmap()
                    .load(profileUri)
                    .override(128, 128)
                    .format(com.bumptech.glide.load.DecodeFormat.PREFER_RGB_565) // Economiza 50% de memória
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_person_24)
                    .circleCrop()
                    .into(imgProfile);
        }
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_item_deleted_fragment,
                R.id.nav_category,
                R.id.nav_profile)
                .setOpenableLayout(drawer)
                .build();
        AtomicReference<NavController> navController = new AtomicReference<>(Navigation.findNavController(this, R.id.nav_host_fragment_content_main));
        NavigationUI.setupActionBarWithNavController(this, navController.get(), mAppBarConfiguration);

        // Configuração personalizada para limpar filtros ao clicar em "Início" no Drawer
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // 👉 Força a navegação limpando os argumentos (filtros) e reiniciando a Home de forma limpa
                navController.get().navigate(R.id.nav_home, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_home, true) // Remove instâncias antigas e reconstrói
                        .build());
            } else {
                // Comportamento normal para os outros destinos do Drawer
                NavigationUI.onNavDestinationSelected(item, navController.get());
            }
            drawer.closeDrawers();
            return true;
        });

        // ✅ Inicia o agendamento de notificações de validade
        NotificationScheduler.start(MainActivity.this);

        // 📊 Atualizar contadores no menu lateral (Navigation Drawer)
        setupNavigationDrawerCounters(navigationView);

        binding.txtPolitica.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://roggerlv52.github.io/bipando/"));
            startActivity(intent);
        });
    }

    private void setupNavigationDrawerCounters(NavigationView navigationView) {

        Menu menu = navigationView.getMenu();
        MenuItem homeItem = menu.findItem(R.id.nav_home);
        MenuItem categoryItem = menu.findItem(R.id.nav_category);
        MenuItem deletedItem = menu.findItem(R.id.nav_item_deleted_fragment);

        String uid = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid()
                : null;

        if (uid == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // ── 1. Produtos activos na Home ───────────────────────────────────
        listenerHome = db.collection("users")
                .document(uid)
                .collection("products")
                .whereEqualTo("deleted", false)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;
                    int count = snapshot.size();
                    runOnUiThread(() -> applyBadge(navigationView, homeItem, count));
                });

        // ── 2. Categorias ─────────────────────────────────────────────────
        listenerCategory = db.collection("users")
                .document(uid)
                .collection("category")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;
                    int count = snapshot.size();
                    runOnUiThread(() -> applyBadge(navigationView, categoryItem, count));
                });

        // ── 3. Produtos deletados ─────────────────────────────────────────
        listenerDeleted = db.collection("users")
                .document(uid)
                .collection("products")
                .whereEqualTo("deleted", true)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;
                    int count = snapshot.size();
                    runOnUiThread(() -> applyBadge(navigationView, deletedItem, count));
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

        }
        return super.onOptionsItemSelected(item);
    }

    private void applyBadge(NavigationView navigationView, MenuItem item, int count) {
        if (item == null) return;

        View actionView = item.getActionView();
        if (actionView == null) {
            actionView = LayoutInflater.from(this)
                    .inflate(R.layout.nav_drawer_badge, navigationView, false);
            item.setActionView(actionView);
        }

        TextView badge = actionView.findViewById(R.id.nav_badge_text);
        if (badge == null) return;

        if (count <= 0) {
            badge.setVisibility(View.GONE);
        } else {
            badge.setText(count > 99 ? "99+" : String.valueOf(count));
            badge.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerHome != null) {
            listenerHome.remove();
            listenerHome = null;
        }
        if (listenerCategory != null) {
            listenerCategory.remove();
            listenerCategory = null;
        }
        if (listenerDeleted != null) {
            listenerDeleted.remove();
            listenerDeleted = null;
        }
    }
}