package com.example.taskmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<User> userList;
    private List<String> selectedUserIds = new ArrayList<>();

    public UserAdapter(List<User> userList) {
        this.userList = userList;
    }

    public List<String> getSelectedUserIds() {
        return selectedUserIds;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.userNameTextView.setText(user.getName());

        holder.userCheckBox.setOnCheckedChangeListener(null);  // Reset listener

        // Gérer la sélection
        holder.userCheckBox.setChecked(selectedUserIds.contains(user.getId()));
        holder.userCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedUserIds.contains(user.getId())) {
                    selectedUserIds.add(user.getId());
                }
            } else {
                selectedUserIds.remove(user.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView userNameTextView;
        CheckBox userCheckBox;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userNameTextView = itemView.findViewById(R.id.textview_user_name);
            userCheckBox = itemView.findViewById(R.id.checkbox_user);
        }
    }
}
