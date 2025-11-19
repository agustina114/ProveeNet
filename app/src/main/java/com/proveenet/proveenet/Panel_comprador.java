package com.proveenet.proveenet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class Panel_comprador extends BaseActivity {

    private BottomNavigationView bottomNavigationView;
    private ImageButton btnLogout, btnNotifications, btnMenu;
    private TextView tvUserName, tvWelcome, tvProveedoresCount, tvProductosCount, tvComprasCount, tvTotalGastado;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    // Listeners para tiempo real
    private ListenerRegistration proveedoresListener;
    private ListenerRegistration productosListener;
    private ListenerRegistration ordenesListener;
    private ListenerRegistration nombreListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panel_comprador);

        //  Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();

        //  Vistas
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

        // Cargar datos en tiempo real
        escucharNombreUsuario();
        escucharEstadisticas();

        // 👇 [NUEVO] Link al Perfil (Clic en el nombre o menú)
        tvUserName.setOnClickListener(v -> irAPerfil());
        btnMenu.setOnClickListener(v -> irAPerfil());

        // Cerrar sesión
        btnLogout.setOnClickListener(v -> {
            apagarListeners();
            auth.signOut();
            Toast.makeText(this, "👋 Sesión cerrada", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Panel_comprador.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        //  Navegación inferior
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_inicio) {
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
            } else if (id == R.id.nav_perfil) {
            startActivity(new Intent(this, Perfil_comprador.class));
            overridePendingTransition(0, 0);
            finish();
            return true;
        }
            return false;
        });

        // Tarjetas de navegación
        CardView cardProveedores = findViewById(R.id.cardProveedores);
        CardView cardProductos = findViewById(R.id.cardProductos);
        CardView cardNuevaCompra = findViewById(R.id.cardNuevaCompra);

        cardProveedores.setOnClickListener(v -> {
            startActivity(new Intent(Panel_comprador.this, Proveedores.class));
            overridePendingTransition(0, 0);
            finish(); // Recomendable añadir finish si quieres navegación lineal
        });
        cardProductos.setOnClickListener(v -> {
            startActivity(new Intent(Panel_comprador.this, Productos.class));
            overridePendingTransition(0, 0);
            finish();
        });
        cardNuevaCompra.setOnClickListener(v -> {
            startActivity(new Intent(Panel_comprador.this, MiCarrito.class));
            overridePendingTransition(0, 0);
            finish();
        });
    }

    // ======================================================
    // 👇 MÉTODO NUEVO para ir al Perfil
    private void irAPerfil() {
        startActivity(new Intent(Panel_comprador.this, Perfil_comprador.class));
        overridePendingTransition(0, 0); // Sin animación
        finish(); // Cerramos panel para no acumular pantallas
    }

    // ======================================================
    private void escucharNombreUsuario() {
        nombreListener = db.collection("compradores")
                .document(user.getUid())
                .addSnapshotListener((document, e) -> {
                    if (e != null) {
                        tvUserName.setText("Usuario");
                        return;
                    }

                    if (document != null && document.exists()) {
                        String nombre = document.getString("nombre");
                        if (nombre != null && !nombre.isEmpty()) {
                            tvUserName.setText(nombre);
                            tvWelcome.setText("Bienvenido, " + nombre);
                        } else {
                            tvUserName.setText("Usuario");
                        }
                    } else {
                        tvUserName.setText("Usuario");
                    }
                });
    }

    // ======================================================
    private void escucharEstadisticas() {

        // Total de proveedores
        proveedoresListener = db.collection("proveedores")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) {
                        tvProveedoresCount.setText("0");
                        return;
                    }
                    tvProveedoresCount.setText(String.valueOf(snapshot.size()));
                });

        // Total de productos activos
        productosListener = db.collection("productos")
                .whereEqualTo("estado", "activo")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) {
                        tvProductosCount.setText("0");
                        return;
                    }
                    tvProductosCount.setText(String.valueOf(snapshot.size()));
                });

        // Total de compras y gasto del comprador actual
        ordenesListener = db.collection("ordenes")
                .whereEqualTo("compradorId", user.getUid())
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) {
                        tvComprasCount.setText("0");
                        tvTotalGastado.setText("$0");
                        return;
                    }

                    int cantidadCompras = snapshot.size();
                    double totalGastado = 0.0;

                    for (QueryDocumentSnapshot doc : snapshot) {
                        Object subtotalObj = doc.get("subtotal");
                        if (subtotalObj instanceof Number) {
                            totalGastado += ((Number) subtotalObj).doubleValue();
                        } else {
                            try {
                                totalGastado += Double.parseDouble(String.valueOf(subtotalObj));
                            } catch (NumberFormatException ignored) {}
                        }
                    }

                    tvComprasCount.setText(String.valueOf(cantidadCompras));
                    tvTotalGastado.setText("$" + String.format("%.0f", totalGastado));
                });
    }

    // ======================================================
    private void apagarListeners() {
        if (nombreListener != null) {
            nombreListener.remove();
        }
        if (proveedoresListener != null) {
            proveedoresListener.remove();
        }
        if (productosListener != null) {
            productosListener.remove();
        }
        if (ordenesListener != null) {
            ordenesListener.remove();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        apagarListeners();
    }
}