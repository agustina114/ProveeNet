package com.proveenet.proveenet;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DashboardProveedor extends BaseActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // 🔹 Referencias a la UI
    private TextView tvNombreEmpresa, tvWelcome, tvProductosActivos, tvStockBajo, tvOrdenesRecibidas, tvVentasMes;
    private BottomNavigationView bottomNavigationView;
    private ImageButton btnLogout;

    // 🔹 Referencias para la última orden
    private LinearLayout llUltimaOrden;
    private TextView tvOrdenNumero, tvOrdenFecha, tvOrdenTotal, tvOrdenEstado, tvNoOrdenes;

    // 👇 1. Declarar los Listeners para tiempo real
    private ListenerRegistration productosActivosListener;
    private ListenerRegistration stockBajoListener;
    private ListenerRegistration ordenesRecibidasListener;
    private ListenerRegistration ventasMesListener;
    private ListenerRegistration ultimaOrdenListener;


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

        // 🔹 Referencias para última orden
        llUltimaOrden = findViewById(R.id.llUltimaOrden);
        tvOrdenNumero = findViewById(R.id.tvOrdenNumero);
        tvOrdenFecha = findViewById(R.id.tvOrdenFecha);
        tvOrdenTotal = findViewById(R.id.tvOrdenTotal);
        tvOrdenEstado = findViewById(R.id.tvOrdenEstado);
        tvNoOrdenes = findViewById(R.id.tvNoOrdenes);

        // ✅ Marca "Inicio" como activo
        bottomNavigationView.setSelectedItemId(R.id.nav_inicio);

        // 🚀 Cargar datos reales (ahora escuchan en tiempo real)
        cargarNombreProveedor(); // El nombre de la empresa no necesita tiempo real
        escucharProductosActivos();
        escucharStockBajo();
        escucharOrdenesRecibidas();
        escucharVentasMes();
        escucharUltimaOrden();

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
            if (id == R.id.nav_perfil) {
                startActivity(new Intent(this, Perfil_proveedor.class));
                overridePendingTransition(0, 0);
                return true;
            }

            return false;
        });

        // ==========================================================
        // 👇 === CAMBIO IMPORTANTE AQUÍ === 👇
        // ==========================================================
        btnLogout.setOnClickListener(v -> {

            // 1. Apagamos los listeners PRIMERO
            detenerListeners();

            // 2. Cerramos la sesión
            auth.signOut();

            // 3. Mostramos mensaje y navegamos
            Toast.makeText(this, "👋 Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(DashboardProveedor.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
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
                    // Evita el crash si el contexto es nulo (si el usuario cierra sesión rápido)
                    if (this.isFinishing() || this.isDestroyed()) {
                        return;
                    }
                    tvNombreEmpresa.setText(user.getEmail());
                    tvWelcome.setText("Bienvenido, " + user.getEmail());
                    Toast.makeText(this, "❌ Error al cargar proveedor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ==========================================================
    private void escucharProductosActivos() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        productosActivosListener = db.collection("productos")
                .whereEqualTo("proveedorId", user.getUid())
                .whereEqualTo("estado", "activo")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {

                        return;
                    }
                    if (snapshot != null) {
                        tvProductosActivos.setText(String.valueOf(snapshot.size()));
                    }
                });
    }

    private void escucharStockBajo() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        stockBajoListener = db.collection("productos")
                .whereEqualTo("proveedorId", user.getUid())
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        // Toast.makeText(this, "❌ Error al contar stock bajo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshot == null) return;

                    int bajos = 0;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Object stockObj = doc.get("stock");
                        if (stockObj != null) {
                            try {
                                long stock; // Usar long para seguridad
                                if (stockObj instanceof Number) {
                                    stock = ((Number) stockObj).longValue();
                                } else {
                                    stock = Long.parseLong(stockObj.toString());
                                }
                                if (stock <= 5) bajos++;
                            } catch (Exception ignored) {}
                        }
                    }
                    tvStockBajo.setText(String.valueOf(bajos));
                });
    }

    private void escucharOrdenesRecibidas() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        ordenesRecibidasListener = db.collection("ordenes")
                .whereEqualTo("proveedorId", user.getUid())
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        // Toast.makeText(this, "❌ Error al cargar órdenes", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshot != null) {
                        tvOrdenesRecibidas.setText(String.valueOf(snapshot.size()));
                    }
                });
    }

    private void escucharVentasMes() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String mesActual = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        ventasMesListener = db.collection("ventas")
                .whereEqualTo("proveedorId", user.getUid())
                .whereEqualTo("mesAno", mesActual)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        // Toast.makeText(this, "❌ Error al calcular ventas: " + (e != null ? e.getMessage() : ""), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshot == null) return;

                    double totalMes = 0.0;
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Object totalObj = doc.get("total");
                        if (totalObj instanceof Number) {
                            totalMes += ((Number) totalObj).doubleValue();
                        }
                    }
                    tvVentasMes.setText("$" + String.format("%.0f", totalMes));
                });
    }

    private void escucharUltimaOrden() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        ultimaOrdenListener = db.collection("ordenes")
                .whereEqualTo("proveedorId", user.getUid())
                .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        llUltimaOrden.setVisibility(View.GONE);
                        tvNoOrdenes.setVisibility(View.VISIBLE);
                        return;
                    }

                    if (snapshot == null || snapshot.isEmpty()) {
                        llUltimaOrden.setVisibility(View.GONE);
                        tvNoOrdenes.setVisibility(View.VISIBLE);
                        return;
                    }

                    DocumentSnapshot doc = snapshot.getDocuments().get(0);

                    String idOrden = doc.getId();
                    String estado = doc.getString("estado");
                    double subtotal = doc.getDouble("subtotal") != null ? doc.getDouble("subtotal") : 0.0;

                    tvOrdenNumero.setText("Orden #" + idOrden.substring(0, 6));
                    tvOrdenTotal.setText("$" + String.format("%.0f", subtotal));

                    if (estado != null && !estado.isEmpty()) {
                        tvOrdenEstado.setText(estado.substring(0,1).toUpperCase() + estado.substring(1));
                    } else {
                        tvOrdenEstado.setText("Pendiente");
                    }

                    tvOrdenFecha.setText("Reciente");

                    llUltimaOrden.setVisibility(View.VISIBLE);
                    tvNoOrdenes.setVisibility(View.GONE);
                });
    }



    private void detenerListeners() {
        if (productosActivosListener != null) {
            productosActivosListener.remove();
            productosActivosListener = null;
        }
        if (stockBajoListener != null) {
            stockBajoListener.remove();
            stockBajoListener = null;
        }
        if (ordenesRecibidasListener != null) {
            ordenesRecibidasListener.remove();
            ordenesRecibidasListener = null;
        }
        if (ventasMesListener != null) {
            ventasMesListener.remove();
            ventasMesListener = null;
        }
        if (ultimaOrdenListener != null) {
            ultimaOrdenListener.remove();
            ultimaOrdenListener = null;
        }
    }

    // 👇 3. Modificamos onStop() para usar el nuevo método
    @Override
    protected void onStop() {
        super.onStop();
        detenerListeners();
    }
}