package com.proveenet.proveenet;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔹 Deja la barra de estado transparente en todas las pantallas
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);

        // 🔹 Ajustar automáticamente el header si existe un view con id "llHeader"
        View header = findViewById(R.id.llHeader);
        if (header != null) {
            int statusBarHeightId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (statusBarHeightId > 0) {
                int statusBarHeight = getResources().getDimensionPixelSize(statusBarHeightId);
                header.setPadding(
                        header.getPaddingLeft(),
                        header.getPaddingTop() + statusBarHeight,
                        header.getPaddingRight(),
                        header.getPaddingBottom()
                );
            }
        }
    }
}
