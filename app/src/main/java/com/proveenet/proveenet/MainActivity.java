package com.proveenet.proveenet;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    // 🔹 Elementos de UI
    private Button btnComprador, btnProveedor, btnLogin;
    private TextView tvLoginTitle, registrate;
    private EditText etEmail, etPassword;
    private ImageView ivLogo;

    // 🔹 Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // 🔹 Estado actual
    private boolean esComprador = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            inicializarFirebase();
            inicializarUI();
            configurarEventos();
            actualizarFormulario();
        } catch (Exception e) {
            Toast.makeText(this, "Error al iniciar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void inicializarFirebase() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void inicializarUI() {
        btnComprador = findViewById(R.id.btnComprador);
        btnProveedor = findViewById(R.id.btnProveedor);
        btnLogin = findViewById(R.id.btnLogin);
        tvLoginTitle = findViewById(R.id.tvLoginTitle);
        registrate = findViewById(R.id.registrate);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        ivLogo = findViewById(R.id.ivLogo);
    }

    private void configurarEventos() {
        btnComprador.setOnClickListener(v -> {
            esComprador = true;
            actualizarFormulario();
        });

        btnProveedor.setOnClickListener(v -> {
            esComprador = false;
            actualizarFormulario();
        });

        btnLogin.setOnClickListener(v -> iniciarSesion());

        // 🔹 Ir a registro
        registrate.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Registro.class);
            startActivity(intent);
        });
    }

    // =======================================================
    // 🔹 Cambia colores y textos según el tipo de usuario
    // =======================================================
    private void actualizarFormulario() {
        int blanco = ContextCompat.getColor(this, android.R.color.white);
        int negro = ContextCompat.getColor(this, android.R.color.black);

        if (esComprador) {
            // ======== COMPRADOR ========
            btnComprador.setBackgroundResource(R.drawable.btn_selector_selected);
            btnProveedor.setBackgroundResource(R.drawable.btn_selector_unselected);
            btnComprador.setTextColor(blanco);
            btnProveedor.setTextColor(negro);

            tvLoginTitle.setText("Iniciar Sesión - Comprador");
            etEmail.setHint("tu@email.cl");

        } else {
            // ======== PROVEEDOR ========
            btnProveedor.setBackgroundResource(R.drawable.btn_selector_selected);
            btnComprador.setBackgroundResource(R.drawable.btn_selector_unselected);
            btnProveedor.setTextColor(blanco);
            btnComprador.setTextColor(negro);

            tvLoginTitle.setText("Iniciar Sesión - Proveedor");
            etEmail.setHint("proveedor@empresa.cl");
        }
    }

    // =======================================================
    // 🔹 Función principal de login
    // =======================================================
    private void iniciarSesion() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user != null) {
                        String uid = user.getUid();
                        if (esComprador) {
                            verificarRolEnFirestore(uid, "compradores");
                        } else {
                            verificarRolEnFirestore(uid, "proveedores");
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    // =======================================================
    // 🔹 Verificar si el usuario pertenece a la colección
    // =======================================================
    private void verificarRolEnFirestore(String uid, String coleccion) {
        db.collection(coleccion).document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        if (coleccion.equals("compradores")) {
                            startActivity(new Intent(this, Panel_comprador.class));
                        } else {
                            startActivity(new Intent(this, DashboardProveedor.class));
                        }
                        finish();
                    } else {
                        Toast.makeText(this, "No se encontró el usuario en " + coleccion, Toast.LENGTH_SHORT).show();
                        auth.signOut();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al verificar rol: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
