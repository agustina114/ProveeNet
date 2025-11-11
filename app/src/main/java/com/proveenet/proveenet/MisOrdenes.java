package com.proveenet.proveenet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MisOrdenes extends AppCompatActivity {

    private RecyclerView recyclerOrdenes;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private OrdenAdapter ordenAdapter;
    private List<Map<String, Object>> listaOrdenes;

    // Header
    private TextView tvNombreEmpresa, tvOrdenesCount;
    private ImageButton btnMenu, btnLogout, btnNotifications;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mis_ordenes);

        // Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Recycler
        recyclerOrdenes = findViewById(R.id.recyclerOrdenes);
        recyclerOrdenes.setLayoutManager(new LinearLayoutManager(this));
        listaOrdenes = new ArrayList<>();
        ordenAdapter = new OrdenAdapter(listaOrdenes, db);
        recyclerOrdenes.setAdapter(ordenAdapter);

        // Header y nav
        tvNombreEmpresa = findViewById(R.id.tvNombreEmpresa);
        tvOrdenesCount = findViewById(R.id.tvOrdenesCount);
        btnMenu = findViewById(R.id.btnMenu);
        btnLogout = findViewById(R.id.btnLogout);
        btnNotifications = findViewById(R.id.btnNotifications);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        bottomNavigationView.setSelectedItemId(R.id.nav_ordenes);

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "⚠️ Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Cargar info del header y las órdenes
        cargarNombreProveedor();
        cargarOrdenesProveedor();

        // --- Header ---
        btnMenu.setOnClickListener(v -> {
            startActivity(new Intent(MisOrdenes.this, DashboardProveedor.class));
            overridePendingTransition(0, 0);
        });

        btnNotifications.setOnClickListener(v ->
                Toast.makeText(this, "🔔 Próximamente notificaciones", Toast.LENGTH_SHORT).show()
        );

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent i = new Intent(MisOrdenes.this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });

        // --- Bottom Navigation ---
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, DashboardProveedor.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_catalogo) {
                startActivity(new Intent(this, MiCatalogo.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_ordenes) {
                return true;
            }
            return false;
        });
    }

    // ==========================================================
    // 👤 Cargar nombre proveedor
    // ==========================================================
    private void cargarNombreProveedor() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("proveedores").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String nombreEmpresa = doc.getString("nombreEmpresa");
                        tvNombreEmpresa.setText(nombreEmpresa != null ? nombreEmpresa : "Proveedor");
                    } else {
                        tvNombreEmpresa.setText("Proveedor");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al cargar nombre: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ==========================================================
    // 📦 Cargar órdenes del proveedor (con control de nulos)
    // ==========================================================
    private void cargarOrdenesProveedor() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("ordenes")
                .whereEqualTo("proveedorId", user.getUid()) // 🔥 el filtro correcto
                .get()
                .addOnSuccessListener(snapshot -> {
                    listaOrdenes.clear();

                    if (snapshot.isEmpty()) {
                        tvOrdenesCount.setText("No hay órdenes");
                        ordenAdapter.notifyDataSetChanged();
                        return;
                    }

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Map<String, Object> orden = doc.getData();
                        if (orden != null) {
                            orden.put("id", doc.getId());
                            // Evita errores por campos nulos
                            if (!orden.containsKey("productoNombre"))
                                orden.put("productoNombre", "Producto desconocido");
                            if (!orden.containsKey("compradorNombre"))
                                orden.put("compradorNombre", "Comprador desconocido");
                            if (!orden.containsKey("subtotal"))
                                orden.put("subtotal", 0.0);
                            listaOrdenes.add(orden);
                        }
                    }

                    ordenAdapter.notifyDataSetChanged();

                    int total = listaOrdenes.size();
                    tvOrdenesCount.setText(total == 1 ? "1 orden" : total + " órdenes");
                })
                .addOnFailureListener(e -> {
                    tvOrdenesCount.setText("Error al cargar");
                    Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
