package com.rogger.bp.ui.profile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.rogger.bp.R;
import com.rogger.bp.databinding.FragmentProfileBinding;
import com.rogger.bp.notification.NotificationPrefs;
import com.rogger.bp.notification.NotificationScheduler;
import com.rogger.bp.ui.base.DialogUtil;
import com.rogger.bp.ui.commun.SharedPreferencesManager;
import com.rogger.bp.ui.login.view.LoginActivity;
import com.rogger.bp.ui.profile.data.DeleteAccountCallback;
import com.rogger.bp.ui.profile.data.ProfileRepository;

import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private ProfileRepository profileRepository;

    // Bug 6 corrigido: launcher para solicitar POST_NOTIFICATIONS em runtime (Android 13+)
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            // Permissão concedida: ativa e agenda
                            NotificationPrefs.onAlert(requireContext(), true);
                            binding.swNotification.setChecked(true);
                            NotificationScheduler.start(requireContext());
                        } else {
                            // Permissão negada: mantém switch desligado
                            binding.swNotification.setChecked(false);
                            NotificationPrefs.onAlert(requireContext(), false);
                        }
                    });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileRepository = new ProfileRepository();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle arg01) {
        binding = FragmentProfileBinding.inflate(inflater, viewGroup, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int themeNumber = SharedPreferencesManager.getThemeNumber(requireContext(), "chave");
        boolean stateBeep = SharedPreferencesManager.getBeepState(requireContext(), "beep");

        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        binding.swNotification.setChecked(NotificationPrefs.getAlert(requireContext()));
        binding.sliderYellow.setValue(NotificationPrefs.getDays(requireContext()));
        binding.txtYellow.setText((int) binding.sliderYellow.getValue() + " dias");
        updateTimeText();

        List<String> userInfo = SharedPreferencesManager.getUserInfo(requireContext());
        String name = userInfo.get(1);
        binding.userNameProfile.setText(name);
        binding.profileTitleName.setText(name);
        Glide.with(requireContext())
                .load(userInfo.get(2))
                .override(350, 350)
                .placeholder(R.drawable.ic_person_24)
                .error(R.drawable.ic_person_24)
                .into(binding.profileImage);

        binding.boxOffBeep.setChecked(stateBeep);
        switch (themeNumber) {
            case 1:
            case 0:
                binding.boxProfile1.setChecked(true);
                break;
            case 2:
                binding.boxProfile2.setChecked(true);
                break;
            case 3:
                binding.boxProfile3.setChecked(true);
                break;
            case 4:
                binding.boxProfile4.setChecked(true);
                break;
        }

        // Switch de notificação — com pedido de permissão no Android 13+
        binding.swNotification.setOnCheckedChangeListener((button, isChecked) -> {
            if (isChecked) {
                // Bug 6 corrigido: verifica e solicita permissão antes de ativar
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                        // Reverte o switch e solicita permissão
                        binding.swNotification.setChecked(false);
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                        return;
                    }
                }
                NotificationPrefs.onAlert(requireContext(), true);
                NotificationScheduler.start(requireContext());
            } else {
                NotificationPrefs.onAlert(requireContext(), false);
                NotificationScheduler.stop(requireContext());
            }
        });
        // Botão de horário
        binding.btnSetNotifTime.setOnClickListener(v -> showTimePickerDialog());

        // Bug 1 corrigido: slider salva E reagenda o worker com o novo limite de dias
        binding.sliderYellow.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                int dias = (int) value;
                binding.txtYellow.setText(dias + " dias");
                NotificationPrefs.saveDays(requireContext(), dias);

                // Reagenda para aplicar o novo valor de dias imediatamente
                if (NotificationPrefs.getAlert(requireContext())) {
                    NotificationScheduler.start(requireContext());
                }
            }
        });

        // Listeners de tema
        CompoundButton.OnCheckedChangeListener listener = this::onCheckedChanged;
        CompoundButton.OnCheckedChangeListener listenerBeep = this::onCheckedBeep;
        binding.boxProfile1.setOnCheckedChangeListener(listener);
        binding.boxProfile2.setOnCheckedChangeListener(listener);
        binding.boxProfile3.setOnCheckedChangeListener(listener);
        binding.boxProfile4.setOnCheckedChangeListener(listener);
        binding.boxOffBeep.setOnCheckedChangeListener(listenerBeep);

        binding.btnDeleteAccount.setOnClickListener(v -> {
            DialogUtil.showCustomDialog(requireContext(), getString(R.string.delete_account_confirme), () -> {
                showProgress(true);
                profileRepository.deleteUserAccount(new DeleteAccountCallback() {
                    @Override
                    public void onFailure(@NonNull String message) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        Log.e("ProfileFragment","Erro -> "+message);
                    }

                    @Override
                    public void onSuccess() {
                        Intent intent = new Intent(requireContext(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    }

                    @Override
                    public void onComplete() {
                        SharedPreferencesManager.setLoginState(requireContext(),"state",false);
                        showProgress(false);
                    }
                });
            });
        });
    }

    private void showProgress(boolean show) {
        binding.progressBarProfile.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnDeleteAccount.setEnabled(!show);
    }

    private void showTimePickerDialog() {
        int hour = NotificationPrefs.getHour(requireContext());
        int minute = NotificationPrefs.getMinute(requireContext());

        TimePickerDialog dialog = new TimePickerDialog(requireContext(),
                (timePicker, hourOfDay, minuteOfHour) -> {
                    NotificationPrefs.saveTime(requireContext(), hourOfDay, minuteOfHour);
                    updateTimeText();

                    // Reagenda com o novo horário se notificações estiverem ativas
                    if (NotificationPrefs.getAlert(requireContext())) {
                        NotificationScheduler.start(requireContext());
                    }
                }, hour, minute, true);
        dialog.show();
    }

    private void updateTimeText() {
        int hour = NotificationPrefs.getHour(requireContext());
        int minute = NotificationPrefs.getMinute(requireContext());
        binding.txtNotifTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
    }

    private void onCheckedBeep(CompoundButton compoundButton, boolean beep) {
        SharedPreferencesManager.sharedBeepState(requireContext(), "beep", beep);
    }

    private void uncheckOthers(@SuppressLint("UseSwitchCompatOrMaterialCode") SwitchMaterial selected) {
        if (selected != binding.boxProfile1) binding.boxProfile1.setChecked(false);
        if (selected != binding.boxProfile2) binding.boxProfile2.setChecked(false);
        if (selected != binding.boxProfile3) binding.boxProfile3.setChecked(false);
        if (selected != binding.boxProfile4) binding.boxProfile4.setChecked(false);
    }

    private void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (!isChecked) return;
        if (buttonView.getId() == R.id.box_profile_1) {
            SharedPreferencesManager.updateThemeNumber(requireContext(), "chave", 1);
            uncheckOthers(binding.boxProfile1);
            requireActivity().setTheme(R.style.Theme_Bpd);
            requireActivity().recreate();
        } else if (buttonView.getId() == R.id.box_profile_2) {
            SharedPreferencesManager.updateThemeNumber(requireContext(), "chave", 2);
            uncheckOthers(binding.boxProfile2);
            requireActivity().setTheme(R.style.Theme_Bpd_2);
            requireActivity().recreate();
        } else if (buttonView.getId() == R.id.box_profile_3) {
            SharedPreferencesManager.updateThemeNumber(requireContext(), "chave", 3);
            uncheckOthers(binding.boxProfile3);
            requireActivity().setTheme(R.style.Theme_Bpd_3);
            requireActivity().recreate();
        } else if (buttonView.getId() == R.id.box_profile_4) {
            SharedPreferencesManager.updateThemeNumber(requireContext(), "chave", 4);
            uncheckOthers(binding.boxProfile4);
            requireActivity().setTheme(R.style.Theme_Bpd_4);
            requireActivity().recreate();
        }
    }
}
