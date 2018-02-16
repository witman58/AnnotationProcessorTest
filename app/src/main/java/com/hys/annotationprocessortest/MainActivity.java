package com.hys.annotationprocessortest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hys.mockbutterknife.annotations.BindView;
import com.hys.mockbutterknife.annotations.OnClick;
import com.hys.mockbutterknife_source.MockButterKnife;

import com.hys.annotationprocessortest.R;

/**
 * Created by 胡延森(QQ:1015950695) on 2018/2/8.
 */

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv_click)
    TextView tvClick;
    @BindView(R.id.tv_dont_click)
    TextView tvDontClcik;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MockButterKnife.bind(this);

        initData();
    }

    private void initData(){
        tvClick.setText(getString(R.string.main_click));
        tvDontClcik.setText(getString(R.string.main_dont_click));
    }

    @OnClick(value = {R.id.tv_click, R.id.tv_dont_click})
    public void onClick(View view){

        if(view.getId() == R.id.tv_click)
            new AboutDialog().show(this.getSupportFragmentManager());
        else if(view.getId() == R.id.tv_dont_click)
            Toast.makeText(this, getString(R.string.main_toast), Toast.LENGTH_SHORT).show();
    }
}
