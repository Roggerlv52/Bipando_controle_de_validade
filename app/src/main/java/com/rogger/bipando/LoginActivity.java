package com.rogger.bipando;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.rogger.bipando.ui.base.BaseActivity;
import com.rogger.bipando.ui.commun.SharedPreferencesManager;

public class LoginActivity extends BaseActivity {
    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private GoogleSignInClient mGoogleSignInClient;
    private ImageView imgshow;
    private int currentIndex = 0;
    private final int[] imageArray = {
        R.drawable.picture_2, R.drawable.picture_3, R.drawable.picture_4, R.drawable.picture_1
    };

    private static final int DELAY = 2000;
	private final Handler handler = new Handler();
	private final Runnable runnable = new Runnable() {
		public void run() {
			showNextImage();
			handler.postDelayed(this, DELAY);
		}
	};

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        boolean loginSate = SharedPreferencesManager.getloginState(this,"state");
        setContentView(R.layout.activity_login);
        if(loginSate){
           openMainActivity();
        }
        progressBar = findViewById(R.id.progressbar_login);
        // Inicializa o Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // Configura Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // ID do cliente OAuth 2.0
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        Button btn_face = findViewById(R.id.login_with_face);
        Button btn_gmail = findViewById(R.id.login_with_gmail);
        imgshow = findViewById(R.id.img_activity_login);
        Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        btn_gmail.setOnClickListener(
                v -> {
                  signIn();
                 SharedPreferencesManager.setloginState(this,"state",true);
                });
        btn_face.setOnClickListener(v ->{
            Snackbar.make(v, "Login com facebook não imprementado", Snackbar.LENGTH_LONG)
                    .setAction("Action", null)
                    .setTextColor(Color.WHITE)
                    .show();
        });
        startSlideshow();
    }
    private void startSlideshow() {
		handler.postDelayed(runnable, DELAY);
	}
    private void showNextImage() {

		imgshow.animate().alpha(0f).setDuration(500).setListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				if (currentIndex == imageArray.length) {
					currentIndex = 0;
				}
				imgshow.setImageResource(imageArray[currentIndex]);
				currentIndex++;
				imgshow.animate().alpha(1f).setDuration(500).setListener(null);
			}
		});

	}
    private void signIn() {
        progressBar.setVisibility(View.VISIBLE);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w("LoginActivity", "Google sign in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d("LoginActivity", "signInWithCredential:success, User: " + user.getDisplayName());
                        progressBar.setVisibility(View.VISIBLE);
                        openMainActivity();
                    } else {
                        Log.w("LoginActivity", "signInWithCredential:failure", task.getException());
                        Toast.makeText(this,R.string.error_login_gmail,Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void openMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TODO: Implement this method
        handler.removeCallbacks(runnable);
    }

}

