package com.proveenet.proveenet;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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

import java.util.HashMap;
import java.util.Map;

public class Perfil_proveedor extends BaseActivity {

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration perfilListener;

    // Elementos de la Interfaz (UI)
    private TextView tvEmpresaHeader, tvEmpresa, tvRubro, tvEmail, tvTelefono, tvDireccion;
    private ImageButton btnLogout, btnNotifications, btnMenu;
    private LinearLayout btnCambiarContrasena, btnConfigNotificaciones;
    private Button btnEditar, btnEliminarCuenta;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_proveedor);

        // 1. Inicializar Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 2. Vincular Vistas (IDs del XML)
        tvEmpresaHeader = findViewById(R.id.tvEmpresaHeader);
        tvEmpresa = findViewById(R.id.tvEmpresa);
        tvRubro = findViewById(R.id.tvRubro);
        tvEmail = findViewById(R.id.tvEmail);
        tvTelefono = findViewById(R.id.tvTelefono);
        tvDireccion = findViewById(R.id.tvDireccion);

        // Botones del Header
        btnLogout = findViewById(R.id.btnLogout);
        btnNotifications = findViewById(R.id.btnNotifications);
        btnMenu = findViewById(R.id.btnMenu);

        // Navegación
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // Botones de Acción
        btnEditar = findViewById(R.id.btnEditar);
        btnEliminarCuenta = findViewById(R.id.btnEliminarCuenta);
        btnCambiarContrasena = findViewById(R.id.btnCambiarContrasena);
        btnConfigNotificaciones = findViewById(R.id.btnConfigNotificaciones);

        // 3. Verificar sesión activa
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // 4. Cargar datos del proveedor en tiempo real
        escucharDatosProveedor();

        // 5. Configurar funcionalidades
        setupBotones();
        setupNavigation();
    }

    // ======================================================
    //  LOGICA DE DATOS (Tiempo Real - Firestore)
    // ======================================================
    private void escucharDatosProveedor() {
        String uid = auth.getCurrentUser().getUid();

        // Conexión a la colección "proveedores"
        perfilListener = db.collection("proveedores").document(uid)
                .addSnapshotListener((document, e) -> {
                    // Manejo de errores
                    if (e != null) {
                        Toast.makeText(this, "Error al cargar perfil", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (document != null && document.exists()) {
                        cargarDatosEnVistas(document);
                    } else {
                        tvEmpresaHeader.setText("Proveedor sin datos");
                    }
                });
    }

    private void cargarDatosEnVistas(DocumentSnapshot doc) {
        // Extraer datos del documento
        String empresa = doc.getString("empresa");
        String correo = doc.getString("correo");
        String rubro = doc.getString("rubro");
        String direccion = doc.getString("direccion");
        String telefono = doc.getString("telefono");

        // Asignar a los TextViews con validación (evita que salga "null")
        tvEmpresaHeader.setText(empresa != null ? empresa : "Sin Nombre");
        tvEmpresa.setText(empresa != null ? empresa : "No especificada");
        tvRubro.setText("Rubro: " + (rubro != null ? rubro : "General"));
        tvEmail.setText(correo != null ? correo : auth.getCurrentUser().getEmail());
        tvTelefono.setText(telefono != null && !telefono.isEmpty() ? telefono : "No especificado");
        tvDireccion.setText(direccion != null && !direccion.isEmpty() ? direccion : "No especificada");
    }

    // ======================================================
    //  LOGICA DE BOTONES
    // ======================================================
    private void setupBotones() {

        // Logout Seguro (Apaga el listener antes de salir)
        btnLogout.setOnClickListener(v -> {
            apagarListener();
            auth.signOut();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Funcionalidad Editar con Modal
        btnEditar.setOnClickListener(v -> mostrarModalEditar());

        // Eliminar Cuenta con Confirmación
        btnEliminarCuenta.setOnClickListener(v -> mostrarAlertaEliminarCuenta());

        // Botones extra de la UI
        btnCambiarContrasena.setOnClickListener(v ->
                Toast.makeText(this, "Cambiar contraseña...", Toast.LENGTH_SHORT).show());

        btnConfigNotificaciones.setOnClickListener(v ->
                Toast.makeText(this, "Configurar notificaciones...", Toast.LENGTH_SHORT).show());
    }

    // 👇 LOGICA DEL MODAL DE EDICION
    private void mostrarModalEditar() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.modal_editar_cuenta);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Vincular vistas del modal
        EditText etEmpresa = dialog.findViewById(R.id.etEmpresaEdit);
        EditText etRubro = dialog.findViewById(R.id.etRubroEdit);
        EditText etTelefono = dialog.findViewById(R.id.etTelefonoEdit);
        EditText etDireccion = dialog.findViewById(R.id.etDireccionEdit);
        Button btnGuardar = dialog.findViewById(R.id.btnGuardarEdit);
        Button btnCancelar = dialog.findViewById(R.id.btnCancelarEdit);

        // Pre-llenar con datos actuales
        etEmpresa.setText(tvEmpresa.getText().toString());
        // Limpiamos el prefijo "Rubro: " si existe para que sea más fácil editar
        String rubroLimpio = tvRubro.getText().toString().replace("Rubro: ", "");
        etRubro.setText(rubroLimpio);

        etTelefono.setText(tvTelefono.getText().toString());
        etDireccion.setText(tvDireccion.getText().toString());

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnGuardar.setOnClickListener(v -> {
            String nuevaEmpresa = etEmpresa.getText().toString().trim();
            String nuevoRubro = etRubro.getText().toString().trim();
            String nuevoTelefono = etTelefono.getText().toString().trim();
            String nuevaDireccion = etDireccion.getText().toString().trim();

            if (nuevaEmpresa.isEmpty()) {
                Toast.makeText(this, "El nombre de la empresa es obligatorio", Toast.LENGTH_SHORT).show();
                return;
            }

            // Actualizar en Firebase
            Map<String, Object> updates = new HashMap<>();
            updates.put("empresa", nuevaEmpresa);
            updates.put("rubro", nuevoRubro);
            updates.put("telefono", nuevoTelefono);
            updates.put("direccion", nuevaDireccion);

            db.collection("proveedores").document(auth.getCurrentUser().getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "✅ Perfil actualizado", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "❌ Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });

        dialog.show();
    }

    private void mostrarAlertaEliminarCuenta() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Cuenta de Proveedor")
                .setMessage("¡Atención! Se eliminarán tu cuenta y tus datos de contacto. Tus productos podrían permanecer visibles hasta que se actualice el sistema. ¿Estás seguro?")
                .setPositiveButton("ELIMINAR", (dialog, which) -> eliminarCuentaReal())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarCuentaReal() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        // 1. Primero borrar el documento en "proveedores"
        db.collection("proveedores").document(user.getUid())
                .delete()
                .addOnSuccessListener(aVoid -> {

                    // 2. Si Firestore se borra, borrar la cuenta de Auth
                    user.delete()
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, "Cuenta eliminada correctamente.", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    // Error de seguridad si lleva mucho tiempo logueado
                                    Toast.makeText(this, "⚠️ Por seguridad, cierra sesión e ingresa de nuevo para finalizar la eliminación.", Toast.LENGTH_LONG).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error al borrar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }


    private void setupNavigation() {
        bottomNavigationView.getMenu().setGroupCheckable(0, false, true);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, DashboardProveedor.class));
                overridePendingTransition(0, 0); // Sin animación
                finish();
                return true;
            } else if (id == R.id.nav_catalogo) {
                startActivity(new Intent(this, MiCatalogo.class));
                overridePendingTransition(0, 0); // Sin animación
                finish();
                return true;
            } else if (id == R.id.nav_ordenes) {
                startActivity(new Intent(this, MisOrdenes.class));
                overridePendingTransition(0, 0); // Sin animación
                finish();
                return true;
            }
            return false;
        });
    }


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