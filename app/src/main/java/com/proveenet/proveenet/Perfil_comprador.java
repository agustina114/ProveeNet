package com.proveenet.proveenet;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog; // Importado
import android.content.Intent;
import android.graphics.Color; // Importado
import android.graphics.drawable.ColorDrawable; // Importado
import android.os.Bundle;
import android.view.ViewGroup; // Importado
import android.widget.Button;
import android.widget.EditText; // Importado
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap; // Importado
import java.util.Locale;
import java.util.Map;     // Importado

public class Perfil_comprador extends BaseActivity {

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration perfilListener;

    // UI Elements
    private TextView tvUserName, tvEmail, tvTelefono, tvDireccion, tvEmpresa, tvUsuario, tvMiembroDesde;
    private ImageButton btnLogout, btnNotifications, btnMenu;
    private LinearLayout btnCambiarContrasena, btnConfigNotificaciones;
    private Button btnEditar, btnEliminarCuenta;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_comprador);

        // 1. Inicializar Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 2. Vincular Vistas
        tvUserName = findViewById(R.id.tvUserName); // Header
        tvEmail = findViewById(R.id.tvEmail);
        tvTelefono = findViewById(R.id.tvTelefono);
        tvDireccion = findViewById(R.id.tvDireccion);
        tvEmpresa = findViewById(R.id.tvEmpresa);
        tvUsuario = findViewById(R.id.tvUsuario); // En info de cuenta
        tvMiembroDesde = findViewById(R.id.tvMiembroDesde);

        btnLogout = findViewById(R.id.btnLogout);
        btnNotifications = findViewById(R.id.btnNotifications);
        btnMenu = findViewById(R.id.btnMenu);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        btnEditar = findViewById(R.id.btnEditar);
        btnEliminarCuenta = findViewById(R.id.btnEliminarCuenta);
        btnCambiarContrasena = findViewById(R.id.btnCambiarContrasena);
        btnConfigNotificaciones = findViewById(R.id.btnConfigNotificaciones);

        // 3. Verificar sesión
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // 4. Cargar datos
        escucharDatosPerfil();

        // 5. Configurar Botones y Navegación
        setupBotones();
        setupNavigation();
    }

    // ======================================================
    //  LOGICA DE DATOS (Tiempo Real)
    // ======================================================
    private void escucharDatosPerfil() {
        String uid = auth.getCurrentUser().getUid();

        perfilListener = db.collection("compradores").document(uid)
                .addSnapshotListener((document, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Error al cargar perfil", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (document != null && document.exists()) {
                        cargarDatosEnVistas(document);
                    } else {
                        FirebaseUser user = auth.getCurrentUser();
                        tvUserName.setText(user.getDisplayName());
                        tvEmail.setText(user.getEmail());
                        tvUsuario.setText(user.getEmail());
                    }
                });
    }

    private void cargarDatosEnVistas(DocumentSnapshot doc) {
        String nombre = doc.getString("nombre");
        String email = doc.getString("correo"); // Asegúrate que en Firestore sea "correo" o "email"
        if (email == null) email = doc.getString("email"); // Intento alternativo

        String telefono = doc.getString("telefono");
        String direccion = doc.getString("direccion");
        String empresa = doc.getString("empresa");

        tvUserName.setText(nombre != null ? nombre : "Usuario");
        tvEmail.setText(email != null ? email : auth.getCurrentUser().getEmail());
        tvUsuario.setText(email != null ? email : auth.getCurrentUser().getEmail());
        tvTelefono.setText(telefono != null && !telefono.isEmpty() ? telefono : "No especificado");
        tvDireccion.setText(direccion != null && !direccion.isEmpty() ? direccion : "No especificada");
        tvEmpresa.setText(empresa != null && !empresa.isEmpty() ? empresa : "No especificada");

        long creationTimestamp = auth.getCurrentUser().getMetadata().getCreationTimestamp();
        Date date = new Date(creationTimestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
        tvMiembroDesde.setText(sdf.format(date));
    }

    // ======================================================
    //  LOGICA DE BOTONES
    // ======================================================
    private void setupBotones() {
        btnLogout.setOnClickListener(v -> {
            apagarListener();
            auth.signOut();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // 👇 AQUI LLAMAMOS AL MODAL DE COMPRADOR
        btnEditar.setOnClickListener(v -> mostrarModalEditar());

        btnEliminarCuenta.setOnClickListener(v -> mostrarAlertaEliminarCuenta());

        btnCambiarContrasena.setOnClickListener(v ->
                Toast.makeText(this, "Enviar correo de restablecimiento...", Toast.LENGTH_SHORT).show());
    }

    // 👇 METODO NUEVO: Mostrar Modal
    private void mostrarModalEditar() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.modal_editar_cuenta_comprador);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Vincular vistas
        EditText etNombre = dialog.findViewById(R.id.etNombreEdit);
        EditText etTelefono = dialog.findViewById(R.id.etTelefonoEdit);
        EditText etDireccion = dialog.findViewById(R.id.etDireccionEdit);
        EditText etEmpresa = dialog.findViewById(R.id.etEmpresaEdit);
        Button btnGuardar = dialog.findViewById(R.id.btnGuardarEdit);
        Button btnCancelar = dialog.findViewById(R.id.btnCancelarEdit);

        // Pre-llenar datos
        etNombre.setText(tvUserName.getText().toString());
        etTelefono.setText(tvTelefono.getText().toString());
        etDireccion.setText(tvDireccion.getText().toString());
        etEmpresa.setText(tvEmpresa.getText().toString());

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnGuardar.setOnClickListener(v -> {
            String nuevoNombre = etNombre.getText().toString().trim();
            String nuevoTelefono = etTelefono.getText().toString().trim();
            String nuevaDireccion = etDireccion.getText().toString().trim();
            String nuevaEmpresa = etEmpresa.getText().toString().trim();

            if (nuevoNombre.isEmpty()) {
                Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show();
                return;
            }

            // Actualizar en Firestore (Colección "compradores")
            Map<String, Object> updates = new HashMap<>();
            updates.put("nombre", nuevoNombre);
            updates.put("telefono", nuevoTelefono);
            updates.put("direccion", nuevaDireccion);
            updates.put("empresa", nuevaEmpresa);

            db.collection("compradores").document(auth.getCurrentUser().getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "✅ Perfil actualizado", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });

        dialog.show();
    }

    private void mostrarAlertaEliminarCuenta() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Cuenta")
                .setMessage("¿Estás seguro? Se borrarán todos tus datos y no podrás recuperar el acceso.")
                .setPositiveButton("ELIMINAR", (dialog, which) -> eliminarCuentaReal())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarCuentaReal() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        // 1. Borrar datos de Firestore
        db.collection("compradores").document(user.getUid()).delete()
                .addOnSuccessListener(aVoid -> {
                    // 2. Borrar usuario de Auth
                    user.delete()
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, "Cuenta eliminada", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "⚠️ Cierra sesión e ingresa de nuevo para eliminar.", Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error al borrar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ======================================================
    //  NAVEGACIÓN
    // ======================================================
    private void setupNavigation() {
        bottomNavigationView.getMenu().setGroupCheckable(0, false, true);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, Panel_comprador.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_proveedores) {
                startActivity(new Intent(this, Proveedores.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_productos) {
                startActivity(new Intent(this, Productos.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_carrito) {
                startActivity(new Intent(this, MiCarrito.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    // ======================================================
    //  CICLO DE VIDA
    // ======================================================
    private void apagarListener() {
        if (perfilListener != null) {
            perfilListener.remove();
            perfilListener = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        apagarListener();
    }
}