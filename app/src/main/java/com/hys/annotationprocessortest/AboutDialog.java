package com.hys.annotationprocessortest;


import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.hys.mockbutterknife.annotations.BindView;
import com.hys.mockbutterknife.annotations.OnClick;
import com.hys.mockbutterknife_source.MockButterKnife;

import com.hys.annotationprocessortest.R;


/**
 * Created by 胡延森(QQ:1015950695) on 2018/2/8.
 */
public class AboutDialog extends android.support.v4.app.DialogFragment {

    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.tv_message)
    TextView tvMessage;

    public AboutDialog() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.about_dialog, null);
        MockButterKnife.bind(this, view);

        initData();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        return builder.create();
    }

    public void show(FragmentManager fragmentManager) {
        if (isAdded()) return;
        try {
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.remove(this);
            ft.add(this, "about_dialog");
            ft.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClick(value = {R.id.btn_ok})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_ok:
                dismissAllowingStateLoss();
                break;
            default:
                break;
        }
    }

    private void initData(){

        tvTitle.setText(getString(R.string.about_title));
        tvMessage.setText(getString(R.string.about_message));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }
}
