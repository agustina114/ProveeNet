package com.proveenet.proveenet;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class Panel_comprador extends BaseActivity {

    private BottomNavigationView bottomNavigationView;
    private ImageButton btnLogout, btnNotifications, btnMenu;
    private TextView tvUserName, tvWelcome, tvProveedoresCount, tvProductosCount, tvComprasCount, tvTotalGastado;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panel_comprador);

        // 🔹 Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();

        bottomNavigationView = findViewById(R.id.bottomNavigation);
        btnLogout = findViewById(R.id.btnLogout);
        btnNotifications = findViewById(R.id.btnNotifications);
        btnMenu = findViewById(R.id.btnMenu);

        tvUserName = findViewById(R.id.tvUserName);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvProveedoresCount = findViewById(R.id.tvProveedoresCount);
        tvProductosCount = findViewById(R.id.tvProductosCount);
        tvComprasCount = findViewById(R.id.tvComprasCount);
        tvTotalGastado = findViewById(R.id.tvTotalGastado);

        bottomNavigationView.setSelectedItemId(R.id.nav_inicio);

        if (user == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        cargarNombreUsuario();

        cargarEstadisticas();

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(Panel_comprador.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // 🔹 Navegación inferior
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_inicio) {
                return true;
            } else if (id == R.id.nav_proveedores) {
                startActivity(new Intent(this, Proveedores.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_productos) {
                startActivity(new Intent(this, Productos.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_carrito) {
                startActivity(new Intent(this, MiCarrito.class));
                overridePendingTransition(0, 0);
                return true;
            }

            return false;
        });
    }


    // ======================================================
    private void cargarNombreUsuario() {
        db.collection("compradores")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String nombre = document.getString("nombre");
                        if (nombre != null && !nombre.isEmpty()) {
                            tvUserName.setText(nombre);
                            tvWelcome.setText("Bienvenido, " + nombre);
                        } else {
                            tvUserName.setText("Usuario");
                        }
                    }
                })
                .addOnFailureListener(e -> tvUserName.setText("Usuario"));
    }


    // ======================================================
    private void cargarEstadisticas() {
        // 🧩 Total de proveedores
        db.collection("proveedores")
                .get()
                .addOnSuccessListener(snapshot -> tvProveedoresCount.setText(String.valueOf(snapshot.size())))
                .addOnFailureListener(e -> tvProveedoresCount.setText("0"));

        // 🧩 Total de productos activos
        db.collection("productos")
                .whereEqualTo("estado", "activo")
                .get()
                .addOnSuccessListener(snapshot -> tvProductosCount.setText(String.valueOf(snapshot.size())))
                .addOnFailureListener(e -> tvProductosCount.setText("0"));

        // 🧩 Total de compras y gasto del comprador actual
        db.collection("ordenes")
                .whereEqualTo("compradorId", user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    int cantidadCompras = snapshot.size();
                    double totalGastado = 0.0;

                    for (QueryDocumentSnapshot doc : snapshot) {
                        Object subtotalObj = doc.get("subtotal");
                        if (subtotalObj != null) {
                            try {
                                totalGastado += Double.parseDouble(subtotalObj.toString());
                            } catch (NumberFormatException ignored) {}
                        }
                    }

                    tvComprasCount.setText(String.valueOf(cantidadCompras));
                    tvTotalGastado.setText("$" + String.format("%.0f", totalGastado));
                })
                .addOnFailureListener(e -> {
                    tvComprasCount.setText("0");
                    tvTotalGastado.setText("$0");
                });
    }
}
