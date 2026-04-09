package com.rogger.bp.ui.profile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.rogger.bp.R;
import com.rogger.bp.notification.NotificationPrefs;
import com.rogger.bp.notification.NotificationScheduler;
import com.rogger.bp.ui.base.MenuUtil;
import com.rogger.bp.ui.commun.SharedPreferencesManager;

import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private SwitchMaterial box1, box2, box3, box4, boxBeep, swNotification;
    private TextView txtNotifTime;
    private View btnSetNotifTime;

    // Bug 6 corrigido: launcher para solicitar POST_NOTIFICATIONS em runtime (Android 13+)
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            // Permissão concedida: ativa e agenda
                            NotificationPrefs.onAlert(requireContext(), true);
                            swNotification.setChecked(true);
                            NotificationScheduler.start(requireContext());
                        } else {
                            // Permissão negada: mantém switch desligado
                            swNotification.setChecked(false);
                            NotificationPrefs.onAlert(requireContext(), false);
                        }
                    });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle arg01) {
        return inflater.inflate(R.layout.fragment_profile, viewGroup, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {


        int themeNumber = SharedPreferencesManager.getThemeNumber(requireContext(), "chave");
        boolean stateBeep = SharedPreferencesManager.getBeepState(requireContext(), "beep");

        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        box1           = view.findViewById(R.id.box_profile_1);
        box2           = view.findViewById(R.id.box_profile_2);
        box3           = view.findViewById(R.id.box_profile_3);
        box4           = view.findViewById(R.id.box_profile_4);
        boxBeep        = view.findViewById(R.id.box_off_beep);
        swNotification = view.findViewById(R.id.swNotification);
        btnSetNotifTime = view.findViewById(R.id.btnSetNotifTime);
        txtNotifTime   = view.findViewById(R.id.txtNotifTime);

        ImageView circleImageView = view.findViewById(R.id.profileImage);
        TextView txtName          = view.findViewById(R.id.userNameProfile);
        TextView txtTitle         = view.findViewById(R.id.profile_title_name);
        Slider sliderYellow       = view.findViewById(R.id.sliderYellow);
        TextView txtYellow        = view.findViewById(R.id.txtYellow);

        // Carrega valores salvos
        swNotification.setChecked(NotificationPrefs.getAlert(requireContext()));
        sliderYellow.setValue(NotificationPrefs.getDays(requireContext()));
        txtYellow.setText((int) sliderYellow.getValue() + " dias");
        updateTimeText();

        // Foto e nome do usuário
        List<String> userInfo = SharedPreferencesManager.getUserInfo(requireContext());
        String name = userInfo.get(1);
        txtName.setText(name);
        txtTitle.setText(name);
        Glide.with(requireContext())
                .load(userInfo.get(2))
                .override(350, 350)
                .placeholder(R.drawable.ic_person_24)
                .error(R.drawable.ic_person_24)
                .into(circleImageView);

        boxBeep.setChecked(stateBeep);
        switch (themeNumber) {
            case 1:
            case 0: box1.setChecked(true); break;
            case 2: box2.setChecked(true); break;
            case 3: box3.setChecked(true); break;
            case 4: box4.setChecked(true); break;
        }

        // Switch de notificação — com pedido de permissão no Android 13+
        swNotification.setOnCheckedChangeListener((button, isChecked) -> {
            if (isChecked) {
                // Bug 6 corrigido: verifica e solicita permissão antes de ativar
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                        // Reverte o switch e solicita permissão
                        swNotification.setChecked(false);
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
        btnSetNotifTime.setOnClickListener(v -> showTimePickerDialog());

        // Bug 1 corrigido: slider salva E reagenda o worker com o novo limite de dias
        sliderYellow.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                int dias = (int) value;
                txtYellow.setText(dias + " dias");
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
        box1.setOnCheckedChangeListener(listener);
        box2.setOnCheckedChangeListener(listener);
        box3.setOnCheckedChangeListener(listener);
        box4.setOnCheckedChangeListener(listener);
        boxBeep.setOnCheckedChangeListener(listenerBeep);

        super.onViewCreated(view, savedInstanceState);
    }

    private void showTimePickerDialog() {
        int hour   = NotificationPrefs.getHour(requireContext());
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
        int hour   = NotificationPrefs.getHour(requireContext());
        int minute = NotificationPrefs.getMinute(requireContext());
        txtNotifTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
    }

    private void onCheckedBeep(CompoundButton compoundButton, boolean beep) {
        SharedPreferencesManager.sharedBeepState(requireContext(), "beep", beep);
    }

    private void uncheckOthers(@SuppressLint("UseSwitchCompatOrMaterialCode") SwitchMaterial selected) {
        if (selected != box1) box1.setChecked(false);
        if (selected != box2) box2.setChecked(false);
        if (selected != box3) box3.setChecked(false);
        if (selected != box4) box4.setChecked(false);
    }

    private void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (!isChecked) return;
        if (buttonView.getId() == R.id.box_profile_1) {
            SharedPreferencesManager.updateThemeNumber(requireContext(), "chave", 1);
            uncheckOthers(box1);
            requireActivity().setTheme(R.style.Theme_Bpd);
            requireActivity().recreate();
        } else if (buttonView.getId() == R.id.box_profile_2) {
            SharedPreferencesManager.updateThemeNumber(requireContext(), "chave", 2);
            uncheckOthers(box2);
            requireActivity().setTheme(R.style.Theme_Bpd_2);
            requireActivity().recreate();
        } else if (buttonView.getId() == R.id.box_profile_3) {
            SharedPreferencesManager.updateThemeNumber(requireContext(), "chave", 3);
            uncheckOthers(box3);
            requireActivity().setTheme(R.style.Theme_Bpd_3);
            requireActivity().recreate();
        } else if (buttonView.getId() == R.id.box_profile_4) {
            SharedPreferencesManager.updateThemeNumber(requireContext(), "chave", 4);
            uncheckOthers(box4);
            requireActivity().setTheme(R.style.Theme_Bpd_4);
            requireActivity().recreate();
        }
    }
}