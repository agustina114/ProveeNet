package com.proveenet.proveenet;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DashboardProveedor extends BaseActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // 🔹 Referencias a la UI
    private TextView tvNombreEmpresa, tvWelcome, tvProductosActivos, tvStockBajo, tvOrdenesRecibidas, tvVentasMes;
    private BottomNavigationView bottomNavigationView;
    private ImageButton btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_proveedor);

        // 🔹 Inicializa Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 🔹 Vincula elementos del layout
        tvNombreEmpresa = findViewById(R.id.tvNombreEmpresa);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvProductosActivos = findViewById(R.id.tvProductosActivos);
        tvStockBajo = findViewById(R.id.tvStockBajo);
        tvOrdenesRecibidas = findViewById(R.id.tvOrdenesRecibidas);
        tvVentasMes = findViewById(R.id.tvVentasMes);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        btnLogout = findViewById(R.id.btnLogout);

        // ✅ Marca “Inicio” como activo
        bottomNavigationView.setSelectedItemId(R.id.nav_inicio);

        // 🚀 Cargar datos reales
        cargarNombreProveedor();
        cargarProductosActivos();
        cargarStockBajo();
        cargarOrdenesRecibidas();
        cargarVentasMes();

        // 🔹 Navegación inferior
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_inicio) return true;

            if (id == R.id.nav_catalogo) {
                startActivity(new Intent(this, MiCatalogo.class));
                overridePendingTransition(0, 0);
                return true;
            }

            if (id == R.id.nav_ordenes) {
                startActivity(new Intent(this, MisOrdenes.class));
                overridePendingTransition(0, 0);
                return true;
            }

            return false;
        });

        // 🚪 Botón cerrar sesión
        btnLogout.setOnClickListener(v -> {
            auth.signOut(); // Cierra la sesión actual en Firebase

            Toast.makeText(this, "👋 Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(DashboardProveedor.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Limpia el historial
            startActivity(intent);
            finish(); // Cierra la Activity actual
        });
    }

    // ==========================================================
    private void cargarNombreProveedor() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            tvNombreEmpresa.setText("Sesión no detectada");
            tvWelcome.setText("Bienvenido");
            Toast.makeText(this, "⚠️ No se detectó sesión activa", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("proveedores").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String empresa = doc.getString("empresa");
                        String correo = doc.getString("correo");

                        if (empresa != null && !empresa.isEmpty()) {
                            tvNombreEmpresa.setText(empresa);
                            tvWelcome.setText("Bienvenido, " + empresa);
                        } else if (correo != null && !correo.isEmpty()) {
                            tvNombreEmpresa.setText(correo);
                            tvWelcome.setText("Bienvenido, " + correo);
                        } else {
                            tvNombreEmpresa.setText(user.getEmail());
                            tvWelcome.setText("Bienvenido, " + user.getEmail());
                        }
                    } else {
                        tvNombreEmpresa.setText(user.getEmail());
                        tvWelcome.setText("Bienvenido, " + user.getEmail());
                    }
                })
                .addOnFailureListener(e -> {
                    tvNombreEmpresa.setText(user.getEmail());
                    tvWelcome.setText("Bienvenido, " + user.getEmail());
                    Toast.makeText(this, "❌ Error al cargar proveedor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    // ==========================================================
    private void cargarProductosActivos() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("productos")
                .whereEqualTo("proveedorId", user.getUid())
                .whereEqualTo("estado", "activo")
                .get()
                .addOnSuccessListener(snapshot ->
                        tvProductosActivos.setText(String.valueOf(snapshot.size())))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error al contar productos", Toast.LENGTH_SHORT).show());
    }


    private void cargarStockBajo() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("productos")
                .whereEqualTo("proveedorId", user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    int bajos = 0;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Object stockObj = doc.get("stock");
                        if (stockObj != null) {
                            try {
                                int stock;
                                if (stockObj instanceof Number) {
                                    stock = ((Number) stockObj).intValue();
                                } else {
                                    stock = Integer.parseInt(stockObj.toString());
                                }
                                if (stock <= 5) bajos++;
                            } catch (Exception ignored) {}
                        }
                    }
                    tvStockBajo.setText(String.valueOf(bajos));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error al contar stock bajo: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private void cargarOrdenesRecibidas() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("ordenes")
                .whereEqualTo("proveedorId", user.getUid())
                .get()
                .addOnSuccessListener(snapshot ->
                        tvOrdenesRecibidas.setText(String.valueOf(snapshot.size())))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error al cargar órdenes", Toast.LENGTH_SHORT).show());
    }


    // ==========================================================
    private void cargarVentasMes() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String mesActual = sdf.format(Calendar.getInstance().getTime());

        db.collection("ordenes")
                .whereEqualTo("proveedorId", user.getUid())
                .whereEqualTo("estado", "confirmada")
                .get()
                .addOnSuccessListener(snapshot -> {
                    double totalMes = 0.0;
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Object fechaObj = doc.get("fechaCreacion");
                        Object subtotalObj = doc.get("subtotal");

                        if (fechaObj != null && subtotalObj != null) {
                            String fecha = fechaObj.toString();
                            if (fecha.contains(mesActual)) {
                                try {
                                    totalMes += Double.parseDouble(subtotalObj.toString());
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                    tvVentasMes.setText("$" + String.format("%.0f", totalMes));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error al calcular ventas", Toast.LENGTH_SHORT).show());
    }
}
