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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class Proveedores extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout llListaProveedores;
    private TextView tvProveedoresCount;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proveedores);

        db = FirebaseFirestore.getInstance();
        llListaProveedores = findViewById(R.id.llContent);
        tvProveedoresCount = findViewById(R.id.tvProveedoresCount);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // ✅ Marca “Proveedores” como activo
        bottomNavigationView.setSelectedItemId(R.id.nav_proveedores);

        // 🔹 Cargar los proveedores reales
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
    // 📡 Cargar proveedores con rol == "proveedor"
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
    // 🧱 Mostrar cards dinámicamente
    // ======================================================
    private void mostrarProveedores(QuerySnapshot snapshot) {
        llListaProveedores.removeAllViews();
        int count = 0;

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            String empresa = doc.getString("empresa");
            String rubro = doc.getString("rubro");
            String correo = doc.getString("correo");
            String telefono = doc.getString("telefono");

            // Inflar una card
            View card = LayoutInflater.from(this).inflate(R.layout.item_proveedor_card, llListaProveedores, false);

            TextView tvNombre = card.findViewById(R.id.tvNombreProveedor);
            TextView tvRubro = card.findViewById(R.id.tvCategoria);
            TextView tvCorreo = card.findViewById(R.id.tvDireccion);
            TextView tvTelefono = card.findViewById(R.id.tvTelefono);

            tvNombre.setText(empresa != null ? empresa : "Proveedor sin nombre");
            tvRubro.setText("Rubro: " + (rubro != null ? rubro : "No especificado"));
            tvCorreo.setText("Correo: " + (correo != null ? correo : "Sin correo"));
            tvTelefono.setText("Teléfono: " + (telefono != null ? telefono : "Sin teléfono"));


            // Botón Contactar
            Button btnContactar = card.findViewById(R.id.btnContactar);
            btnContactar.setOnClickListener(v ->
                    Toast.makeText(this, "📞 Contactar a " + empresa, Toast.LENGTH_SHORT).show()
            );

            llListaProveedores.addView(card);
            count++;
        }

        tvProveedoresCount.setText(count + " proveedores disponibles");
    }
}
