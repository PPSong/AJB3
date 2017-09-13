package com.penn.ajb3;

import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.penn.ajb3.databinding.ActivityTestBinding;
import com.squareup.picasso.Picasso;

public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityTestBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_test);
        Picasso.with(this).setLoggingEnabled(true);
        Picasso.with(this)
                .load("https://ss0.bdstatic.com/5aV1bjqh_Q23odCf/static/superman/img/logo/bd_logo1_31bdc765.png")
                .placeholder(android.R.drawable.ic_menu_myplaces)
                .error(android.R.drawable.stat_notify_error)
                .into(binding.mainIv);

    }
}
