package com.rogger.bp.ui.profile;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.rogger.bp.R;
import com.rogger.bp.notification.NotificationPrefs;
import com.rogger.bp.notification.NotificationScheduler;
import com.rogger.bp.ui.commun.SharedPreferencesManager;

import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private SwitchMaterial box1, box2, box3, box4, boxBeep, swNotification;
    private TextView txtNotifTime;
    private View btnSetNotifTime;

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
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Mostrar a seta de voltar
            activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        box1 = view.findViewById(R.id.box_profile_1);
        box2 = view.findViewById(R.id.box_profile_2);
        box3 = view.findViewById(R.id.box_profile_3);
        box4 = view.findViewById(R.id.box_profile_4);
        boxBeep = view.findViewById(R.id.box_off_beep);
        swNotification = view.findViewById(R.id.swNotification);
        
        btnSetNotifTime = view.findViewById(R.id.btnSetNotifTime);
        txtNotifTime = view.findViewById(R.id.txtNotifTime);

        ImageView circleImageView = view.findViewById(R.id.profileImage);
        TextView txtName = view.findViewById(R.id.userNameProfile);
        TextView txtTitle = view.findViewById(R.id.profile_title_name);
        Slider sliderYellow = view.findViewById(R.id.sliderYellow);
        TextView txtYellow = view.findViewById(R.id.txtYellow);

        swNotification.setChecked(NotificationPrefs.getAlert(requireContext()));
        sliderYellow.setValue(NotificationPrefs.getDays(requireContext()));

        txtYellow.setText((int) sliderYellow.getValue() + " dias");
        
        // Atualiza o texto do horário com o valor salvo
        updateTimeText();

        List<String> userInfo = SharedPreferencesManager.getUserInfo(requireContext());
        String name = userInfo.get(1);
        txtName.setText(name);
        txtTitle.setText(name);

            Glide.with(requireContext())
                    .load(userInfo.get(2))
                    .override(350, 350) // reduz tamanho
                    .placeholder(R.drawable.ic_person_24)   // enquanto carrega
                    .error(R.drawable.ic_person_24)         // se der erro
                    .into(circleImageView);;

        boxBeep.setChecked(stateBeep);
        switch (themeNumber) {
            case 1:
            case 0:
                box1.setChecked(true);
                break;
            case 2:
                box2.setChecked(true);
                break;
            case 3:
                box3.setChecked(true);
                break;
            case 4:
                box4.setChecked(true);
                break;
        }

        swNotification.setOnCheckedChangeListener((button, isChecked) -> {
            NotificationPrefs.onAlert(requireContext(), isChecked);
            if (isChecked) {
                NotificationScheduler.start(requireContext());
            } else {
                NotificationScheduler.stop(requireContext());
            }
        });

        btnSetNotifTime.setOnClickListener(v -> showTimePickerDialog());

        CompoundButton.OnCheckedChangeListener listener = this::onCheckedChanged;
        CompoundButton.OnCheckedChangeListener listenerBeep = this::onCheckedBeep;
        // Adicionando o listener aos CheckBoxes
        box1.setOnCheckedChangeListener(listener);
        box2.setOnCheckedChangeListener(listener);
        box3.setOnCheckedChangeListener(listener);
        box4.setOnCheckedChangeListener(listener);

        boxBeep.setOnCheckedChangeListener(listenerBeep);
        sliderYellow.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                txtYellow.setText((int) value + " dias");
                NotificationPrefs.saveDays(requireContext(), (int) value);
            }
        });

        super.onViewCreated(view, savedInstanceState);
    }

    private void showTimePickerDialog() {
        int hour = NotificationPrefs.getHour(requireContext());
        int minute = NotificationPrefs.getMinute(requireContext());

        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minuteOfHour) -> {
                    NotificationPrefs.saveTime(requireContext(), hourOfDay, minuteOfHour);
                    updateTimeText();
                    
                    // Se as notificações estiverem ativadas, reinicia o agendador para aplicar o novo horário
                    if (NotificationPrefs.getAlert(requireContext())) {
                        NotificationScheduler.start(requireContext());
                    }
                }, hour, minute, true);
        timePickerDialog.show();
    }

    private void updateTimeText() {
        int hour = NotificationPrefs.getHour(requireContext());
        int minute = NotificationPrefs.getMinute(requireContext());
        txtNotifTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
    }

    private void onCheckedBeep(CompoundButton compoundButton, boolean beep) {
        if (beep) {
            if (compoundButton.getId() == R.id.box_off_beep) {
                SharedPreferencesManager.sharedBeepState(requireContext(), "beep", true);
            }
        } else {
            SharedPreferencesManager.sharedBeepState(requireContext(), "beep", false);
        }
    }

    private void uncheckOthers(@SuppressLint("UseSwitchCompatOrMaterialCode") SwitchMaterial selectedCheckBox) {
        if (selectedCheckBox != box1) box1.setChecked(false);
        if (selectedCheckBox != box2) box2.setChecked(false);
        if (selectedCheckBox != box3) box3.setChecked(false);
        if (selectedCheckBox != box4) box4.setChecked(false);
    }

    private void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
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

}
