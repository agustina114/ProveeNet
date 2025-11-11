package com.proveenet.proveenet;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
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

public class DashboardProveedor extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // 🔹 Referencias a la UI
    private TextView tvNombreEmpresa, tvWelcome, tvProductosActivos, tvStockBajo, tvOrdenesRecibidas, tvVentasMes;
    private BottomNavigationView bottomNavigationView;

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
    }

    // ==========================================================
    // 👤 Cargar el nombre real del proveedor
    // ==========================================================
    private void cargarNombreProveedor() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "⚠️ No se detectó sesión activa", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("proveedores").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String nombreEmpresa = doc.getString("nombreEmpresa");
                        if (nombreEmpresa != null && !nombreEmpresa.isEmpty()) {
                            tvNombreEmpresa.setText(nombreEmpresa);
                            tvWelcome.setText("Bienvenido, " + nombreEmpresa);
                        } else {
                            tvNombreEmpresa.setText("Proveedor");
                            tvWelcome.setText("Bienvenido, proveedor");
                        }
                    } else {
                        tvNombreEmpresa.setText("Proveedor no registrado");
                        tvWelcome.setText("Bienvenido");
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "❌ Error al cargar proveedor: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    // ==========================================================
    // 📦 Contar productos activos
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

    // ==========================================================
// ⚠️ Contar productos con stock bajo (≤ 5) - tolerante a String o Number
// ==========================================================
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

                                // 🔹 Detectar tipo del campo en tiempo real
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

    // ==========================================================
    // 📬 Contar órdenes recibidas del proveedor
    // ==========================================================
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
    // 💰 Calcular ventas del mes actual
    // ==========================================================
    private void cargarVentasMes() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        // Obtener mes actual (ej: "2025-11")
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String mesActual = sdf.format(Calendar.getInstance().getTime());

        db.collection("ordenes")
                .whereEqualTo("proveedorId", user.getUid())
                .whereEqualTo("estado", "confirmada") // solo confirmadas
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
