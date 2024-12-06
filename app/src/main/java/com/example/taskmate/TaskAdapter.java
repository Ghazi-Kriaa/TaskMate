package com.example.taskmate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private Context context;
    private List<Task> taskList;
    private OnTaskClickListener listener;

    // Constructeur pour l'adaptateur
    public TaskAdapter(Context context, List<Task> taskList) {
        this.context = context;
        this.taskList = taskList;
    }

    // Interface pour gérer les clics sur les tâches
    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.listener = listener;
    }

    // Créer une vue pour chaque élément de la liste
    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    // Remplir les données dans les vues des éléments
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);

        holder.taskTitle.setText("Title: "+task.getTaskTitle());
        holder.taskUser.setText("Collaborator: "+task.getAssignedUser());
        holder.taskStatus.setText("Status: " + task.getStatus());

        // Gestion du clic sur un élément
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(task);
            }
        });
    }

    // Retourner le nombre d'éléments dans la liste
    @Override
    public int getItemCount() {
        return taskList.size();
    }

    // ViewHolder pour chaque élément de la liste
    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskTitle;
        TextView taskUser;
        TextView taskStatus;

        public TaskViewHolder(View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.taskTitle);
            taskUser = itemView.findViewById(R.id.taskUser);
            taskStatus = itemView.findViewById(R.id.taskStatus);
        }
    }
}
