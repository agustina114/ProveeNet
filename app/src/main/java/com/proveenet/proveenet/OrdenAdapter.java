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


import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
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

        String idOrden        = safeString(orden.get("id"));
        String estado         = safeString(orden.get("estado"));
        String metodoPago     = safeString(orden.get("metodoPago"));
        String productoNombre = safeString(orden.get("productoNombre"));
        String productoId     = safeString(orden.get("productoId"));

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

        holder.btnEditar.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Confirmar Orden")
                    .setMessage("¿Deseas confirmar esta orden y actualizar el stock del producto?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        db.collection("ordenes").document(idOrden)
                                .update("estado", "confirmada",
                                        "confirmacionProveedor", "confirmada")
                                .addOnSuccessListener(aVoid -> {
                                    db.collection("productos").document(productoId)
                                            .get()
                                            .addOnSuccessListener(snapshot -> {
                                                if (snapshot.exists()) {
                                                    Object stockObj = snapshot.get("stock");
                                                    long stockActual = safeLong(stockObj);
                                                    long nuevoStock = Math.max(stockActual - cantidad, 0);

                                                    db.collection("productos").document(productoId)
                                                            .update("stock", nuevoStock)
                                                            .addOnSuccessListener(aVoid2 -> {
                                                                Toast.makeText(context,
                                                                        "Orden confirmada",
                                                                        Toast.LENGTH_SHORT).show();

                                                                holder.btnEditar.setText("Confirmada");
                                                                holder.btnEditar.setEnabled(false);
                                                                holder.tvEstado.setText("confirmada");
                                                            })
                                                            .addOnFailureListener(e ->
                                                                    Toast.makeText(context,
                                                                            "Error al actualizar stock: " + e.getMessage(),
                                                                            Toast.LENGTH_SHORT).show());
                                                } else {
                                                    Toast.makeText(context,
                                                            "Producto no encontrado",
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(context,
                                                "Error al confirmar: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        // Botón eliminar
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
