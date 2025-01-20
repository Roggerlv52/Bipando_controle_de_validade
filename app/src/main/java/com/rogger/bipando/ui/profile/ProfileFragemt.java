package com.rogger.bipando.ui.profile;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.rogger.bipando.R;
import com.rogger.bipando.ui.commun.SharedPreferencesManager;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragemt extends Fragment {
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private SwitchMaterial box1, box2, box3, box4,boxBeep;

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
        boolean stateBeep = SharedPreferencesManager.getBeepState(requireContext(),"beep");

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

        CircleImageView circleImageView = view.findViewById(R.id.profileImage);
        TextView txtName = view.findViewById(R.id.userNameProfile);
        TextView txtTitle = view.findViewById(R.id.profile_title_name);

        List<String> userInfo = SharedPreferencesManager.getUserInfo(requireContext());
        String imageUrl = userInfo.get(1);
        String name = userInfo.get(0);
        txtName.setText(name);
        txtTitle.setText(name);
        if (imageUrl != null){
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(circleImageView);
        }
        if(stateBeep){
            boxBeep.setChecked(true);
        }
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

        CompoundButton.OnCheckedChangeListener listener = this::onCheckedChanged;
        CompoundButton.OnCheckedChangeListener listenerBeep = this::onCheckedBeep;
        // Adicionando o listener aos CheckBoxes
        box1.setOnCheckedChangeListener(listener);
        box2.setOnCheckedChangeListener(listener);
        box3.setOnCheckedChangeListener(listener);
        box4.setOnCheckedChangeListener(listener);

        boxBeep.setOnCheckedChangeListener(listenerBeep);
        super.onViewCreated(view, savedInstanceState);
    }

    private void onCheckedBeep(CompoundButton compoundButton, boolean beep) {
        if(beep){
            if (compoundButton.getId() == R.id.box_off_beep) {
                SharedPreferencesManager.sharedBeepState(requireContext(),"beep", true);
            }
        }
        else{
            SharedPreferencesManager.sharedBeepState(requireContext(),"beep", false);
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
                requireActivity().setTheme(R.style.Theme_Bipando);
                requireActivity().recreate();
            } else if (buttonView.getId() == R.id.box_profile_2) {
                SharedPreferencesManager.updateThemeNumber(requireContext(), "chave", 2);
                uncheckOthers(box2);
                requireActivity().setTheme(R.style.Theme_Bipando_2);
                requireActivity().recreate();
            } else if (buttonView.getId() == R.id.box_profile_3) {
                SharedPreferencesManager.updateThemeNumber(requireContext(), "chave", 3);
                uncheckOthers(box3);
                requireActivity().setTheme(R.style.Theme_Bipando_3);
                requireActivity().recreate();
            } else if (buttonView.getId() == R.id.box_profile_4) {
                SharedPreferencesManager.updateThemeNumber(requireContext(), "chave", 4);
                uncheckOthers(box4);
                requireActivity().setTheme(R.style.Theme_Bipando_4);
                requireActivity().recreate();
            }
        }
    }

}