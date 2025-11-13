package com.proveenet.proveenet;

import androidx.appcompat.app.AppCompatActivity;

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

public class Proveedores extends BaseActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private LinearLayout llListaProveedores;
    private TextView tvProveedoresCount, tvUserName;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proveedores);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        llListaProveedores = findViewById(R.id.llListaProveedores);
        tvProveedoresCount = findViewById(R.id.tvProveedoresCount);
        tvUserName = findViewById(R.id.tvUserName);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        bottomNavigationView.setSelectedItemId(R.id.nav_proveedores);

        mostrarNombreUsuario();

        cargarProveedores();

        // 🔹 Menú inferior
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, Panel_comprador.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_proveedores) {
                return true;
            } else if (id == R.id.nav_productos) {
                startActivity(new Intent(this, Productos.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }


    // ======================================================
    private void mostrarNombreUsuario() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            tvUserName.setText("Usuario desconocido");
            return;
        }

        String uid = user.getUid();

        // Buscar primero en "compradores"
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
                        // Si no está en compradores, buscar en "proveedores"
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


    // ======================================================
    private void cargarProveedores() {
        db.collection("proveedores")
                .whereEqualTo("rol", "proveedor")
                .get()
                .addOnSuccessListener(this::mostrarProveedores)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }


    // ======================================================
    private void mostrarProveedores(QuerySnapshot snapshot) {
        llListaProveedores.removeAllViews();
        int count = 0;

        for (DocumentSnapshot doc : snapshot.getDocuments()) {

            String empresa = doc.getString("empresa");
            String rubro = doc.getString("rubro");
            String correo = doc.getString("correo");
            String telefono = doc.getString("telefono");
            String direccion = doc.getString("direccion"); // ✔ NUEVO

            // 🔹 Inflar card bonita item_proveedor.xml
            View card = LayoutInflater.from(this)
                    .inflate(R.layout.item_proveedor, llListaProveedores, false);

            // 🔹 Vincular vistas que SI existen
            TextView tvNombre = card.findViewById(R.id.tvNombreProveedor);
            TextView tvCategoria = card.findViewById(R.id.tvCategoria);
            TextView tvVerificado = card.findViewById(R.id.tvVerificado);
            TextView tvDireccion = card.findViewById(R.id.tvDireccion);
            TextView tvTelefono = card.findViewById(R.id.tvTelefono);
            // Si agregas correo de nuevo después:
            // TextView tvCorreo = card.findViewById(R.id.tvCorreo);

            Button btnCatalogo = card.findViewById(R.id.btnVerCatalogo);
            Button btnContactar = card.findViewById(R.id.btnContactar);

            // 🔹 Asignar datos reales
            tvNombre.setText(empresa != null ? empresa : "Proveedor sin nombre");
            tvCategoria.setText(rubro != null ? rubro : "Sin categoría");

            // ❌ No existe campo verificado: lo ocultamos
            tvVerificado.setVisibility(View.GONE);

            // ✔ DIRECCIÓN REAL
            if (direccion != null && !direccion.isEmpty()) {
                tvDireccion.setText(direccion);
                tvDireccion.setVisibility(View.VISIBLE);
            } else {
                tvDireccion.setText("Sin dirección");
                tvDireccion.setVisibility(View.VISIBLE);
            }

            // ✔ TELÉFONO
            tvTelefono.setText(telefono != null ? telefono : "Sin teléfono");

            // 🔹 Botón Ver Catálogo
            btnCatalogo.setOnClickListener(v ->
                    Toast.makeText(this, "📦 Ver catálogo de " + empresa, Toast.LENGTH_SHORT).show()
            );

            // 🔹 Botón Contactar
            btnContactar.setOnClickListener(v ->
                    Toast.makeText(this, "📞 Contactar a " + empresa, Toast.LENGTH_SHORT).show()
            );

            llListaProveedores.addView(card);
            count++;
        }

        tvProveedoresCount.setText(count + " proveedores disponibles");
    }

}
