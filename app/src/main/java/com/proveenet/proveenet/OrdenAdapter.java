package com.proveenet.proveenet;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap; // 👈 IMPORTADO
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrdenAdapter extends RecyclerView.Adapter<OrdenAdapter.OrdenViewHolder> {

    private final List<Map<String, Object>> listaOrdenes;
    private final FirebaseFirestore db;

    public OrdenAdapter(List<Map<String, Object>> listaOrdenes, FirebaseFirestore db) {
        this.listaOrdenes = listaOrdenes;
        this.db = db;
    }

    @Override
    public OrdenViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_orden, parent, false);
        return new OrdenViewHolder(v);
    }

    @Override
    public void onBindViewHolder(OrdenViewHolder holder, int position) {
        Map<String, Object> orden = listaOrdenes.get(position);
        Context context = holder.itemView.getContext();

        // --- Extraemos TODOS los datos que necesitamos ---
        String idOrden        = safeString(orden.get("id"));
        String estado         = safeString(orden.get("estado"));
        String metodoPago     = safeString(orden.get("metodoPago"));
        String productoNombre = safeString(orden.get("productoNombre"));
        String productoId     = safeString(orden.get("productoId"));

        // 👇 DATOS NUEVOS (para la colección 'ventas')
        String proveedorId    = safeString(orden.get("proveedorId"));
        String clienteId      = safeString(orden.get("clienteId")); // O "compradorId"

        double subtotal = safeDouble(orden.get("subtotal"));
        long cantidad   = safeLong(orden.get("cantidad"));

        // 🔹 Formatear fecha
        Object fechaObj = orden.get("fechaCreacion");
        String fechaFormateada = formatearFecha(fechaObj);

        holder.tvOrdenNumero.setText("Orden: " + (idOrden.isEmpty() ? "N/A" : idOrden));
        holder.tvEstado.setText(estado.isEmpty() ? "pendiente" : estado);
        holder.tvFecha.setText(fechaFormateada);
        holder.tvMetodoPago.setText(metodoPago.isEmpty() ? "No definido" : metodoPago);
        holder.tvTotal.setText("$" + String.format("%.0f", subtotal));
        holder.tvProductos.setText(productoNombre.isEmpty() ? "Sin producto" : productoNombre);

        // Botón Confirmar
        if ("pendiente".equalsIgnoreCase(estado)) {
            holder.btnEditar.setText("Confirmar");
            holder.btnEditar.setEnabled(true);
        } else {
            holder.btnEditar.setText("Confirmada");
            holder.btnEditar.setEnabled(false);
        }

        // --- 👇 LÓGICA DEL BOTÓN "CONFIRMAR" REEMPLAZADA ---
        holder.btnEditar.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Confirmar Orden")
                    .setMessage("¿Deseas confirmar esta orden, registrar la venta y actualizar el stock?")
                    .setPositiveButton("Sí", (dialog, which) -> {

                        // Usamos una Transacción para asegurar que todo se haga junto
                        db.runTransaction(transaction -> {
                                    // 1. LEER: Obtener el stock actual del producto
                                    DocumentReference productoRef = db.collection("productos").document(productoId);
                                    DocumentSnapshot productoSnap = transaction.get(productoRef);

                                    if (!productoSnap.exists()) {
                                        throw new FirebaseFirestoreException("El producto no existe.",
                                                FirebaseFirestoreException.Code.NOT_FOUND);
                                    }

                                    long stockActual = safeLong(productoSnap.get("stock"));
                                    long nuevoStock = stockActual - cantidad;

                                    if (nuevoStock < 0) {
                                        throw new FirebaseFirestoreException("Stock insuficiente para confirmar.",
                                                FirebaseFirestoreException.Code.ABORTED);
                                    }

                                    // 2. OPERACIONES DE ESCRITURA (dentro de la transacción)

                                    // Operación 1: Actualizar Stock del Producto
                                    transaction.update(productoRef, "stock", nuevoStock);

                                    // Operación 2: Actualizar Estado de la Orden
                                    DocumentReference ordenRef = db.collection("ordenes").document(idOrden);
                                    transaction.update(ordenRef, "estado", "confirmada",
                                            "confirmacionProveedor", "confirmada");

                                    // Operación 3: Crear el nuevo documento de VENTA
                                    DocumentReference ventaRef = db.collection("ventas").document(); // ID automático
                                    String mesAno = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

                                    Map<String, Object> nuevaVenta = new HashMap<>();
                                    nuevaVenta.put("proveedorId", proveedorId);
                                    nuevaVenta.put("clienteId", clienteId);
                                    nuevaVenta.put("ordenId", idOrden);
                                    nuevaVenta.put("total", subtotal);
                                    nuevaVenta.put("fechaConfirmacion", Timestamp.now());
                                    nuevaVenta.put("mesAno", mesAno); // Campo clave para el dashboard

                                    transaction.set(ventaRef, nuevaVenta);

                                    return null; // Éxito de la transacción
                                })
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(context, "✅ Orden confirmada y venta registrada", Toast.LENGTH_SHORT).show();

                                    // Actualizar la UI al instante
                                    holder.btnEditar.setText("Confirmada");
                                    holder.btnEditar.setEnabled(false);
                                    holder.tvEstado.setText("confirmada");
                                })
                                .addOnFailureListener(e -> {
                                    // Si falla (ej. por stock), mostrará el error
                                    Toast.makeText(context, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
        // --- FIN DE LA LÓGICA DEL BOTÓN "CONFIRMAR" ---

        // Botón eliminar (sin cambios)
        holder.btnEliminar.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Eliminar orden")
                    .setMessage("¿Seguro que deseas eliminar esta orden?")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        db.collection("ordenes").document(idOrden)
                                .delete()
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(context, "Orden eliminada", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return listaOrdenes.size();
    }

    // ------------------------------------------------------------
    // 🔹 Formatear Timestamp
    // ------------------------------------------------------------
    private String formatearFecha(Object fechaObj) {
        try {
            if (fechaObj instanceof Timestamp) {
                Date date = ((Timestamp) fechaObj).toDate();
                SimpleDateFormat sdf = new SimpleDateFormat(
                        "dd 'de' MMMM 'de' yyyy, HH:mm",
                        new Locale("es", "CL")
                );
                return sdf.format(date);
            }
        } catch (Exception ignored) {}
        return "Fecha no disponible";
    }

    // ------------------------------------------------------------
    // Métodos seguros
    // ------------------------------------------------------------
    private String safeString(Object o) {
        return o == null ? "" : o.toString();
    }

    private double safeDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0.0; }
    }

    private long safeLong(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return 0L; }
    }

    // ------------------------------------------------------------
    public static class OrdenViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrdenNumero, tvEstado, tvFecha, tvTotal, tvMetodoPago, tvProductos;
        Button btnEditar, btnEliminar;

        public OrdenViewHolder(View itemView) {
            super(itemView);
            tvOrdenNumero = itemView.findViewById(R.id.tvOrdenNumero);
            tvEstado      = itemView.findViewById(R.id.tvEstado);
            tvFecha       = itemView.findViewById(R.id.tvFecha);
            tvTotal       = itemView.findViewById(R.id.tvTotal);
            tvMetodoPago  = itemView.findViewById(R.id.tvMetodoPago);
            tvProductos   = itemView.findViewById(R.id.tvProductos);
            btnEditar     = itemView.findViewById(R.id.btnEditar);
            btnEliminar   = itemView.findViewById(R.id.btnEliminar);
        }
    }
}