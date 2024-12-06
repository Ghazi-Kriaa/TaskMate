package com.example.taskmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {
    private ArrayList<Project> projects;
    private OnProjectClickListener onProjectClickListener;

    public ProjectAdapter(ArrayList<Project> projects) {
        this.projects = projects;
    }

    // Interface pour gérer les clics sur un projet
    public interface OnProjectClickListener {
        void onProjectClick(Project project);
    }

    // Méthode pour définir l'écouteur de clic
    public void setOnProjectClickListener(OnProjectClickListener listener) {
        this.onProjectClickListener = listener;
    }

    @Override
    public ProjectViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ProjectViewHolder holder, int position) {
        Project project = projects.get(position);
        holder.projectTitle.setText(project.getTitle());  // Exemple pour afficher le titre
        holder.projectDescription.setText(project.getDescription());
        // Ajouter un écouteur de clic
        holder.itemView.setOnClickListener(v -> {
            if (onProjectClickListener != null) {
                onProjectClickListener.onProjectClick(project);
            }
        });
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    public static class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView projectTitle;
        TextView projectDescription;

        public ProjectViewHolder(View itemView) {
            super(itemView);
            projectTitle = itemView.findViewById(R.id.projectTitle);  // Assurez-vous d'avoir un TextView avec l'id projectTitle dans item_project.xml
            projectDescription = itemView.findViewById(R.id.projectDescription); // Id pour la description
        }
    }
}
