package com.proveenet.proveenet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class Productos extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LinearLayout llListaProductos;
    private TextView tvProductosCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_productos);

        // 🔹 Inicializar Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 🔹 Referencias UI
        llListaProductos = findViewById(R.id.llListaProductos);
        tvProductosCount = findViewById(R.id.tvProductosCount);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // 🔹 Selecciona "Productos" en la barra inferior
        bottomNavigationView.setSelectedItemId(R.id.nav_productos);

        // 🔹 Carga productos disponibles
        cargarProductosDisponibles();

        // 🔹 Configura navegación inferior
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, Panel_comprador.class));
                finish();
                return true;
            } else if (id == R.id.nav_proveedores) {
                startActivity(new Intent(this, Proveedores.class));
                finish();
                return true;
            } else if (id == R.id.nav_productos) {
                return true;
            } else if (id == R.id.nav_carrito) {
                startActivity(new Intent(this, MiCarrito.class));
                finish();
                return true;
            }
            return false;
        });
    }

    // ======================================================
    // 📦 Cargar todos los productos activos
    // ======================================================
    private void cargarProductosDisponibles() {
        db.collection("productos")
                .whereEqualTo("estado", "activo")
                .get()
                .addOnSuccessListener(this::mostrarProductos)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error al cargar productos", Toast.LENGTH_LONG).show());
    }

    // ======================================================
    // 🧱 Mostrar productos en cards
    // ======================================================
    private void mostrarProductos(QuerySnapshot snapshot) {
        llListaProductos.removeAllViews();
        int count = 0;

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            String codigo = doc.getId();
            String nombre = doc.getString("nombre");
            String categoria = doc.getString("categoria");
            String descripcion = doc.getString("descripcion");
            String proveedor = doc.getString("proveedorNombre");
            String proveedorId = doc.getString("proveedorId"); // <-- ¡¡CORREGIDO!! Esta línea faltaba

            // ======== 🧩 ARREGLO UNIVERSAL PARA CAMPOS FIRESTORE ========
            // Precio
            Object precioObjeto = doc.get("precio");
            double precioDouble = 0.0;
            String precioTexto = "0";

            if (precioObjeto != null) {
                if (precioObjeto instanceof Number) {
                    precioDouble = ((Number) precioObjeto).doubleValue();
                    precioTexto = String.format("%.0f", precioDouble);
                } else if (precioObjeto instanceof String) {
                    String limpio = ((String) precioObjeto).replaceAll("[^0-9]", "");
                    if (!limpio.isEmpty()) {
                        try {
                            precioDouble = Double.parseDouble(limpio);
                            precioTexto = limpio;
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            // Stock
            Object stockObjeto = doc.get("stock");
            String stockTexto = "0";
            if (stockObjeto != null) {
                if (stockObjeto instanceof Number) {
                    stockTexto = ((Number) stockObjeto).toString();
                } else if (stockObjeto instanceof String) {
                    stockTexto = (String) stockObjeto;
                }
            }
            // ============================================================

            // 🔹 Inflar card de producto
            View cardView = LayoutInflater.from(this)
                    .inflate(R.layout.item_producto_publico, llListaProductos, false);

            ((TextView) cardView.findViewById(R.id.tvNombreProducto))
                    .setText(nombre != null ? nombre : "Sin nombre");
            ((TextView) cardView.findViewById(R.id.tvPrecio))
                    .setText("$" + precioTexto);
            ((TextView) cardView.findViewById(R.id.tvProveedor))
                    .setText(proveedor != null ? proveedor : "Desconocido");
            ((TextView) cardView.findViewById(R.id.tvStock))
                    .setText(stockTexto + " disponibles");
            ((TextView) cardView.findViewById(R.id.tvCategoria))
                    .setText(categoria != null ? categoria : "Sin categoría");
            ((TextView) cardView.findViewById(R.id.tvDescripcion))
                    .setText(descripcion != null ? descripcion : "Sin descripción");

            Button btnAgregar = cardView.findViewById(R.id.btnAgregarCotizacion);

            double finalPrecioDouble = precioDouble;
            String finalProveedorId = proveedorId; // <-- ¡¡CORREGIDO!!

            btnAgregar.setOnClickListener(v ->
                    // <-- ¡¡CORREGIDO!! Pasamos 5 parámetros
                    agregarAlCarrito(codigo, nombre, proveedor, finalProveedorId, finalPrecioDouble));

            llListaProductos.addView(cardView);
            count++;
        }

        tvProductosCount.setText(count + " disponibles");
    }

    // ======================================================
    // 🛒 Agregar producto al carrito
    // ======================================================
    // <-- ¡¡CORREGIDO!! Recibimos 5 parámetros
    private void agregarAlCarrito(String productoId, String nombre, String proveedor, String proveedorId, double precio) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "⚠️ Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();

        db.collection("carritos")
                .document(userId)
                .collection("items")
                .document(productoId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Ya existe: incrementar cantidad
                        Long cantidadActual = doc.getLong("cantidad");
                        if (cantidadActual == null) cantidadActual = 0L;

                        db.collection("carritos")
                                .document(userId)
                                .collection("items")
                                .document(productoId)
                                .update("cantidad", cantidadActual + 1)
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(this, "🛒 Cantidad actualizada", Toast.LENGTH_SHORT).show());
                    } else {
                        // Nuevo producto
                        Map<String, Object> item = new HashMap<>();
                        item.put("nombre", nombre);
                        item.put("proveedor", proveedor);
                        item.put("proveedorId", proveedorId); // <-- ¡¡CORREGIDO!! Esta línea faltaba
                        item.put("precio", precio);   // ✅ Double puro
                        item.put("cantidad", 1L);     // ✅ Long
                        item.put("productoId", productoId);

                        db.collection("carritos")
                                .document(userId)
                                .collection("items")
                                .document(productoId)
                                .set(item)
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(this, "✅ Agregado al carrito: " + nombre, Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "❌ Error al agregar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                });
    }
}