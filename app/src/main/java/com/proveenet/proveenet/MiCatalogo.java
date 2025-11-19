package com.proveenet.proveenet;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton; // 👈 IMPORTANTE: Asegúrate que esté
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class MiCatalogo extends BaseActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout llProductos, llProductosRecientes;
    private Button btnAgregarProducto;
    private TextView tvProductosCount;
    private TextView tvNombreEmpresa;

    // 👇 (No es necesario declarar btnLogout aquí si solo se usa en onCreate)

    // 👇 1. Declarar las variables para los Listeners
    private ListenerRegistration productosListener;
    private ListenerRegistration productosRecientesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mi_catalogo);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        llProductos = findViewById(R.id.llProductos);
        llProductosRecientes = findViewById(R.id.llProductosRecientes);
        btnAgregarProducto = findViewById(R.id.btnAgregarProducto);
        tvProductosCount = findViewById(R.id.tvProductosCount);
        tvNombreEmpresa = findViewById(R.id.tvNombreEmpresa);

        // ==========================================================
        // 👇 === ¡AQUÍ ESTÁ EL ARREGLO! === 👇
        // ==========================================================
        ImageButton btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {

            // 1. Detenemos los listeners de ESTA PANTALLA
            detenerListeners();

            // 2. Cerramos la sesión
            auth.signOut();

            // 3. Mostramos mensaje y vamos al inicio
            Toast.makeText(this, "👋 Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        // ==========================================================


        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "⚠️ Debes iniciar sesión primero", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        cargarDatosProveedor(user);
        escucharProductosEnTiempoReal();
        escucharProductosRecientes();

        btnAgregarProducto.setOnClickListener(v -> mostrarModalAgregarProducto());

        // 🔹 Barra de navegación inferior
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_catalogo);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, DashboardProveedor.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }

            if (id == R.id.nav_catalogo) return true;

            if (id == R.id.nav_ordenes) {
                startActivity(new Intent(this, MisOrdenes.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }  if (id == R.id.nav_perfil) {
                startActivity(new Intent(this, Perfil_proveedor.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }

            return false;
        });
    }

    private void cargarDatosProveedor(FirebaseUser user) {
        if (user == null) return;

        tvNombreEmpresa.setText("Cargando...");

        db.collection("proveedores").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nombre = documentSnapshot.getString("empresa");
                        if (nombre != null && !nombre.isEmpty()) {
                            tvNombreEmpresa.setText(nombre);
                        } else {
                            tvNombreEmpresa.setText("Empresa sin nombre");
                        }
                    } else {
                        tvNombreEmpresa.setText("Perfil no encontrado");
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return; // Evitar crash si se cierra sesión
                    tvNombreEmpresa.setText("Error al cargar");
                    Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void mostrarModalAgregarProducto() {
        // ... (Tu método está bien, no se necesita cambiar)
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "⚠️ Sesión expirada. Inicia sesión nuevamente.", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.modal_agregar_producto);
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etCodigo = dialog.findViewById(R.id.etCodigo);
        EditText etNombre = dialog.findViewById(R.id.etNombre);
        EditText etCategoria = dialog.findViewById(R.id.etCategoria);
        EditText etDescripcion = dialog.findViewById(R.id.etDescripcion);
        EditText etPrecio = dialog.findViewById(R.id.etPrecio);
        EditText etStock = dialog.findViewById(R.id.etStock);
        Button btnGuardar = dialog.findViewById(R.id.btnGuardar);
        Button btnCancelar = dialog.findViewById(R.id.btnCancelar);

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnGuardar.setOnClickListener(v -> {
            String uid = user.getUid();
            String codigo = etCodigo.getText().toString().trim().toUpperCase();
            String nombre = etNombre.getText().toString().trim();
            String categoria = etCategoria.getText().toString().trim();
            String descripcion = etDescripcion.getText().toString().trim();

            long precio = 0;
            long stock = 0;
            try {
                precio = Long.parseLong(etPrecio.getText().toString().trim());
                stock = Long.parseLong(etStock.getText().toString().trim());
            } catch (NumberFormatException e) {
                Toast.makeText(this, "⚠️ Ingresa valores numéricos válidos para precio y stock", Toast.LENGTH_SHORT).show();
                return;
            }

            if (codigo.isEmpty() || nombre.isEmpty()) {
                Toast.makeText(this, "⚠️ Completa los campos obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            String nombreProveedor = tvNombreEmpresa.getText().toString();
            if (nombreProveedor.equals("Cargando...") || nombreProveedor.equals("Error al cargar")) {
                nombreProveedor = "Proveedor";
            }

            Map<String, Object> producto = new HashMap<>();
            producto.put("codigo", codigo);
            producto.put("nombre", nombre);
            producto.put("categoria", categoria);
            producto.put("descripcion", descripcion);
            producto.put("precio", precio);
            producto.put("stock", stock);
            producto.put("estado", "activo");
            producto.put("proveedorId", uid);
            producto.put("proveedorNombre", nombreProveedor);

            db.collection("productos").document(codigo)
                    .get()
                    .addOnSuccessListener(existing -> {
                        if (existing.exists()) {
                            Toast.makeText(this, "⚠️ Ya existe un producto con ese código", Toast.LENGTH_LONG).show();
                        } else {
                            db.collection("productos").document(codigo)
                                    .set(producto)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "✅ Producto agregado correctamente", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "❌ Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error al verificar producto: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        dialog.show();
    }


    private void escucharProductosEnTiempoReal() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        productosListener = db.collection("productos")
                .whereEqualTo("proveedorId", user.getUid())
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) { // Quitamos el toast de error al cerrar sesión
                        return;
                    }
                    if (snapshots == null) return;


                    llProductos.removeAllViews();
                    int count = 0;

                    for (DocumentSnapshot doc : snapshots) {
                        View card = LayoutInflater.from(this).inflate(R.layout.item_producto_card, llProductos, false);
                        llenarCardProducto(doc, card);
                        llProductos.addView(card);
                        count++;
                    }

                    if (count == 0)
                        tvProductosCount.setText("No hay productos");
                    else if (count == 1)
                        tvProductosCount.setText("1 producto");
                    else
                        tvProductosCount.setText(count + " productos");
                });
    }


    private void escucharProductosRecientes() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        productosRecientesListener = db.collection("productos")
                .whereEqualTo("proveedorId", user.getUid())
                .orderBy("codigo", Query.Direction.DESCENDING)
                .limit(2)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    llProductosRecientes.removeAllViews();

                    for (DocumentSnapshot doc : snapshots) {
                        View card = LayoutInflater.from(this).inflate(R.layout.item_producto_card, llProductosRecientes, false);
                        llenarCardProducto(doc, card);
                        llProductosRecientes.addView(card);
                    }
                });
    }


    private void llenarCardProducto(DocumentSnapshot doc, View card) {
        // ... (Tu método está bien, no se necesita cambiar)
        String nombre = doc.getString("nombre");
        String codigo = doc.getString("codigo");
        String categoria = doc.getString("categoria");
        String descripcion = doc.getString("descripcion");


        Object precioObj = doc.get("precio");
        String precio;
        if (precioObj instanceof Number) {
            precio = String.valueOf(((Number) precioObj).longValue());
        } else {
            precio = String.valueOf(precioObj != null ? precioObj : "0");
        }

        Object stockObj = doc.get("stock");
        String stock;
        if (stockObj instanceof Number) {
            stock = String.valueOf(((Number) stockObj).longValue());
        } else {
            stock = String.valueOf(stockObj != null ? stockObj : "0");
        }

        String estado = doc.getString("estado");
        String proveedorNombre = doc.getString("proveedorNombre");

        ((TextView) card.findViewById(R.id.tvNombre)).setText(nombre != null ? nombre : "Sin nombre");
        ((TextView) card.findViewById(R.id.tvCodigo)).setText("Código: " + (codigo != null ? codigo : "-"));
        ((TextView) card.findViewById(R.id.tvCategoria)).setText("Categoría: " + (categoria != null ? categoria : "Sin categoría"));
        ((TextView) card.findViewById(R.id.tvDescripcion)).setText(descripcion != null ? descripcion : "Sin descripción");
        ((TextView) card.findViewById(R.id.tvPrecio)).setText("$" + precio);
        ((TextView) card.findViewById(R.id.tvStock)).setText(stock + " unidades");

        TextView tvEstado = card.findViewById(R.id.tvEstado);
        TextView tvProveedor = card.findViewById(R.id.tvProveedor);

        if (estado != null && estado.trim().equalsIgnoreCase("activo")) {
            tvEstado.setText("Estado: Activo");
            tvEstado.setTextColor(Color.parseColor("#2E7D32"));
            tvEstado.setBackgroundResource(R.drawable.badge_estado_activo);
        } else {
            tvEstado.setText("Estado: Inactivo");
            tvEstado.setTextColor(Color.parseColor("#757575"));
            tvEstado.setBackgroundResource(R.drawable.badge_estado_inactivo);
        }

        tvProveedor.setText("Proveedor: " + (proveedorNombre != null ? proveedorNombre : "Desconocido"));

        card.findViewById(R.id.btnEliminar).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Eliminar producto")
                    .setMessage("¿Seguro que deseas eliminar este producto?")
                    .setPositiveButton("Sí", (d, w) ->
                            db.collection("productos").document(doc.getId())
                                    .delete()
                                    .addOnSuccessListener(x -> {
                                        // llProductos.removeView(card); // Quitado para evitar bugs
                                        Toast.makeText(this, "🗑️ Producto eliminado", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(ex ->
                                            Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_LONG).show())
                    )
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
        card.findViewById(R.id.btnEditar).setOnClickListener(v -> {
            mostrarModalEditarProducto(doc);
        });
    }

    private void mostrarModalEditarProducto(DocumentSnapshot doc) {
        // ... (Tu método está bien, no se necesita cambiar)
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.modal_editar_producto);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etNombreEdit = dialog.findViewById(R.id.etNombreEdit);
        EditText etCategoriaEdit = dialog.findViewById(R.id.etCategoriaEdit);
        EditText etDescripcionEdit = dialog.findViewById(R.id.etDescripcionEdit);
        EditText etPrecioEdit = dialog.findViewById(R.id.etPrecioEdit);
        EditText etStockEdit = dialog.findViewById(R.id.etStockEdit);
        Button btnGuardarEdit = dialog.findViewById(R.id.btnGuardarEdit);
        Button btnCancelarEdit = dialog.findViewById(R.id.btnCancelarEdit);

        etNombreEdit.setText(doc.getString("nombre"));
        etCategoriaEdit.setText(doc.getString("categoria"));
        etDescripcionEdit.setText(doc.getString("descripcion"));
        etPrecioEdit.setText(String.valueOf(doc.get("precio")));
        etStockEdit.setText(String.valueOf(doc.get("stock")));

        btnCancelarEdit.setOnClickListener(v -> dialog.dismiss());

        btnGuardarEdit.setOnClickListener(v -> {
            String nombre = etNombreEdit.getText().toString().trim();
            String categoria = etCategoriaEdit.getText().toString().trim();
            String descripcion = etDescripcionEdit.getText().toString().trim();
            long precio = 0;
            long stock = 0;

            try {
                precio = Long.parseLong(etPrecioEdit.getText().toString().trim());
                stock = Long.parseLong(etStockEdit.getText().toString().trim());
            } catch (NumberFormatException e) {
                Toast.makeText(this, "⚠️ Ingrese valores válidos para precio y stock", Toast.LENGTH_SHORT).show();
                return;
            }

            if (nombre.isEmpty()) {
                Toast.makeText(this, "⚠️ El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> actualizaciones = new HashMap<>();
            actualizaciones.put("nombre", nombre);
            actualizaciones.put("categoria", categoria);
            actualizaciones.put("descripcion", descripcion);
            actualizaciones.put("precio", precio);
            actualizaciones.put("stock", stock);

            db.collection("productos").document(doc.getId())
                    .update(actualizaciones)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "✅ Producto actualizado", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "❌ Error al actualizar: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        dialog.show();
    }

    // ==========================================================
    // 👇 === NUEVO MÉTODO PARA DETENER LISTENERS === 👇
    // ==========================================================
    private void detenerListeners() {
        if (productosListener != null) {
            productosListener.remove();
            productosListener = null;
        }

        if (productosRecientesListener != null) {
            productosRecientesListener.remove();
            productosRecientesListener = null;
        }
    }


    // 👇 MODIFICADO para usar el nuevo método
    @Override
    protected void onStop() {
        super.onStop();
        detenerListeners();
    }

}