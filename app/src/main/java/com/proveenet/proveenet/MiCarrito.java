package com.proveenet.proveenet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class MiCarrito extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // --- Vistas ---
    private LinearLayout llCarrito, llCarritoVacio, llResumen;
    private ScrollView scrollCarrito;
    private TextView tvTotalProductos, tvSubtotal, tvTotal;
    private ImageButton btnBack, btnVaciarCarrito;
    private Button btnFinalizarCompra, btnExplorarProductos;
    private BottomNavigationView bottomNavigationView;

    // --- Variables Globales ---
    private double subtotalGlobal = 0.0;
    private List<Map<String, Object>> itemsActuales = new ArrayList<>();
    private ListenerRegistration carritoListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mi_carrito);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // --- Vistas ---
        llCarrito = findViewById(R.id.llCarrito);
        llCarritoVacio = findViewById(R.id.llCarritoVacio);
        llResumen = findViewById(R.id.llResumen);
        scrollCarrito = findViewById(R.id.scrollCarrito);
        tvTotalProductos = findViewById(R.id.tvTotalProductos);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvTotal = findViewById(R.id.tvTotal);

        // --- ARREGLO ---
        btnFinalizarCompra = findViewById(R.id.btnCotizar);

        btnVaciarCarrito = findViewById(R.id.btnVaciarCarrito);
        btnExplorarProductos = findViewById(R.id.btnExplorarProductos);
        btnBack = findViewById(R.id.btnBack);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "⚠️ Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bottomNavigationView.setSelectedItemId(R.id.nav_carrito);

        cargarCarrito();

        // --- Listeners ---
        btnBack.setOnClickListener(v -> finish());
        btnExplorarProductos.setOnClickListener(v -> {
            startActivity(new Intent(this, Productos.class));
            finish();
        });
        btnVaciarCarrito.setOnClickListener(v -> vaciarCarrito());
        btnFinalizarCompra.setOnClickListener(v -> confirmarFinalizacion());

        // --- Navegación ---
        // --- ¡ARREGLO! Navegación completada
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, Panel_comprador.class));
                finish();
                return true;
            } else if (id == R.id.nav_productos) {
                startActivity(new Intent(this, Productos.class));
                finish();
                return true;
            } else if (id == R.id.nav_proveedores) { // Añadido
                startActivity(new Intent(this, Proveedores.class));
                finish();
                return true;

            } else if (id == R.id.nav_carrito) {
                return true; // Ya estamos aquí
            }
            return false;
        });
    }

    private void cargarCarrito() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        carritoListener = db.collection("carritos")
                .document(user.getUid())
                .collection("items")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Error al cargar carrito", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        mostrarCarritoVacio();
                        return;
                    }

                    mostrarCarritoConProductos(snapshots);
                });
    }

    private void mostrarCarritoConProductos(QuerySnapshot snapshots) {
        scrollCarrito.setVisibility(View.VISIBLE);
        llResumen.setVisibility(View.VISIBLE);
        llCarritoVacio.setVisibility(View.GONE);

        llCarrito.removeAllViews();
        subtotalGlobal = 0.0;
        int totalItems = 0;
        itemsActuales.clear();

        for (DocumentSnapshot doc : snapshots) {
            String nombre = doc.getString("nombre");
            String proveedor = doc.getString("proveedor");
            String productoId = doc.getId();
            String proveedorId = doc.getString("proveedorId"); // <-- ¡ARREGLO! Leer el ID

            Double precio = 0.0;
            if (doc.contains("precio") && doc.get("precio") instanceof Number)
                precio = doc.getDouble("precio");

            Long cantidad = 1L;
            if (doc.contains("cantidad") && doc.get("cantidad") instanceof Number)
                cantidad = doc.getLong("cantidad");

            if (precio == null) precio = 0.0;
            if (cantidad == null) cantidad = 1L;

            double subtotalItem = precio * cantidad;
            subtotalGlobal += subtotalItem;
            totalItems += cantidad;

            // Guardar item
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("productoId", productoId);
            itemMap.put("nombre", nombre);
            itemMap.put("proveedor", proveedor);
            itemMap.put("proveedorId", proveedorId); // <-- ¡ARREGLO! Guardar el ID
            itemMap.put("precio", precio);
            itemMap.put("cantidad", cantidad);
            itemsActuales.add(itemMap);

            View itemView = LayoutInflater.from(this).inflate(R.layout.item_carrito, llCarrito, false);

            ((TextView) itemView.findViewById(R.id.tvNombreCarrito)).setText(nombre);
            ((TextView) itemView.findViewById(R.id.tvProveedorCarrito)).setText(proveedor);
            ((TextView) itemView.findViewById(R.id.tvPrecioCarrito)).setText("$" + String.format("%.0f", precio));
            ((TextView) itemView.findViewById(R.id.tvCantidadCarrito)).setText(String.valueOf(cantidad));
            ((TextView) itemView.findViewById(R.id.tvSubtotalCarrito)).setText("$" + String.format("%.0f", subtotalItem));

            Button btnMenos = itemView.findViewById(R.id.btnMenos);
            Button btnMas = itemView.findViewById(R.id.btnMas);
            Button btnEliminar = itemView.findViewById(R.id.btnEliminarCarrito);

            long cantidadActual = cantidad;

            btnMenos.setOnClickListener(v -> {
                if (cantidadActual > 1) actualizarCantidad(productoId, cantidadActual - 1);
            });

            btnMas.setOnClickListener(v -> actualizarCantidad(productoId, cantidadActual + 1));
            btnEliminar.setOnClickListener(v -> eliminarDelCarrito(productoId));

            llCarrito.addView(itemView);
        }

        tvTotalProductos.setText(totalItems + " productos");
        tvSubtotal.setText("$" + String.format("%.0f", subtotalGlobal));
        tvTotal.setText("$" + String.format("%.0f", subtotalGlobal));
    }

    private void actualizarCantidad(String productoId, long nuevaCantidad) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("carritos")
                .document(user.getUid())
                .collection("items")
                .document(productoId)
                .update("cantidad", nuevaCantidad)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show());
    }

    private void eliminarDelCarrito(String productoId) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Eliminar producto")
                .setMessage("¿Deseas quitar este producto del carrito?")
                .setPositiveButton("Sí", (d, w) -> {
                    db.collection("carritos")
                            .document(user.getUid())
                            .collection("items")
                            .document(productoId)
                            .delete()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(this, "🗑️ Producto eliminado", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void vaciarCarrito() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Vaciar carrito")
                .setMessage("¿Seguro que deseas vaciar todo el carrito?")
                .setPositiveButton("Sí", (d, w) -> {
                    WriteBatch batch = db.batch();
                    db.collection("carritos")
                            .document(user.getUid())
                            .collection("items")
                            .get()
                            .addOnSuccessListener(snapshots -> {
                                if (snapshots.isEmpty()) return;
                                for (DocumentSnapshot doc : snapshots) {
                                    batch.delete(doc.getReference());
                                }
                                batch.commit()
                                        .addOnSuccessListener(aVoid ->
                                                Toast.makeText(this, "🧹 Carrito vaciado", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Error al vaciar", Toast.LENGTH_SHORT).show());
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ==========================================================
    // 🧾 CONFIRMACIÓN Y CREACIÓN DE ORDEN
    // ==========================================================
    private void confirmarFinalizacion() {
        if (itemsActuales.isEmpty()) {
            Toast.makeText(this, "El carrito está vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Finalizar compra")
                .setMessage("¿Deseas confirmar la compra y generar la orden?")
                .setPositiveButton("Sí", (dialog, which) -> crearOrden())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void crearOrden() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String compradorId = user.getUid();
        String compradorNombre = user.getDisplayName() != null ? user.getDisplayName() : (user.getEmail() != null ? user.getEmail() : "Usuario Anónimo");

        // --- ¡ARREGLO! Usar un WriteBatch ---
        WriteBatch batch = db.batch();

        for (Map<String, Object> item : itemsActuales) {
            String productoId = (String) item.get("productoId");
            String productoNombre = (String) item.get("nombre");
            String proveedorNombre = (String) item.get("proveedor");
            String proveedorId = (String) item.get("proveedorId"); // <-- ¡ARREGLO! Leer el ID
            double precioUnitario = (double) item.get("precio");
            long cantidad = (long) item.get("cantidad");
            double subtotal = precioUnitario * cantidad;

            Map<String, Object> orden = new HashMap<>();
            orden.put("compradorId", compradorId);
            orden.put("compradorNombre", compradorNombre);
            orden.put("productoId", productoId);
            orden.put("productoNombre", productoNombre);
            orden.put("proveedorNombre", proveedorNombre);
            orden.put("proveedorId", proveedorId); // <-- ¡ARREGLO! Guardar el ID

            // --- ¡ARREGLO! Guardar números como NÚMEROS ---
            // Tu DashboardProveedor.java lo necesita para sumar
            orden.put("cantidad", cantidad);
            orden.put("precioUnitario", precioUnitario);
            orden.put("subtotal", subtotal);

            orden.put("fechaCreacion", FieldValue.serverTimestamp()); // Es más confiable
            orden.put("estado", "pendiente");
            orden.put("confirmacionProveedor", "pendiente");

            // Añadir esta orden al batch
            DocumentReference ordenRef = db.collection("ordenes").document();
            batch.set(ordenRef, orden);
        }

        // Ejecutar todas las operaciones a la vez
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Orden generada correctamente", Toast.LENGTH_SHORT).show();
                    vaciarCarritoSilencioso(); // Vaciar carrito SÓLO si todo salió bien
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Error al crear orden", Toast.LENGTH_SHORT).show();
                });
    }

    private void vaciarCarritoSilencioso() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        WriteBatch batch = db.batch();
        db.collection("carritos")
                .document(user.getUid())
                .collection("items")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) return;
                    for (DocumentSnapshot doc : snapshots) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit();
                });
    }

    private void mostrarCarritoVacio() {
        scrollCarrito.setVisibility(View.GONE);
        llResumen.setVisibility(View.GONE);
        llCarritoVacio.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (carritoListener != null) carritoListener.remove();
    }
}