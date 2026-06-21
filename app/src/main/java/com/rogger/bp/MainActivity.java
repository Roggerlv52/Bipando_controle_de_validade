package com.rogger.bp;


import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.rogger.bp.data.database.BpDatabase;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        if (binding == null) return; // Trava de segurança

        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout());

            // Ajusta apenas o preenchimento superior da AppBar para empurrar a Toolbar para baixo
            if (binding.appBarMain.appBarLayout != null) {
                binding.appBarMain.appBarLayout.setPadding(0, systemBars.top, 0, 0);
            }
            return insets;
        });

        ImageSyncScheduler.INSTANCE.start(this);

        NotificationUtil.createChannel(this);

        mAuth = FirebaseAuth.getInstance();

        // ── ✅ CORREÇÃO DE SEGURANÇA SÊNIOR (Validação de Sessão Real) ──
        // Se o Firebase diz que o usuário não está autenticado, limpa qualquer lixo local
        // e força o redirecionamento imediato para a LoginActivity
        if (mAuth.getCurrentUser() == null) {
            SharedPreferencesManager.setLoginState(this, "state", false);
            SharedPreferencesManager.clearUserInfo(this);

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return; // Interrompe o resto do onCreate para evitar loops ou NullPointerExceptions
        }

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        View viewProfile = navigationView.getHeaderView(0);

        int originalTopPadding = viewProfile.getHeight();
        ViewCompat.setOnApplyWindowInsetsListener(viewProfile, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), originalTopPadding + systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
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
        BpDatabase db = BpDatabase.Companion.getDatabase(this);
        // ── ✅ ATUALIZAÇÃO: Escuta reativa do Limite de Produtos Gratuito ──
        TextView txtNavHeaderLimite = viewProfile.findViewById(R.id.txt_nav_header_limite);
        ProgressBar progressNavHeaderLimite = viewProfile.findViewById(R.id.progress_nav_header_limite);
        if (txtNavHeaderLimite != null) {
            db.productDao().getTotalProductsCountLiveData().observe(this, count -> {
                int total = count != null ? count : 0;
                boolean isPremium = SharedPreferencesManager.isPremium(this);

                if (isPremium) {
                    // 1. Caso Premium: Exibe o informativo verde e esconde a barra de progresso
                    txtNavHeaderLimite.setText(getString(R.string.premium_active_status, total));
                    txtNavHeaderLimite.setTextColor(android.graphics.Color.parseColor("#4CAF50")); // Verde limpo

                    if (progressNavHeaderLimite != null) {
                        progressNavHeaderLimite.setVisibility(android.view.View.GONE);
                    }
                } else {
                    // 2. Caso Plano Gratuito: Exibe a contagem fracionada e a barra de limite
                    txtNavHeaderLimite.setText(getString(R.string.free_limit_status, total));
                    txtNavHeaderLimite.setTextColor(android.graphics.Color.WHITE); // Branco

                    if (progressNavHeaderLimite != null) {
                        progressNavHeaderLimite.setVisibility(android.view.View.VISIBLE);
                        progressNavHeaderLimite.setMax(100);

                        // Garante que o progresso não ultrapassa visualmente os 100% da barra
                        progressNavHeaderLimite.setProgress(Math.min(total, 100));

                        if (total >= 100) {
                            // Limite atingido: Pinta a barra de Vermelho
                            progressNavHeaderLimite.setProgressTintList(
                                    android.content.res.ColorStateList.valueOf(android.graphics.Color.RED)
                            );
                        } else {
                            // Limite disponível: Pinta a barra de Verde Lima (conforme a imagem)
                            progressNavHeaderLimite.setProgressTintList(
                                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#8BC34A"))
                            );
                        }
                    }
                }
            });
        }

        List<String> userInfo = SharedPreferencesManager.getUserInfo(this);
        String profileUri = userInfo.get(2);
        if (profileUri != null) {
            txtProfileName.setText(userInfo.get(1));
            txtProfileEmail.setText(userInfo.get(3));
            Glide.with(this)
                    .asBitmap()
                    .load(profileUri)
                    .override(128, 128)
                    .format(DecodeFormat.PREFER_RGB_565) // Economiza 50% de memória
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

                navController.get().navigate(R.id.nav_home,null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_home, true)
                        .build());
            } else if (id == R.id.share_app) { // 👉 Adicionada a ação de compartilhar
                compartilharAplicativo();
            } else {
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

        // 👉 Obtém o banco de dados Room local
        BpDatabase db = BpDatabase.Companion.getDatabase(this);

        // ── 1. Contagem reativa de Produtos Ativos (Home) ──
        db.productDao().getActiveProductsCountLiveData().observe(this, count -> {
            applyBadge(navigationView, homeItem, count != null ? count : 0);
        });

        // ── 2. Contagem reativa de Categorias ──
        db.categoryDao().getCategoriesCountLiveData().observe(this, count -> {
            applyBadge(navigationView, categoryItem, count != null ? count : 0);
        });

        // ── 3. Contagem reativa de Produtos Deletados (Lixeira) ──
        db.productDao().getDeletedProductsCountLiveData(true).observe(this, count -> {
            applyBadge(navigationView, deletedItem, count != null ? count : 0);
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

    private void compartilharAplicativo() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        String mensagem = getString(R.string.share_app_message, getPackageName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, mensagem);

        shareIntent.putExtra(Intent.EXTRA_TEXT, mensagem);

        // Cria o seletor amigável de aplicativos
        Intent chooser = Intent.createChooser(shareIntent, getString(R.string.share_app_title));
        try {
            startActivity(chooser);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, getString(R.string.share_app_no_app_found), Toast.LENGTH_SHORT).show();
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
    }
}