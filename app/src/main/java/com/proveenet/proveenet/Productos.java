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

public class Productos extends BaseActivity {

    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LinearLayout llListaProductos;
    private TextView tvProductosCount, tvUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_productos);

        // 🔹 Inicializar Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        llListaProductos = findViewById(R.id.llListaProductos);
        tvProductosCount = findViewById(R.id.tvProductosCount);
        tvUserName = findViewById(R.id.tvUserName);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        bottomNavigationView.setSelectedItemId(R.id.nav_productos);

        mostrarNombreUsuario();

        cargarProductosDisponibles();

        setupBottomNavigation();
    }


    // ======================================================
    private void mostrarNombreUsuario() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            tvUserName.setText("Usuario desconocido");
            return;
        }

        String uid = user.getUid();

        // Primero busca en compradores
        db.collection("compradores").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String nombre = document.getString("nombre");
                        if (nombre != null && !nombre.isEmpty()) {
                            tvUserName.setText(nombre);
                        } else {
                            tvUserName.setText("Sin nombre");
                        }
                    } else {
                        // Si no está en compradores, buscar en proveedores
                        db.collection("proveedores").document(uid).get()
                                .addOnSuccessListener(docProv -> {
                                    if (docProv.exists()) {
                                        String nombre = docProv.getString("nombre");
                                        if (nombre != null && !nombre.isEmpty()) {
                                            tvUserName.setText(nombre);
                                        } else {
                                            tvUserName.setText("Sin nombre");
                                        }
                                    } else {
                                        tvUserName.setText("Usuario desconocido");
                                    }
                                })
                                .addOnFailureListener(e -> tvUserName.setText("Error al cargar nombre"));
                    }
                })
                .addOnFailureListener(e -> tvUserName.setText("Error al cargar nombre"));
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
    private void cargarProductosDisponibles() {
        db.collection("productos")
                .whereEqualTo("estado", "activo")
                .get()
                .addOnSuccessListener(this::mostrarProductos)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error al cargar productos", Toast.LENGTH_LONG).show());
    }


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
            String proveedorId = doc.getString("proveedorId");

            Object precioObj = doc.get("precio");
            double precio = 0.0;
            String precioTxt = "0";

            if (precioObj != null) {
                if (precioObj instanceof Number) {
                    precio = ((Number) precioObj).doubleValue();
                    precioTxt = String.format("%.0f", precio);
                } else if (precioObj instanceof String) {
                    try {
                        precio = Double.parseDouble(((String) precioObj).replaceAll("[^0-9.]", ""));
                        precioTxt = String.valueOf(precio);
                    } catch (Exception ignored) {}
                }
            }

            Object stockObj = doc.get("stock");
            String stockTxt = "0";
            if (stockObj != null) {
                if (stockObj instanceof Number) {
                    stockTxt = ((Number) stockObj).toString();
                } else if (stockObj instanceof String) {
                    stockTxt = (String) stockObj;
                }
            }
            // =============================================================

            // 🔹 Inflar card
            View cardView = LayoutInflater.from(this)
                    .inflate(R.layout.item_producto_publico, llListaProductos, false);

            ((TextView) cardView.findViewById(R.id.tvNombreProducto)).setText(nombre != null ? nombre : "Sin nombre");
            ((TextView) cardView.findViewById(R.id.tvPrecio)).setText("$" + precioTxt);
            ((TextView) cardView.findViewById(R.id.tvProveedor)).setText(proveedor != null ? proveedor : "Desconocido");
            ((TextView) cardView.findViewById(R.id.tvStock)).setText(stockTxt + " disponibles");
            ((TextView) cardView.findViewById(R.id.tvCategoria)).setText(categoria != null ? categoria : "Sin categoría");
            ((TextView) cardView.findViewById(R.id.tvDescripcion)).setText(descripcion != null ? descripcion : "Sin descripción");

            Button btnAgregar = cardView.findViewById(R.id.btnAgregarCotizacion);
            double finalPrecio = precio;
            String finalProveedorId = proveedorId;

            btnAgregar.setOnClickListener(v ->
                    agregarAlCarrito(codigo, nombre, proveedor, finalProveedorId, finalPrecio));

            llListaProductos.addView(cardView);
            count++;
        }

        tvProductosCount.setText(count + " disponibles");
    }


    // ======================================================
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
                        Map<String, Object> item = new HashMap<>();
                        item.put("nombre", nombre);
                        item.put("proveedor", proveedor);
                        item.put("proveedorId", proveedorId);
                        item.put("precio", precio);
                        item.put("cantidad", 1L);
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
