package com.proveenet.proveenet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Registro extends BaseActivity {

    private Button btnComprador, btnProveedor;
    private LinearLayout llFormComprador, llFormProveedor;
    private LinearLayout btnVolver;

    // 🔹 Campos Comprador
    private EditText etNombreComprador, etEmailComprador, etEmpresaComprador;
    private EditText etPasswordComprador, etConfirmPasswordComprador;
    private Button btnCrearCuentaComprador;

    // 🔹 Campos Proveedor
    private EditText etNombreEmpresaProveedor, etEmailProveedor, etTelefonoProveedor;
    private EditText etPasswordProveedor, etConfirmPasswordProveedor;
    private Spinner spinnerRubro;
    private Button btnCrearCuentaProveedor;

    // 🔹 Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;


    private final ColorStateList moradoComprador = ColorStateList.valueOf(Color.parseColor("#6A1B9A"));
    private final ColorStateList verdeProveedor = ColorStateList.valueOf(Color.parseColor("#2E7D32"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        initializeViews();
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupListeners();
        mostrarFormularioComprador();
    }

    private void initializeViews() {
        btnComprador = findViewById(R.id.btnComprador);
        btnProveedor = findViewById(R.id.btnProveedor);
        llFormComprador = findViewById(R.id.llFormComprador);
        llFormProveedor = findViewById(R.id.llFormProveedor);
        btnVolver = findViewById(R.id.btnVolver);

        etNombreComprador = findViewById(R.id.etNombreComprador);
        etEmailComprador = findViewById(R.id.etEmailComprador);
        etEmpresaComprador = findViewById(R.id.etEmpresaComprador);
        etPasswordComprador = findViewById(R.id.etPasswordComprador);
        etConfirmPasswordComprador = findViewById(R.id.etConfirmPasswordComprador);
        btnCrearCuentaComprador = findViewById(R.id.btnCrearCuentaComprador);

        etNombreEmpresaProveedor = findViewById(R.id.etNombreEmpresaProveedor);
        etEmailProveedor = findViewById(R.id.etEmailProveedor);
        spinnerRubro = findViewById(R.id.spinnerRubro);
        etTelefonoProveedor = findViewById(R.id.etTelefonoProveedor);
        etPasswordProveedor = findViewById(R.id.etPasswordProveedor);
        etConfirmPasswordProveedor = findViewById(R.id.etConfirmPasswordProveedor);
        btnCrearCuentaProveedor = findViewById(R.id.btnCrearCuentaProveedor);
    }

    private void setupListeners() {
        btnComprador.setOnClickListener(v -> mostrarFormularioComprador());
        btnProveedor.setOnClickListener(v -> mostrarFormularioProveedor());
        btnVolver.setOnClickListener(v -> finish());

        btnCrearCuentaComprador.setOnClickListener(v -> registrarComprador());
        btnCrearCuentaProveedor.setOnClickListener(v -> registrarProveedor());
    }


    private void mostrarFormularioComprador() {
        llFormComprador.setVisibility(View.VISIBLE);
        llFormProveedor.setVisibility(View.GONE);

        // Botón activo → morado
        btnComprador.setBackgroundResource(R.drawable.btn_selector_selected);
        ViewCompat.setBackgroundTintList(btnComprador, moradoComprador);
        btnComprador.setTextColor(Color.WHITE);

        // Botón inactivo → gris
        btnProveedor.setBackgroundResource(R.drawable.btn_selector_unselected);
        btnProveedor.setTextColor(Color.parseColor("#757575"));

        // Botón “Crear Cuenta”
        btnCrearCuentaComprador.setBackgroundResource(R.drawable.button_blue_bg);
        ViewCompat.setBackgroundTintList(btnCrearCuentaComprador, moradoComprador);
    }

    private void mostrarFormularioProveedor() {
        llFormComprador.setVisibility(View.GONE);
        llFormProveedor.setVisibility(View.VISIBLE);

        // Botón activo → verde
        btnProveedor.setBackgroundResource(R.drawable.btn_selector_selected);
        ViewCompat.setBackgroundTintList(btnProveedor, verdeProveedor);
        btnProveedor.setTextColor(Color.WHITE);

        // Botón inactivo → gris
        btnComprador.setBackgroundResource(R.drawable.btn_selector_unselected);
        btnComprador.setTextColor(Color.parseColor("#757575"));

        // Botón “Crear Cuenta”
        btnCrearCuentaProveedor.setBackgroundResource(R.drawable.button_blue_bg);
        ViewCompat.setBackgroundTintList(btnCrearCuentaProveedor, verdeProveedor);
    }


    // =======================================================
    private void registrarComprador() {
        String nombre = etNombreComprador.getText().toString().trim();
        String email = etEmailComprador.getText().toString().trim();
        String empresa = etEmpresaComprador.getText().toString().trim();
        String password = etPasswordComprador.getText().toString().trim();
        String confirmPassword = etConfirmPasswordComprador.getText().toString().trim();

        if (!validarCamposComprador(nombre, email, empresa, password, confirmPassword)) return;

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();

                    Map<String, Object> comprador = new HashMap<>();
                    comprador.put("nombre", nombre);
                    comprador.put("correo", email);
                    comprador.put("empresa", empresa);
                    comprador.put("rol", "comprador");

                    db.collection("compradores").document(uid).set(comprador)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "✅ Cuenta de comprador creada", Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error al guardar datos: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al registrar: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private boolean validarCamposComprador(String nombre, String email, String empresa, String password, String confirmPassword) {
        if (nombre.isEmpty()) {
            etNombreComprador.setError("Ingresa tu nombre completo");
            return false;
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmailComprador.setError("Ingresa un email válido");
            return false;
        }
        if (empresa.isEmpty()) {
            etEmpresaComprador.setError("Ingresa el nombre de tu empresa");
            return false;
        }
        if (password.isEmpty() || password.length() < 6) {
            etPasswordComprador.setError("La contraseña debe tener al menos 6 caracteres");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPasswordComprador.setError("Las contraseñas no coinciden");
            return false;
        }
        return true;
    }


    // =======================================================
    private void registrarProveedor() {
        String empresa = etNombreEmpresaProveedor.getText().toString().trim();
        String email = etEmailProveedor.getText().toString().trim();
        String rubro = spinnerRubro.getSelectedItem().toString();
        String telefono = etTelefonoProveedor.getText().toString().trim();
        String password = etPasswordProveedor.getText().toString().trim();
        String confirmPassword = etConfirmPasswordProveedor.getText().toString().trim();

        if (!validarCamposProveedor(empresa, email, rubro, telefono, password, confirmPassword)) return;

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();

                    Map<String, Object> proveedor = new HashMap<>();
                    proveedor.put("empresa", empresa);
                    proveedor.put("correo", email);
                    proveedor.put("rubro", rubro);
                    proveedor.put("telefono", telefono);
                    proveedor.put("rol", "proveedor");

                    db.collection("proveedores").document(uid).set(proveedor)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "✅ Cuenta de proveedor creada", Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error al guardar datos: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al registrar: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private boolean validarCamposProveedor(String empresa, String email, String rubro, String telefono, String password, String confirmPassword) {
        if (empresa.isEmpty()) {
            etNombreEmpresaProveedor.setError("Ingresa el nombre de tu empresa");
            return false;
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmailProveedor.setError("Ingresa un email válido");
            return false;
        }
        if (rubro.equals("Seleccionar rubro")) {
            Toast.makeText(this, "Selecciona un rubro", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (telefono.isEmpty()) {
            etTelefonoProveedor.setError("Ingresa tu teléfono");
            return false;
        }
        if (password.isEmpty() || password.length() < 6) {
            etPasswordProveedor.setError("La contraseña debe tener al menos 6 caracteres");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPasswordProveedor.setError("Las contraseñas no coinciden");
            return false;
        }
        return true;
    }
}
